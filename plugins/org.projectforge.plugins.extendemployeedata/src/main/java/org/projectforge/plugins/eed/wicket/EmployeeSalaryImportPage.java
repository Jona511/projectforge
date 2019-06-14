/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.eed.wicket;

import java.io.InputStream;
import java.util.Date;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.utils.ImportedSheet;
import org.projectforge.plugins.eed.service.EmployeeSalaryImportService;
import org.projectforge.web.core.importstorage.AbstractImportPage;

public class EmployeeSalaryImportPage extends AbstractImportPage<EmployeeSalaryImportForm>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeSalaryImportPage.class);

  @SpringBean
  private EmployeeSalaryImportService employeeSalaryImportService;

  public EmployeeSalaryImportPage(final PageParameters parameters)
  {
    super(parameters);
    form = new EmployeeSalaryImportForm(this);
    body.add(form);
    form.init();
    clear(); // reset state of the page, clear the import storage
    if (parameters != null && parameters.get(0) != null && parameters.get(0).toString() != null && parameters.get(0).toString().equals("success")) {
      error(I18nHelper.getLocalizedMessage("plugins.eed.salaryimport.success"));
    }
  }

  @Override
  protected void clear()
  {
    super.clear();
    form.setDateDropDownsEnabled(true);
  }

  boolean doImport(final Date dateToSelectAttrRow)
  {
    checkAccess();
    final FileUpload fileUpload = form.fileUploadField.getFileUpload();
    if (fileUpload != null) {
      final Boolean success = doImportWithExcelExceptionHandling(() -> {
        final InputStream is = fileUpload.getInputStream();
        final String clientFileName = fileUpload.getClientFileName();
        setStorage(employeeSalaryImportService.importData(is, clientFileName, dateToSelectAttrRow));
        return true;
      });
      return Boolean.TRUE.equals(success);
    }
    return false;
  }

  @Override
  protected ImportedSheet<?> reconcile(final String sheetName)
  {
    checkAccess();
    final ImportedSheet<?> sheet = super.reconcile(sheetName);
    employeeSalaryImportService.reconcile(getStorage(), sheetName);
    return sheet;
  }

  @Override
  protected ImportedSheet<?> commit(final String sheetName)
  {
    checkAccess();
    final ImportedSheet<?> sheet = super.commit(sheetName);
    employeeSalaryImportService.commit(getStorage(), sheetName);
    return sheet;
  }

  @Override
  protected void selectAll(final String sheetName)
  {
    checkAccess();
    super.selectAll(sheetName);
  }

  @Override
  protected void select(final String sheetName, final int number)
  {
    checkAccess();
    super.select(sheetName, number);
  }

  @Override
  protected void deselectAll(final String sheetName)
  {
    checkAccess();
    super.deselectAll(sheetName);
  }

  private void checkAccess()
  {
    accessChecker.checkLoggedInUserRight(UserRightId.HR_EMPLOYEE, UserRightValue.READWRITE);
    accessChecker.checkRestrictedOrDemoUser();
  }

  @Override
  protected String getTitle()
  {
    return getString("plugins.eed.salaryimport.title");
  }

}
