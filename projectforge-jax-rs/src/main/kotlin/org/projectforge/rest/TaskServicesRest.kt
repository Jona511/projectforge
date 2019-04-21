package org.projectforge.rest

import org.projectforge.business.fibu.AuftragsPositionsStatus
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskFilter
import org.projectforge.business.task.TaskNode
import org.projectforge.business.task.TaskTree
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.service.UserPreferencesService
import org.projectforge.common.i18n.Priority
import org.projectforge.common.task.TaskStatus
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDate
import org.projectforge.rest.core.RestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * For uploading address immages.
 */
@Component
@Path("task")
class TaskServicesRest {
    class Kost2(val number: String, val cost2Art: String)
    class OrderPosition(val number: Int, val personDays: Int?, val title: String, status: AuftragsPositionsStatus?)
    class Order(val number: String,
                val title: String,
                val text: String,
                val orderPositions: MutableList<OrderPosition>? = null) // Positions

    enum class TreeStatus() { LEAF, OPENED, CLOSED }
    class Task(val id: Int,
               /**
                * Indent is only given for table view.
                */
               var indent: Int? = null,
               /**
                * All (opened) sub notes for table view or direct child notes for tree view
                */
               var childs: MutableList<Task>? = null,
               var treeStatus: TreeStatus? = null,
               val title: String? = null,
               val shortDescription: String? = null,
               val protectTimesheetsUntil: PFDate? = null,
               val reference: String? = null,
               val priority: Priority? = null,
               val status: TaskStatus? = null,
               val responsibleUser: PFUserDO? = null,
               var kost2List: List<Kost2>? = null,
               var path: List<Task>? = null) {
        constructor(node: TaskNode) : this(id = node.task.id, title = node.task.title, shortDescription = node.task.shortDescription,
                protectTimesheetsUntil = PFDate.from(node.task.protectTimesheetsUntil), reference = node.task.reference,
                priority = node.task.priority, status = node.task.status, responsibleUser = node.task.responsibleUser)
    }

    class Result(val root: Task,
                 var translations: MutableMap<String, String>? = null)

    private class BuildContext(val user: PFUserDO,
                               val taskFilter: TaskFilter,
                               val rootTask: Task, // Only for table view.
                               val openedNodes: MutableSet<Int>)

    private val log = org.slf4j.LoggerFactory.getLogger(TaskServicesRest::class.java)

    @Autowired
    private lateinit var accessChecker: AccessChecker;

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var userPreferencesService: UserPreferencesService

    private val restHelper = RestHelper()

    private val taskTree: TaskTree
        /** Lazy init, because test cases failed due to NPE in TenantRegistryMap. */
        get() = TaskTreeHelper.getTaskTree()

