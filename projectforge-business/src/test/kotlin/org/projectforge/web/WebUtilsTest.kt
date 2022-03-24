/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WebUtilsTest {

  @Test
  fun urlNormalizingTest() {
    assertNull(WebUtils.normalizeUri(null))
    assertEquals("/", WebUtils.normalizeUri(""))
    assertEquals("/", WebUtils.normalizeUri("/"))
    assertEquals("/react/", WebUtils.normalizeUri("/react/"))
    assertEquals("/rs", WebUtils.normalizeUri("/react/../rs"))
    assertEquals("<invalid>", WebUtils.normalizeUri("../rs"))
    assertEquals("/", WebUtils.normalizeUri("/react/../rs/../"))
    assertEquals("<invalid>", WebUtils.normalizeUri("/react/../rs/../.."))
    assertEquals("<invalid>", WebUtils.normalizeUri("/react/../rs/../../"))
    assertEquals("<invalid>", WebUtils.normalizeUri("/react/../rs/../../react"))
  }
}
