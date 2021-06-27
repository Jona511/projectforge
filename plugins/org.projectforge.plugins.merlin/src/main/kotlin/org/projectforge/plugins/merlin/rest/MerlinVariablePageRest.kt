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

package org.projectforge.plugins.merlin.rest

import de.micromata.merlin.word.templating.VariableType
import mu.KotlinLogging
import org.projectforge.model.rest.RestPaths
import org.projectforge.plugins.merlin.MerlinTemplate
import org.projectforge.plugins.merlin.MerlinTemplateDO
import org.projectforge.plugins.merlin.MerlinTemplateDao
import org.projectforge.plugins.merlin.MerlinVariable
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

private val log = KotlinLogging.logger {}

/**
 * Modal dialog showing details of an attachment with the functionality to download, modify and delete it.
 */
@RestController
@RequestMapping("${Rest.URL}/merlinvariables")
class MerlinVariablePageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var merlinTemplateDao: MerlinTemplateDao

  /**
   * Returns the form for a single attachment, including file properties as well as editable properties such
   * as file name and description.
   * The form supports also the buttons: download, delete and update.
   * The react path of this should look like: 'react/attachment/dynamic/42?category=contract...'
   * @param id: Id of data object with attachments.
   */
  @GetMapping("dynamic")
  fun getForm(
    @RequestParam("id", required = true) id: Int,
    request: HttpServletRequest
  ): FormLayoutData {
    val dto = ExpiringSessionAttributes.getAttribute(request.session, "${this::class.java.name}:$id")
    if (dto == null || dto !is MerlinTemplate) {
      throw InternalError("Please try again.")
    }
    return FormLayoutData(dto, createAttachmentLayout(dto), createServerData(request))
  }

  /**
   * For editing variables.
   */
  @PostMapping("edit/{variable}")
  fun getForm(
    @PathVariable("variable", required = true) variable: String,
    @Valid @RequestBody dto: MerlinTemplate, request: HttpServletRequest
  ): ResponseEntity<*> {
    ExpiringSessionAttributes.setAttribute(request.session, "${this::class.java.name}:${dto.id}", dto, 1)
    return ResponseEntity.ok()
      .body(
        ResponseAction(
          PagesResolver.getDynamicPageUrl(
            this::class.java,
            id = dto.id,
            absolute = true
          ), targetType = TargetType.MODAL
        )
      )
/*    merlinTemplateDao.
    services.getDataObject(pagesRest, id) // Check data object availability.
    val data = AttachmentsServicesRest.AttachmentData(category = category, id = id, fileId = fileId, listId = listId)
    data.attachment = services.getAttachment(pagesRest, data)
    val actionListener = services.getListener(category)
    val layout = actionListener.createAttachmentLayout(
      id,
      category,
      fileId,
      listId,
      attachment = data.attachment,
      encryptionSupport = true,
      data = data,
    )*/
  }

  /**
   * Will be called, if the user wants to see the encryption options.
   */
  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(@Valid @RequestBody postData: PostData<MerlinTemplate>): ResponseEntity<ResponseAction> {
    val dto = postData.data
    // write access is always true, otherwise watch field wasn't registered.
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable(
          "ui",
          createAttachmentLayout(dto)
        )
        .addVariable("data", dto)
    )
  }

  private fun createAttachmentLayout(dto: MerlinTemplate): UILayout {
    val lc = LayoutContext(MerlinVariable::class.java)
    val layout = UILayout("plugins.merlin.variable.edit")
      .add(
        UIFieldset(UILength(md = 12, lg = 12))
          .add(
            UIRow().add(
              UICol(UILength(md = 6))
                .add(UIReadOnlyField("name", lc))
                .add(UISelect("type", lc, values = VariableType.values().map { UISelectValue(it, it.name) }))
            )
          )
          .add(
            UIRow().add(
              UICol(UILength(md = 6))
                .add(
                  UICheckbox("required", lc)
                )
                .add(
                  UICheckbox("unique", lc)
                )
            )
          )
          .add(
            UIRow().add(
              UICol()
                .add(
                  UIReadOnlyField("description", lc)
                )
            )
          )
      )
    LayoutUtils.process(layout)
    return layout
  }
}
