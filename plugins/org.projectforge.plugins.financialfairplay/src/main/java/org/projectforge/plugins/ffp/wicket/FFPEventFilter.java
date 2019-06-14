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

package org.projectforge.plugins.ffp.wicket;

import java.io.Serializable;

import org.projectforge.framework.persistence.api.BaseSearchFilter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * 
 * @author Florian Blumenstein
 * 
 */
@XStreamAlias("FFPEventFilter")
public class FFPEventFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = 8567780910637887786L;

  private boolean showOnlyActiveEntries=true;

  public FFPEventFilter()
  {
  }

  public FFPEventFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  public boolean isShowOnlyActiveEntries() {
    return showOnlyActiveEntries;
  }

  public void setShowOnlyActiveEntries(boolean showOnlyActiveEntries) {
    this.showOnlyActiveEntries = showOnlyActiveEntries;
  }
}
