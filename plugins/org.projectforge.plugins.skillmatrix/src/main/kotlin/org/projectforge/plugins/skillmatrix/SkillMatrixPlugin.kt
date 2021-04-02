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

package org.projectforge.plugins.skillmatrix

import mu.KotlinLogging
import org.projectforge.Const
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.plugins.core.AbstractPlugin
import org.projectforge.plugins.core.PluginAdminService
import org.springframework.beans.factory.annotation.Autowired

private val log = KotlinLogging.logger {}

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class SkillMatrixPlugin : AbstractPlugin(PluginAdminService.PLUGIN_SKILL_MATRIX_ID, "Skill matrix", "The users skills managed by the users themselve.") {

    @Autowired
    private lateinit var skillEntryDao: SkillEntryDao

    @Autowired
    private lateinit var menuCreator: MenuCreator

    override fun initialize() {
        // Register it:
        register(id, SkillEntryDao::class.java, skillEntryDao, "plugins.skillmatrix")

        // Define the access management:
        registerRight(SkillRight(accessChecker))

        menuCreator.add(MenuItemDefId.PROJECT_MANAGEMENT, MenuItemDef(info.id, "plugins.skillmatrix.menu", "${Const.REACT_APP_PATH}skillentry"));

        // All the i18n stuff:
        addResourceBundle(RESOURCE_BUNDLE_NAME)
    }

    companion object {
        const val RESOURCE_BUNDLE_NAME = "SkillMatrixI18nResources"
    }
}
