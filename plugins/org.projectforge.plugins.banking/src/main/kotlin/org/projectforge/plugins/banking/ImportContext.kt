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

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate
import kotlin.reflect.KMutableProperty1

class ImportContext {
  class MatchingPair(val read: BankAccountRecord? = null, val dbRecord: BankAccountRecordDO? = null)

  val foundColumns = mutableMapOf<String, String>()
  val ignoredColumns = mutableListOf<String>()
  val pairsByDate = mutableMapOf<LocalDate, List<MatchingPair>>()

  var fromDate: LocalDate? = null
  var untilDate: LocalDate? = null

  @JsonIgnore // for debugging as json
  val lineMapping = mutableMapOf<Int, KMutableProperty1<BankAccountRecord, Any>>()
  var readTransactions = mutableListOf<BankAccountRecord>()
  var databaseTransactions: List<BankAccountRecordDO>? = null

  internal fun createPairs() {
    if (fromDate == null || untilDate == null || fromDate!! > untilDate!!) {
      return // Shouldn't occur
    }
    var date = fromDate!!
    for (i in 0..999999) { // Paranoia counter, max 1 mio records.
      val readByDay = readTransactions.filter { it.date == date }
      val dbRecordsByDay = databaseTransactions?.filter { it.date == date } ?: emptyList()
      val list = buildMatchingPairs(readByDay, dbRecordsByDay)
      pairsByDate[date] = list
      date = date.plusDays(1)
      if (date > untilDate) {
        break
      }
    }
  }

  private fun buildMatchingPairs(
    readByDay: List<BankAccountRecord>,
    dbRecordsByDay: List<BankAccountRecordDO>,
  ): List<MatchingPair> {
    val pairs = mutableListOf<MatchingPair>()
    if (readByDay.isEmpty()) {
      dbRecordsByDay.forEach { dbRecord ->
        pairs.add(MatchingPair(null, dbRecord))
      }
      return pairs // Nothing to import (only db records given).
    }
    val scoreMatrix = Array(readByDay.size) { IntArray(dbRecordsByDay.size) }
    for (k in 0 until readByDay.size) {
      for (l in 0 until dbRecordsByDay.size) {
        scoreMatrix[k][l] = readByDay[k].matchScore(dbRecordsByDay[l])
      }
    }
    val takenReadRecords = mutableSetOf<Int>()
    val takenDBRecords = mutableSetOf<Int>()
    for (i in 0..(readByDay.size + dbRecordsByDay.size)) { // Paranoia counter
      var maxScore = 0
      var maxK = -1
      var maxL = -1
      for (k in 0 until readByDay.size) {
        if (takenReadRecords.contains(k)) {
          continue // Entry k is already taken.
        }
        for (l in 0 until dbRecordsByDay.size) {
          if (takenDBRecords.contains(l)) {
            continue // Entry l is already taken.
          }
          if (scoreMatrix[k][l] > maxScore) {
            maxScore = scoreMatrix[k][l]
            maxK = k
            maxL = l
          }
        }
      }
      if (maxScore < 1) {
        break // No matching pair exists anymore.
      }
      takenReadRecords.add(maxK)
      takenDBRecords.add(maxL)
      pairs.add(MatchingPair(readByDay[maxK], dbRecordsByDay[maxL]))
    }
    // Now, add the unmatching records
    for (k in 0 until readByDay.size) {
      if (takenReadRecords.contains(k)) {
        continue // Entry k is already taken.
      }
      pairs.add(MatchingPair(readByDay[k], null))
    }
    for (l in 0 until dbRecordsByDay.size) {
      if (takenDBRecords.contains(l)) {
        continue // Entry l is already taken.
      }
      pairs.add(MatchingPair(null, dbRecordsByDay[l]))
    }
    return pairs
  }

  fun analyzeReadTransactions() {
    readTransactions.removeIf { it.date == null }
    readTransactions.sortBy { it.date }
    if (readTransactions.isEmpty()) {
      return // nothing to do.
    }
    fromDate = readTransactions.first().date!!
    untilDate = readTransactions.last().date!!
  }
}
