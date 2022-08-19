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

package org.projectforge.plugins.banking

import org.hibernate.search.annotations.Analyze
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.Constants
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.props.PropertyType
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.math.BigDecimal
import java.time.LocalDate
import javax.persistence.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
  name = "T_PLUGIN_BANKING_ACCOUNT_RECORD",
)
@NamedQueries(
  NamedQuery(
    name = BankAccountRecordDO.FIND_BY_TIME_PERIOD,
    query = "from BankAccountRecordDO where id=:id and date>=:from and date<=:until order by date"
  ),
)
open class BankAccountRecordDO : DefaultBaseDO() {
  @PropertyInfo(i18nKey = "plugins.teamcal.calendar")
  @IndexedEmbedded(depth = 1)
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "banking_account_fk", nullable = false)
  open var bankAccount: BankAccountDO? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.record.amount", type = PropertyType.CURRENCY)
  @get:Column(name = "amount", scale = 2, precision = 12)
  open var amount: BigDecimal? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.record.date")
  @Field(analyze = Analyze.NO)
  open var date: LocalDate? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.record.valueDate")
  @Field(analyze = Analyze.NO)
  @get:Column(name = "value_date")
  open var valueDate: LocalDate? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.record.type")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT)
  open var type: String? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.record.subject")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT)
  open var subject: String? = null

  @PropertyInfo(i18nKey = "comment")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT)
  open var comment: String? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.record.currency")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT)
  open var currency: String? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.record.debteeId")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT, name = "debtee_id")
  open var debteeId: String? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.record.mandateReference")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT, name = "mandate_reference")
  open var mandateReference: String? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.record.customerReference")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT, name = "customer_reference")
  open var customerReference: String? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.record.collectionReference")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT, name = "collection_reference")
  open var collectionReference: String? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.record.info")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT)
  open var info: String? = null

  /**
   * Receiver for outgoing payments and sender for incoming payments.
   */
  @PropertyInfo(i18nKey = "plugins.banking.account.record.receiverSender")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT, name = "receiver_sender")
  open var receiverSender: String? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.iban")
  @Field
  @get:Column(length = Constants.LENGTH_TITLE)
  open var iban: String? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.bic")
  @Field
  @get:Column(length = Constants.LENGTH_TITLE)
  open var bic: String? = null

  companion object {
    const val FIND_BY_TIME_PERIOD =  "BankAccountRecordDO_FindByTimePeriod"

  }
}
