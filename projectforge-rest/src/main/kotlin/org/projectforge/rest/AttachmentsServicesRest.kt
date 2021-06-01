/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.common.FormatterUtils
import org.projectforge.framework.api.TechnicalException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.jcr.AttachmentsDaoAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.jcr.FileInfo
import org.projectforge.jcr.ZipMode
import org.projectforge.jcr.ZipUtils
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.projectforge.ui.UIAttachmentList
import org.projectforge.ui.UIToast
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Rest services for up- and downloading, updating and deletion of attachments. It's used by ContractDao as well as
 * by Data transfer for registered users.
 */
@RestController
@RequestMapping("${Rest.URL}/attachments")
class AttachmentsServicesRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  class AttachmentData(
    var category: String,
    var id: Int,
    var fileId: String,
    var listId: String? = null
  ) {
    lateinit var attachment: Attachment
  }

  class ResponseData(var attachments: List<Attachment>?)

  @PostMapping("modify")
  fun modify(request: HttpServletRequest, @RequestBody postData: PostData<AttachmentData>)
      : ResponseEntity<ResponseAction>? {
    validateCsrfToken(request, postData)?.let { return it }
    val data = postData.data
    val attachment = data.attachment
    val pagesRest = getPagesRest(data.category, data.listId)
    getAttachment(pagesRest, data) // Check attachment availability
    val obj = getDataObject(pagesRest, data.id) // Check data object availability.

    attachmentsService.changeFileInfo(
      pagesRest.jcrPath!!, data.fileId, pagesRest.baseDao, obj, attachment.name, attachment.description,
      pagesRest.attachmentsAccessChecker, data.listId
    )
    val list =
      attachmentsService.getAttachments(pagesRest.jcrPath!!, data.id, pagesRest.attachmentsAccessChecker, data.listId)
    return ResponseEntity.ok()
      .body(
        ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
          .addVariable("data", ResponseData(list))
      )
  }

  @PostMapping("encrypt")
  fun encrypt(request: HttpServletRequest, @RequestBody postData: PostData<AttachmentData>)
      : Any? {
    validateCsrfToken(request, postData)?.let { return it }
    val data = postData.data
    val attachment = data.attachment
    val encryptionMode = attachment.newZipMode ?: ZipMode.ENCRYPTED_STANDARD
    val password = attachment.password
    if (password.isNullOrBlank() || password.length < 6) {
      return UIToast.createToast(translateMsg("user.changePassword.error.notMinLength", "6"))
    }
    val pagesRest = getPagesRest(data.category, data.listId)
    getAttachment(pagesRest, data) // Check attachment availability
    val obj = getDataObject(pagesRest, data.id) // Check data object availability.

    val pair = attachmentsService.getAttachmentInputStream(
      pagesRest.jcrPath!!, data.id, data.fileId, pagesRest.attachmentsAccessChecker, data.listId
    )
    if (pair?.second == null) {
      log.error { "Can't encrypt zip file. Not found as inputstream: $attachment" }
      return UIToast.createToast(translate("exception.internalError"))
    }
    var newFilename = "untitled.zip"
    val tmpFile = File.createTempFile("projectforge-encrypted-zip", null)
    pair.second.use { istream ->
      val file = File(pair.first.fileName ?: "untitled.zip")
      val filenameWithoutExtension = file.nameWithoutExtension
      val oldExtension = file.extension
      val preserveExtension = if (oldExtension.equals("zip", ignoreCase = true)) "" else ".$oldExtension"
      newFilename = "$filenameWithoutExtension$preserveExtension.zip"
      FileOutputStream(tmpFile).use { out ->
        ZipUtils.encryptZipFile(
          file.name,
          password,
          istream,
          out,
          encryptionMode
        )
      }
    }
    FileInputStream(tmpFile).use { istream ->
      attachmentsService.addAttachment(
        pagesRest.jcrPath!!,
        fileInfo = FileInfo(
          newFilename,
          fileSize = tmpFile.length(),
          description = attachment.description,
          zipMode = encryptionMode,
        ),
        inputStream = istream,
        baseDao = pagesRest.baseDao,
        obj = obj,
        accessChecker = pagesRest.attachmentsAccessChecker
      )
    }
    tmpFile.delete()
    attachmentsService.deleteAttachment(
      pagesRest.jcrPath!!,
      data.fileId,
      pagesRest.baseDao,
      obj,
      pagesRest.attachmentsAccessChecker,
      data.listId
    )
    val list =
      attachmentsService.getAttachments(pagesRest.jcrPath!!, data.id, pagesRest.attachmentsAccessChecker, data.listId)
    return ResponseEntity.ok()
      .body(
        ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
          .addVariable("data", ResponseData(list))
      )
  }

  /*
  @PostMapping("decrypt")
  fun decrypt(request: HttpServletRequest, @RequestBody postData: PostData<AttachmentData>)
      : Any? {
    validateCsrfToken(request, postData)?.let { return it }
    val data = postData.data
    val attachment = data.attachment
    val password = attachment.password
    val pagesRest = getPagesRest(data.category, data.listId)
    getAttachment(pagesRest, data) // Check attachment availability
    val obj = getDataObject(pagesRest, data.id) // Check data object availability.

    val pair = attachmentsService.getAttachmentInputStream(
      pagesRest.jcrPath!!, data.id, data.fileId, pagesRest.attachmentsAccessChecker, data.listId
    )
    if (pair?.second == null) {
      log.error { "Can't encrypt zip file. Not found as inputstream: $attachment" }
      return UIToast.createToast(translate("exception.internalError"))
    }
    pair.second.use { istream ->
      val file = File(pair.first.fileName ?: "untitled.zip")
      val filenameWithoutExtension = file.nameWithoutExtension
      val oldExtension = file.extension
      val preserveExtension = if (oldExtension.equals("zip", ignoreCase = true)) "" else ".$oldExtension"
      newFilename = "$filenameWithoutExtension$preserveExtension.zip"
      FileOutputStream(tmpFile).use { out ->
        ZipUtils.encryptZipFile(
          newFilename,
          password,
          istream,
          out,
          encryptionMode
        )
      }
    }
    FileInputStream(tmpFile).use { istream ->
      attachmentsService.addAttachment(
        pagesRest.jcrPath!!,
        fileInfo = FileInfo(
          newFilename,
          fileSize = tmpFile.length(),
          description = attachment.description,
          zipMode = encryptionMode,
        ),
        inputStream = istream,
        baseDao = pagesRest.baseDao,
        obj = obj,
        accessChecker = pagesRest.attachmentsAccessChecker
      )
    }
    attachmentsService.deleteAttachment(
      pagesRest.jcrPath!!,
      data.fileId,
      pagesRest.baseDao,
      obj,
      pagesRest.attachmentsAccessChecker,
      data.listId
    )
    val list =
      attachmentsService.getAttachments(pagesRest.jcrPath!!, data.id, pagesRest.attachmentsAccessChecker, data.listId)
    return ResponseEntity.ok()
      .body(
        ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
          .addVariable("data", ResponseData(list))
      )
  }*/

  /**
   * Upload service e. g. for [UIAttachmentList].
   * @param id Object id where the uploaded file should belong to.
   * @param listId Usable for handling different upload areas for one page. If only one attachment list is needed, you may
   * ignore this value.
   */
  @PostMapping("upload/{category}/{id}/{listId}")
  fun uploadAttachment(
    @PathVariable("category", required = true) category: String,
    @PathVariable("id", required = true) id: Int,
    @PathVariable("listId") listId: String?,
    @RequestParam("file") file: MultipartFile
  )