    /**
     * Gets the user's task tree as tree matching the filter. The open task nodes will be restored from the user's prefs.
     * @param initial If true, the layout info and translations are also returned. Default is to return only the tree data.
     * @param open Optional task to open in the tree (if a descendent child of closed tasks, all ancestor tasks will be opened as well).
     * @param close Optional task to close.
     * @param table If true, the result will be returned flat with indent counter of each task node, otherwise a tree object is returned.
     * @return json
     */
    @GET
    @Path("tree")
    @Produces(MediaType.APPLICATION_JSON)
    fun getTree(@QueryParam("initial") initial: Boolean?,
                @QueryParam("open") open: Int?,
                @QueryParam("close") close: Int?,
                @QueryParam("table") table: Boolean?)
            : Response {
        @Suppress("UNCHECKED_CAST")
        val openNodes = userPreferencesService.getEntry(TaskTree.USER_PREFS_KEY_OPEN_TASKS) as MutableSet<Int>
        val rootNode = taskTree.rootTaskNode
        val root = Task(rootNode)
        addKost2List(root)
        root.childs = mutableListOf()
        val ctx = BuildContext(ThreadLocalUserContext.getUser(), TaskFilter(), root, openNodes)
        openTask(ctx, open)
        closeTask(ctx, close)
        //UserPreferencesHelper.putEntry(TaskTree.USER_PREFS_KEY_OPEN_TASKS, expansion.getIds(), true)
        val indent = if (table == true) 0 else null
        buildTree(ctx, root, rootNode, indent)
        if (table == true && (accessChecker.isLoggedInUserMemberOfAdminGroup() ||
                accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP))) {
            // Append root node for admins and financial staff only in table view for displaying purposes.
            root.childs?.add(Task(rootNode))
        }
        val result = Result(root)
        if (initial == true) {
            result.translations = mutableMapOf()
            val translations = result.translations!!
            translations["task"] = translate("task")
            translations["task.consumption"] = translate("task.consumption")
            translations["task.tree.info"] = translate("task.tree.info")
        }
        return restHelper.buildResponse(result)
    }

    /**
     * Gets the task data including kost2 information if any and its path.
     * @param id Task id.
     * @return json
     */
    @GET
    @Path("info/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getTaskInfo(@PathParam("id") id: Int?): Response {
        val taskNode = taskTree.getTaskNodeById(id) ?: return restHelper.buildResponseItemNotFound()
        val task = Task(taskNode)
        addKost2List(task)
        val pathToRoot = taskTree.getPathToRoot(taskNode.parentId)
        val pathArray = mutableListOf<Task>()
        pathToRoot?.forEach {
            val ancestor = Task(id = it.task.id, title = it.task.title)
            pathArray.add(ancestor)
        }
        task.path = pathArray
        return restHelper.buildResponse(task)
    }

    private fun addKost2List(task: Task) {
        val kost2DOList = taskTree.getKost2List(task.id)
        if (!kost2DOList.isNullOrEmpty()) {
            val kost2List: List<Kost2> = kost2DOList.map {
                Kost2((it as Kost2DO).formattedNumber, it.kost2Art.name)
            }
            task.kost2List = kost2List
        }
    }

    /**
     * @param indent null for tree view, int for table view.
     */
    private fun buildTree(ctx: BuildContext, task: Task, taskNode: TaskNode, indent: Int? = null) {
        if (!taskNode.hasChilds()) {
            task.treeStatus = TreeStatus.LEAF
            return
        }
        if (ctx.openedNodes.contains(taskNode.taskId)) {
            task.treeStatus = TreeStatus.OPENED
            val childs = taskNode.childs.toMutableList()
            childs.sortBy({ it.task.title })
            childs.forEach {
                if (ctx.taskFilter.match(it, taskDao, ctx.user)) {
                    val child = Task(it)
                    addKost2List(child)
                    if (indent != null) {
                        ctx.rootTask.childs!!.add(child) // All childs are added to root task (table view!)
                        child.indent = indent
                        buildTree(ctx, child, it, indent + 1) // Build as table (all childs are direct childs of root node.
                    } else {
                        // TaskNode has childs and is opened:
                        if (task.childs == null)
                            task.childs = mutableListOf()
                        task.childs!!.add(child)
                        buildTree(ctx, child, it, null) // Build as tree
                    }
                }
            }
        } else {
            task.treeStatus = TreeStatus.CLOSED
        }
    }

    private fun openTask(ctx: BuildContext, taskId: Int?) {
        if (taskId == null)
            return
        val taskNode = taskTree.getTaskNodeById(taskId)
        if (taskNode == null) {
            log.warn("Task with id $taskId not found to open.")
            return
        }
        ctx.openedNodes.add(taskId)
        var parent = taskNode.parent
        while (parent != null) {
            ctx.openedNodes.add(parent.taskId)
            parent = parent.parent
        }
    }

    private fun closeTask(ctx: BuildContext, taskId: Int?) {
        if (taskId == null)
            return
        val taskNode = taskTree.getTaskNodeById(taskId)
        if (taskNode == null) {
            log.warn("Task with id $taskId not found to close.")
            return
        }
        ctx.openedNodes.remove(taskId)
    }
}
