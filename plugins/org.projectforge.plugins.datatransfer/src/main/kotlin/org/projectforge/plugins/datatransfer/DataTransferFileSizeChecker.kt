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

package org.projectforge.plugins.datatransfer

import mu.KotlinLogging
import org.projectforge.common.MaxFileSizeExceeded
import org.projectforge.jcr.FileInfo
import org.projectforge.jcr.FileSizeChecker

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class DataTransferFileSizeChecker(val globalMaxFileSizeOfDataTransfer: Long) : FileSizeChecker {

  override fun checkSize(file: FileInfo, data: Any?): MaxFileSizeExceeded? {
    var ex = checkSize(file, globalMaxFileSizeOfDataTransfer, DataTransferAreaDao.MAX_FILE_SIZE_SPRING_PROPERTY)
    if (ex != null) {
      return ex
    }
    if (data == null || data !is DataTransferAreaDO) {
      log.warn { "maxUploadsizeKB of area not given. area not given or not of Type DataTransferAreadDO: $data" }
      return null
    }
    val maxUploadSizeKB = data.maxUploadSizeKB
    if (maxUploadSizeKB == null) {
      log.warn { "maxUploadsizeKB of area not given. Can't check this size: $data" }
      return null
    }
    ex = checkSize(file, 1024L * maxUploadSizeKB, null)
    if (ex != null) {
      return ex
    }
    data.attachmentsSize?.let { totalSize ->
      file.size?.let { fileSize ->
        // Total size of area is 2 times of maxUploadSize:
        if (fileSize + totalSize > 2048L * maxUploadSizeKB) {
          val ex2 = MaxFileSizeExceeded(
            2048L * maxUploadSizeKB,
            "File will not be stored: $file.",
            fileSize + totalSize,
            file.fileName,
            null,
            info = file
          )
          log.error { ex2.message }
          return ex2
        }
      }
    }
    log.warn { "Size of attachments of area not given. Can't check total size of area: $data" }
    return null
  }
}