//@RequestParam("files") files: Array<MultipartFile>)
      : ResponseEntity<*>? {
    val pagesRest = getPagesRest(category, listId)
    //files.forEach { file ->
    val filename = file.originalFilename
    log.info {
      "User tries to upload attachment: id='$id', listId='$listId', filename='$filename', size=${
        FormatterUtils.formatBytes(
          file.size
        )
      }, page='${this::class.java.name}'."
    }

    val obj = getDataObject(pagesRest, id) // Check data object availability.
    attachmentsService.addAttachment(
      pagesRest.jcrPath!!,
      fileInfo = FileInfo(file.originalFilename, fileSize = file.size),
      inputStream = file.inputStream,
      baseDao = pagesRest.baseDao,
      obj = obj,
      accessChecker = pagesRest.attachmentsAccessChecker
    )
    //}
    val list = attachmentsService.getAttachments(pagesRest.jcrPath!!, id, pagesRest.attachmentsAccessChecker, listId)
    return ResponseEntity.ok()
      .body(
        ResponseAction(targetType = TargetType.UPDATE, merge = true)
          .addVariable("data", ResponseData(list))
      )
  }

  @PostMapping("delete")
  fun delete(request: HttpServletRequest, @RequestBody postData: PostData<AttachmentData>)
      : ResponseEntity<ResponseAction>? {
    validateCsrfToken(request, postData)?.let { return it }
    val data = postData.data
    val pagesRest = getPagesRest(data.category, data.listId)
    val obj = getDataObject(pagesRest, data.id) // Check data object availability.
    attachmentsService.deleteAttachment(
      pagesRest.jcrPath!!,
      data.fileId,
      pagesRest.baseDao,
      obj,
      pagesRest.attachmentsAccessChecker,
      data.listId
    )
    val list =
      attachmentsService.getAttachments(pagesRest.jcrPath!!, data.id, pagesRest.attachmentsAccessChecker, data.listId)
        ?: emptyList() // Client needs empty list to update data of attachments.
    return ResponseEntity.ok()
      .body(
        ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
          .addVariable("data", ResponseData(list))
      )
  }

  @GetMapping("download/{category}/{id}")
  fun download(
    @PathVariable("category", required = true) category: String,
    @PathVariable("id", required = true) id: Int,
    @RequestParam("fileId", required = true) fileId: String,
    @RequestParam("listId") listId: String?
  )
      : ResponseEntity<InputStreamResource> {
    log.info { "User tries to download attachment: ${paramsToString(category, id, fileId, listId)}." }
    val pagesRest = getPagesRest(category, listId)

    val result =
      attachmentsService.getAttachmentInputStream(pagesRest.jcrPath!!, id, fileId, pagesRest.attachmentsAccessChecker)
        ?: throw TechnicalException(
          "File to download not accessible for user or not found: ${
            paramsToString(
              category,
              id,
              fileId,
              listId
            )
          }."
        )

    val filename = result.first.fileName ?: "file"
    val inputStream = result.second
    return RestUtils.downloadFile(filename, inputStream)
  }

  internal fun getPagesRest(
    category: String,
    listId: String?
  ): AbstractPagesRest<out ExtendedBaseDO<Int>, *, out BaseDao<*>> {
    val pagesRest = PagesResolver.getPagesRest(category)
      ?: throw UnsupportedOperationException("PagesRest class for category '$category' not known (registered).")
    pagesRest.attachmentsAccessChecker.let {
      if (it is AttachmentsDaoAccessChecker<*>) {
        it.checkJcrActivity(listId)
      }
      return pagesRest
    }
  }

  fun getAttachment(
    jcrPath: String,
    attachmentsAccessChecker: AttachmentsAccessChecker,
    data: AttachmentData
  ): Attachment {
    return attachmentsService.getAttachmentInfo(
      jcrPath,
      data.id,
      data.fileId,
      attachmentsAccessChecker,
      data.listId
    )
      ?: throw TechnicalException(
        "Attachment '$data.fileId' for object with id $data.id not found for category '$data.category' and list '$data.listId'.",
        "Attachment not found."
      )
  }

  internal fun getAttachment(pagesRest: AbstractPagesRest<*, *, *>, data: AttachmentData): Attachment {
    return getAttachment(pagesRest.jcrPath!!, pagesRest.attachmentsAccessChecker, data)
  }

  internal fun getDataObject(pagesRest: AbstractPagesRest<*, *, *>, id: Int): ExtendedBaseDO<Int> {
    return pagesRest.baseDao.getById(id)
      ?: throw TechnicalException(
        "Entity with id $id not accessible for category '$pagesRest.category' or doesn't exist.",
        "User without access or id unknown."
      )

  }

  private fun paramsToString(category: String, id: Any, fileId: String, listId: String?): String {
    return "category='$category', id='$id', fileId='$fileId', listId='$listId'"
  }
}
