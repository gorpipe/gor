/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package gorsat.parser

import java.text.SimpleDateFormat
import java.time.{Duration, LocalDate, LocalDateTime}
import java.util.Date
import gorsat.parser.FunctionSignature._
import gorsat.parser.FunctionTypes.{iFun, lFun, sFun}

import java.time.format.DateTimeFormatter

object TimeAndDateFunctions {
  def register(functions: FunctionRegistry): Unit = {
    functions.register("EPOCH", getSignatureEmpty2Long(() => epoch()), epoch _)
    functions.register("EPOCH", getSignatureStringString2Long(epochWithDateAndFormat), epochWithDateAndFormat _)
    functions.register("EDATE", getSignatureLong2String(edate), edate _)
    functions.register("EDATE", getSignatureLongString2String(edateWithFormat), edateWithFormat _)
    functions.register("DATE", getSignatureEmpty2String(currentdate), currentdate _)
    functions.register("DATE", getSignatureString2String(currentdateWithFormat), currentdateWithFormat _)
    functions.register("DAYDIFF", getSignatureStringStringString2Long(daydiff), daydiff _)
    functions.register("MONTHDIFF", getSignatureStringStringString2Long(monthdiff), monthdiff _)
    functions.register("YEARDIFF", getSignatureStringStringString2Long(yeardiff), yeardiff _)
    functions.register("CURRENTDATE", getSignatureEmpty2String(currentdate), currentdate _)
    functions.register("CURRENTDATE", getSignatureString2String(currentdateWithFormat), currentdateWithFormat _)
    functions.register("ADDYEARS", getSignatureStringStringInt2String(addyears), addyears _)
    functions.register("ADDMONTHS", getSignatureStringStringInt2String(addmonths), addmonths _)
    functions.register("ADDDAYS", getSignatureStringStringInt2String(adddays), adddays _)
    functions.register("YEAR", getSignatureStringString2Int(year), year _)
    functions.register("MONTH", getSignatureStringString2Int(month), month _)
    functions.register("DAYOFWEEK", getSignatureStringString2Int(dayofweek), dayofweek _)
    functions.register("DAYOFMONTH", getSignatureStringString2Int(dayofmonth), dayofmonth _)
    functions.register("DAYOFYEAR", getSignatureStringString2Int(dayofyear), dayofyear _)
  }

  def yeardiff(format: sFun, date1: sFun, date2: sFun): lFun = {
    cvp => {
      val dateFormat = new SimpleDateFormat(format(cvp))
      val parsedDate1 = dateFormat.parse(date1(cvp)).toInstant
      val parsedDate2 = dateFormat.parse(date2(cvp)).toInstant
      Duration.between(parsedDate1, parsedDate2).toDays / 365
    }
  }

  def monthdiff(format: sFun, date1: sFun, date2: sFun): lFun = {
    cvp => {
      val dateFormat = new SimpleDateFormat(format(cvp))
      val parsedDate1 = dateFormat.parse(date1(cvp)).toInstant
      val parsedDate2 = dateFormat.parse(date2(cvp)).toInstant
      Duration.between(parsedDate1, parsedDate2).toDays / 30
    }
  }

  def daydiff(format: sFun, date1: sFun, date2: sFun): lFun = {
    cvp => {
      val dateFormat = new SimpleDateFormat(format(cvp))
      val parsedDate1 = dateFormat.parse(date1(cvp)).toInstant
      val parsedDate2 = dateFormat.parse(date2(cvp)).toInstant
      Duration.between(parsedDate1, parsedDate2).toDays
    }
  }

  def epoch(): lFun = _ => new Date().getTime

  def epochWithDateAndFormat(ex1: sFun, ex2: sFun): lFun = {
    cvp => new SimpleDateFormat(ex2(cvp)).parse(ex1(cvp)).getTime
  }

  def edate(ex1: lFun): sFun = cvp => {
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(ex1(cvp)))
  }

  def edateWithFormat(ex1: lFun, ex2: sFun): sFun = cvp => {
    new SimpleDateFormat(ex2(cvp)).format(new Date(ex1(cvp)))
  }

  def currentdate(): sFun = cvp => {
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
  }

  def currentdateWithFormat(format: sFun): sFun = cvp => {
    new SimpleDateFormat(format(cvp)).format(new Date())
  }

  def addyears(format: sFun, date: sFun, years: iFun): sFun = cvp => {
    val formatString = format(cvp)
    val formatter = DateTimeFormatter.ofPattern(formatString)
    val d = LocalDate.parse(date(cvp), formatter)
    val d2 = d.plusYears(years(cvp))
    d2.format(formatter)
  }

  def addmonths(format: sFun, date: sFun, months: iFun): sFun = cvp => {
    val formatString = format(cvp)
    val formatter = DateTimeFormatter.ofPattern(formatString)
    val d = LocalDate.parse(date(cvp), formatter)
    val d2 = d.plusMonths(months(cvp))
    d2.format(formatter)
  }

  def adddays(format: sFun, date: sFun, days: iFun): sFun = cvp => {
    val formatString = format(cvp)
    val formatter = DateTimeFormatter.ofPattern(formatString)
    val d = LocalDate.parse(date(cvp), formatter)
    val d2 = d.plusDays(days(cvp))
    d2.format(formatter)
  }

  def year(format: sFun, date: sFun): iFun = cvp => {
    val formatString = format(cvp)
    val formatter = DateTimeFormatter.ofPattern(formatString)
    val d = LocalDate.parse(date(cvp), formatter)
    d.getYear
  }

  def month(format: sFun, date: sFun): iFun = cvp => {
    val formatString = format(cvp)
    val formatter = DateTimeFormatter.ofPattern(formatString)
    val d = LocalDate.parse(date(cvp), formatter)
    d.getMonthValue
  }

  def dayofweek(format: sFun, date: sFun): iFun = cvp => {
    val formatString = format(cvp)
    val formatter = DateTimeFormatter.ofPattern(formatString)
    val d = LocalDate.parse(date(cvp), formatter)
    d.getDayOfWeek.getValue
  }

  def dayofmonth(format: sFun, date: sFun): iFun = cvp => {
    val formatString = format(cvp)
    val formatter = DateTimeFormatter.ofPattern(formatString)
    val d = LocalDate.parse(date(cvp), formatter)
    d.getDayOfMonth
  }

  def dayofyear(format: sFun, date: sFun): iFun = cvp => {
    val formatString = format(cvp)
    val formatter = DateTimeFormatter.ofPattern(formatString)
    val d = LocalDate.parse(date(cvp), formatter)
    d.getDayOfYear
  }
}
