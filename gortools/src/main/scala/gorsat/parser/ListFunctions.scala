/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
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

import java.util.regex.Pattern

import gorsat.Analysis.Cols2ListAnalysis
import gorsat.Commands.ColumnSelection
import gorsat.parser.FunctionSignature._
import gorsat.parser.FunctionTypes.{bFun, dFun, iFun, sFun}
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.model.ColumnValueProvider

import scala.collection.mutable

object ListFunctions {
  //noinspection VarCouldBeVal
  private var dummyCvp: ColumnValueProvider = _

  def register(functions: FunctionRegistry): Unit = {
    functions.register("LISTNUMMIN", getSignatureString2Double(listNumMin), listNumMin _)
    functions.register("LISTNUMMAX", getSignatureString2Double(listNumMax), listNumMax _)
    functions.register("LISTNUMSUM", getSignatureString2Double(listNumSum), listNumSum _)
    functions.register("LISTNUMSTD", getSignatureString2Double(listNumStd), listNumStd _)
    functions.register("LISTNUMAVG", getSignatureString2Double(listNumAvg), listNumAvg _)
    functions.register("LISTFIRST", getSignatureString2String(listFirst), listFirst _)
    functions.register("LISTFIRST", getSignatureStringString2String(listFirstWithDelimiter), listFirstWithDelimiter _)
    functions.register("LISTSECOND", getSignatureString2String(listSecond), listSecond _)
    functions.register("LISTSECOND", getSignatureStringString2String(listSecondWithDelimiter), listSecondWithDelimiter _)
    functions.register("LISTREVERSE", getSignatureString2String(listReverse), listReverse _)
    functions.register("LISTREVERSE", getSignatureStringString2String(listReverseWithSeparator), listReverseWithSeparator _)
    functions.register("LISTSORTASC", getSignatureString2String(listSortAsc), listSortAsc _)
    functions.register("LISTSORTDESC", getSignatureString2String(listSortDesc), listSortDesc _)
    functions.register("LISTNUMSORTASC", getSignatureString2String(listNumSortAsc), listNumSortAsc _)
    functions.register("LISTNUMSORTDESC", getSignatureString2String(listNumSortDesc), listNumSortDesc _)
    functions.register("LISTINDEX", getSignatureStringString2Int(listIndex), listIndex _)
    functions.register("LISTINDEX", getSignatureStringStringString2Int(listIndexWithDelimiter), listIndexWithDelimiter _)
    functions.register("LISTCOUNT", getSignatureString2String(listCount), listCount _)
    functions.register("LISTCOUNT", getSignatureStringString2String(listCountWithDelimiter), listCountWithDelimiter _)
    functions.register("LISTLAST", getSignatureString2String(listLast), listLast _)
    functions.register("LISTLAST", getSignatureStringString2String(listLastWithDelimiter), listLastWithDelimiter _)
    functions.register("LISTTAIL", getSignatureString2String(listTail), listTail _)
    functions.register("LISTTAIL", getSignatureStringString2String(listTailWithDelimiter), listTailWithDelimiter _)
    functions.registerWithOwner("LISTMAP", getSignatureStringString2String(removeOwner(listMap)), listMap _)
    functions.registerWithOwner("LISTMAP", getSignatureStringStringString2String(removeOwner(listMapWithDelimiter)), listMapWithDelimiter _)
    functions.registerWithOwner("LISTFILTER", getSignatureStringString2String(removeOwner(listFilter)), listFilter _)
    functions.registerWithOwner("LISTFILTER", getSignatureStringStringString2String(removeOwner(listFilterWithDelimiter)), listFilterWithDelimiter _)
    functions.register("LISTZIP", getSignatureStringString2String(listZip), listZip _)
    functions.register("LISTZIP", getSignatureStringStringStringString2String(listZipWithSeparatorAndDelimiter), listZipWithSeparatorAndDelimiter _)
    functions.registerWithOwner("LISTZIPFILTER", getSignatureStringStringString2String(removeOwner(listZipFilter)), listZipFilter _)
    functions.registerWithOwner("LISTZIPFILTER", getSignatureStringStringStringString2String(removeOwner(listZipFilterWithDelimiter)), listZipFilterWithDelimiter _)
    functions.register("LISTSIZE", getSignatureString2Int(listSize), listSize _)
    functions.register("LISTSIZE", getSignatureStringString2Int(listSizeWithDelimiter), listSizeWithDelimiter _)
    functions.register("LISTDIST", getSignatureString2String(listDist), listDist _)
    functions.register("LISTMAX", getSignatureString2String(listMax), listMax _)
    functions.register("LISTMIN", getSignatureString2String(listMin), listMin _)
    functions.register("LISTTRIM", getSignatureString2String(listTrim), listTrim _)
    functions.register("LISTNTH", getSignatureStringInt2String(listNth), listNth _)
    functions.register("LISTNTH", getSignatureStringIntString2String(listNthWithDelimiter), listNthWithDelimiter _)
    functions.register("LISTCOMB", getSignatureStringInt2String(listComb), listComb _)
    functions.register("LISTADD", getSignatureStringString2String(listAdd), listAdd _)
    functions.register("LISTADD", getSignatureStringStringString2String(listAddWithDelimiter), listAddWithDelimiter _)
    functions.register("LISTCOMB", getSignatureStringIntInt2String(listComb2), listComb2 _)
    functions.registerWithOwner("FSVMAP", getSignatureStringIntStringString2String(removeOwner(fsvMap)), fsvMap _)
    functions.register("LISTHASANY", getSignatureStringString2Boolean(listHasAny), listHasAny _)
    functions.register("LISTHASANY", getSignatureStringStringList2Boolean(listHasAnyWithStringLiterals), listHasAnyWithStringLiterals _)
    functions.register("CSLISTHASANY", getSignatureStringString2Boolean(csListHasAny), csListHasAny _)
    functions.register("CSLISTHASANY", getSignatureStringStringList2Boolean(csListHasAnyWithStringLiterals), csListHasAnyWithStringLiterals _)
    functions.register("CONTAINSCOUNT", getSignatureStringStringList2Int(containsCount), containsCount _)
    functions.register("CSCONTAINSCOUNT", getSignatureStringStringList2Int(csContainsCount), csContainsCount _)
    functions.register("CONTAINS", getSignatureStringStringList2Boolean(contains), contains _)
    functions.register("CONTAINSALL", getSignatureStringStringList2Boolean(contains), contains _)
    functions.register("CSCONTAINS", getSignatureStringStringList2Boolean(csContains), csContains _)
    functions.register("CSCONTAINSALL", getSignatureStringStringList2Boolean(csContains), csContains _)
    functions.register("CONTAINSANY", getSignatureStringStringList2Boolean(containsAny), containsAny _)
    functions.register("CSCONTAINSANY", getSignatureStringStringList2Boolean(csContainsAny), csContainsAny _)
    functions.register("CSCONTAINSANY", getSignatureStringStringList2Boolean(csContainsAny), csContainsAny _)
    functions.register("LISTHASCOUNT", getSignatureStringStringList2Int(listHasCount), listHasCount _)
    functions.register("CSLISTHASCOUNT", getSignatureStringStringList2Int(csListHasCount), csListHasCount _)
    functions.registerWithOwner("COLS2LIST", getSignatureString2String(removeOwner(cols2List)), cols2List _)
    functions.registerWithOwner("COLS2LIST", getSignatureStringString2String(removeOwner(cols2ListCustomSep)), cols2ListCustomSep _)
    functions.registerWithOwner("COLS2LISTMAP", getSignatureStringString2String(removeOwner(cols2Listmap)), cols2Listmap _)
    functions.registerWithOwner("COLS2LISTMAP", getSignatureStringStringString2String(removeOwner(cols2ListmapCustomSep)), cols2ListmapCustomSep _)
  }

  def cols2Listmap(owner: ParseArith, columnSelection: sFun, expression: sFun): sFun = {
    cols2ListmapCustomSep(owner, columnSelection, expression, _ => ",")
  }


  def cols2ListmapCustomSep(owner: ParseArith, columnSelection: sFun, expression: sFun, sep: sFun): sFun = {
    def getColumnSelection = {
      try {
        ColumnSelection(owner.getHeader.toString, columnSelection(dummyCvp), owner.context, owner.executeNor)
      } catch {
        case _: NullPointerException => throw new GorParsingException("COLS2LISTMAP expects a quoted column selection " +
          "expression")
      }
    }
    def getExpression = {
      try {
        val exprSrc = expression(dummyCvp)
        Cols2ListAnalysis.compileExpression(exprSrc, owner.getHeader)
      } catch {
        case _: NullPointerException => throw new GorParsingException("COLS2LISTMAP expects a quoted expression")
      }
    }

    val columns: ColumnSelection = getColumnSelection
    val ex = getExpression
    val separator = sep(dummyCvp)
    val offset = if (owner.executeNor) 2 else 0
    cvp => {
      val buffer = new java.lang.StringBuilder()
      Cols2ListAnalysis.addColumnValuesAsList(columns, offset, separator, cvp, ex, buffer)
      buffer.toString
    }
  }

  def cols2List(owner: ParseArith, columnSelection: sFun): sFun = {
    cols2ListCustomSep(owner, columnSelection, _ => ",")
  }

  def cols2ListCustomSep(owner: ParseArith, columnSelection: sFun, sep: sFun): sFun = {
    def getColumnSelection = {
      try {
        ColumnSelection(owner.getHeader.toString, columnSelection(dummyCvp), owner.context, owner.executeNor)
      } catch {
        case _: NullPointerException => throw new GorParsingException("COLS2LIST expects a qouted column selection " +
          "expression")
      }
    }

    val columns: ColumnSelection = getColumnSelection
    val separator = sep(dummyCvp)
    val offset = if (owner.executeNor) 2 else 0
    val ex = (_: ColumnValueProvider, x: CharSequence) => x
    cvp => {
      val buffer = new java.lang.StringBuilder()
      Cols2ListAnalysis.addColumnValuesAsList(columns, offset, separator, cvp, ex, buffer)
      buffer.toString
    }
  }

  def listHasCount(ex1: sFun, ex2: List[String]): iFun = {
    cvp => {
      val tmp = ex1(cvp).split(",", -1).map(b => b.toUpperCase)
      ex2.map(x => x.toUpperCase).count(x => tmp.contains(x))
    }
  }

  def csListHasCount(ex1: sFun, ex2: List[String]): iFun = {
    cvp => {
      val tmp = ex1(cvp).split(",", -1)
      ex2.count(x => tmp.contains(x))
    }
  }

  def csContainsAny(ex1: sFun, ex2: List[String]): bFun = {
    cvp => {
      ex2.exists(ex1(cvp).contains(_))
    }
  }

  def containsAny(ex1: sFun, ex2: List[String]): bFun = {
    val w = ex2.map(x => x.toUpperCase)
    cvp => {
      w.exists(ex1(cvp).toUpperCase.contains(_))
    }
  }

  def csContains(ex1: sFun, ex2: List[String]): bFun = {
    cvp => {
      ex2.forall(ex1(cvp).contains(_))
    }
  }

  def contains(ex1: sFun, ex2: List[String]): bFun = {
    val w = ex2.map(x => x.toUpperCase)
    cvp => {
      w.forall(ex1(cvp).toUpperCase.contains(_))
    }
  }

  def containsCount(ex1: sFun, ex2: List[String]): iFun = {
    val w = ex2.map(x => x.toUpperCase)
    cvp => {
      w.count(ex1(cvp).toUpperCase.contains(_))
    }
  }

  def csContainsCount(ex1: sFun, ex2: List[String]): iFun = {
    cvp => {
      ex2.count(ex1(cvp).contains(_))
    }
  }

  def csListHasAny(ex1: sFun, ex2: sFun): bFun = {
    cvp => {
      val tmp = ex2(cvp).split(",", -1)
      ex1(cvp).split(",", -1).exists(tmp.contains(_))
    }
  }

  def csListHasAnyWithStringLiterals(ex1: sFun, ex2: List[String]): bFun = {
    cvp => {
      ex1(cvp).split(",", -1).exists(ex2.contains(_))
    }
  }

  def listHasAny(ex1: sFun, ex2: sFun): bFun = {
    cvp => {
      val tmp = ex2(cvp).split(",", -1).map(x => x.toUpperCase)
      ex1(cvp).split(",", -1).map(b => b.toUpperCase).exists(tmp.contains(_))
    }
  }

  def listHasAnyWithStringLiterals(ex1: sFun, ex2: List[String]): bFun = {
    val w = ex2.map(x => x.toUpperCase)
    cvp => {
      ex1(cvp).split(",", -1).map(b => b.toUpperCase).exists(w.contains(_))
    }
  }


  def fsvMap(owner: ParseArith, ex1: sFun, ex2: iFun, ex3: sFun, ex4: sFun): sFun = {
    val filter = owner.createSubFilter()
    val calcType = filter.compileCalculation(ex3(dummyCvp)).charAt(0)
    val func = calcType match {
      case 'S' => cvp: ColumnValueProvider => filter.evalStringFunction(cvp)
      case 'D' => cvp: ColumnValueProvider => filter.evalDoubleFunction(cvp).toString
      case 'L' => cvp: ColumnValueProvider => filter.evalLongFunction(cvp).toString
      case 'I' => cvp: ColumnValueProvider => filter.evalIntFunction(cvp).toString
      case _ => _: ColumnValueProvider => ""
    }
    cvp => fsvMapInner(func, cvp, ex1(cvp), ex2(cvp), ex4(cvp))
  }

  private def fsvMapInner(f: ColumnValueProvider => String, cvp: ColumnValueProvider, sourceList: String,
                          itemSize: Int, delimiter: String = ","): String = {
    val builder = new java.lang.StringBuilder()
    val listCvp = FixedSizeIteratorCvp(cvp, sourceList, itemSize)
    while (listCvp.hasNext) {
      listCvp.next()
      builder.append(f(listCvp))
      if (listCvp.hasNext) builder.append(delimiter)
    }
    builder.toString
  }

  def listAdd(list: sFun, item: sFun): sFun = {
    cvp => {
      val l = list(cvp)
      val i = item(cvp)
      if (l.isEmpty) i else l + "," + i
    }
  }

  def listAddWithDelimiter(list: sFun, item: sFun, delim: sFun): sFun = {
    cvp => {
      val l = list(cvp)
      val i = item(cvp)
      if (l.isEmpty) i else l + delim(cvp) + i
    }
  }

  def listComb(ex1: sFun, ex2: iFun): sFun = {
    cvp =>
      listCombInner(ex1(cvp), ex2(cvp))
  }

  def listComb2(ex1: sFun, ex2: iFun, ex3: iFun): sFun = {
    cvp =>
      listCombInner(ex1(cvp), ex2(cvp), ex3(cvp))
  }

  private def listCombInner(string: String, n: Int): String = listCombInner(string, n, n)

  private def listCombInner(string: String, from: Int, upTo: Int): String = {
    val strings = string.split(',')
    val stringBuilder = new mutable.StringBuilder()
    var n = upTo
    while (n >= from) {
      strings.combinations(n).foreach(comb => {
        stringBuilder.append(comb.head)
        comb.tail.foreach(s => {
          stringBuilder.append(',')
          stringBuilder.append(s)
        })
        stringBuilder.append(';')
      })
      n -= 1
    }
    stringBuilder.substring(0, stringBuilder.length - 1)
  }

  def listNth(ex1: sFun, ex2: iFun): sFun = {
    cvp => listNthInner(cvp, ex1(cvp), ex2(cvp))
  }

  def listNthWithDelimiter(ex1: sFun, ex2: iFun, ex3: sFun): sFun = {
    cvp => listNthInner(cvp, ex1(cvp), ex2(cvp), ex3(cvp))
  }

  //Returns the Nth (starting at 1) element in a list separated with delimiter.
  private def listNthInner(cvp: ColumnValueProvider, sourceList: String, N: Int, delimiter: String = ","): String = {
    // Early exit for special cases
    if (sourceList.length < N) return ""
    if (delimiter.length == 0) return sourceList.substring(N - 1, N)

    // General case
    val listCvp = IteratorCvp(cvp, sourceList, delimiter)
    var i = 1
    while (listCvp.hasNext && i < N) {
      listCvp.next()
      i += 1
    }

    if (i == N) {
      listCvp.next()
      listCvp.stringValue(SpecialColumns.ListItem)
    } else {
      ""
    }
  }

  def listSize(ex: sFun): iFun = {
    cvp =>
      countArgs(ex(cvp))
  }

  def listSizeWithDelimiter(ex1: sFun, ex2: sFun): iFun = {
    cvp =>
      countArgs(ex1(cvp), ex2(cvp))
  }

  private def countArgs(string: String, delimiter: String = ","): Int = {
    if (string == "" || string == null) return 0
    if (delimiter.length == 0) return string.length
    var counter = 1
    var lastIdx = 0
    if (delimiter.length == 1) {
      val del = delimiter.charAt(0)
      var i = 0
      while (i < string.length) {
        if (string.charAt(i) == del) counter += 1
        i += 1
      }
    }
    else if (dontTreatAsRegex(delimiter)) {
      val del1 = delimiter.charAt(0)
      val del2 = delimiter.charAt(1)
      var i = 0
      val upTo = string.length - 1
      while (i < upTo) {
        if (del1 == string.charAt(i) && del2 == string.charAt(i + 1)) {
          counter += 1
          i += 1
        }
        i += 1
      }
    }
    else {
      val matcher = Pattern.compile(delimiter).matcher(string)
      counter = 1
      while (matcher.find(lastIdx)) {
        counter += 1
        lastIdx = matcher.end
      }
    }
    counter
  }

  def listTrim(ex: sFun): sFun = {
    cvp => {
      ex(cvp).split(',').map(_.trim).mkString(",")
    }
  }

  def listZipFilter(owner: ParseArith, ex1: sFun, ex2: sFun, ex3: sFun): sFun = {
    val filter = owner.createSubFilter()
    filter.compileFilter(ex3(dummyCvp))
    cvp =>
      listZipFilterInner(cvp, filter, ex1(cvp), ex2(cvp))

  }

  def listZipFilterWithDelimiter(owner: ParseArith, ex1: sFun, ex2: sFun, ex3: sFun, ex4: sFun): sFun = {
    val filter = owner.createSubFilter()
    filter.compileFilter(ex3(dummyCvp))
    cvp =>
      listZipFilterInner(cvp, filter, ex1(cvp), ex2(cvp), ex4(cvp))

  }

  def listZipFilterInner(cvp: ColumnValueProvider, filter: ParseArith, arg1: String, arg2: String,
                         delimiter: String = ","): String = {
    val buffer = new java.lang.StringBuilder()

    val sourceList = IteratorCvp(cvp, arg1, delimiter)
    val conditionList = IteratorCvp(cvp, arg2, delimiter)

    var needDelimiter = false
    while (sourceList.hasNext && conditionList.hasNext) {
      conditionList.next()
      sourceList.next()
      if (filter.evalBooleanFunction(conditionList)) {
        if (needDelimiter) {
          buffer.append(delimiter)
        }
        needDelimiter = true
        buffer.append(sourceList.stringValue(SpecialColumns.ListItem))
      }
    }
    buffer.toString
  }

  def listZip(ex1: sFun, ex2: sFun): sFun = {
    cvp =>
      listZipInner(ex1(cvp), ex2(cvp))
  }

  def listZipWithSeparatorAndDelimiter(ex1: sFun, ex2: sFun, ex3: sFun, ex4: sFun): sFun = {
    cvp =>
      listZipInner(ex1(cvp), ex2(cvp), ex3(cvp), ex4(cvp))
  }

  private def listZipInner(arg1: String, arg2: String, delimiter: String = ",", separator: String = ";"): String = {
    if (arg1 == "" || arg2 == "") return ""

    delimiter.length match {
      case 0 => listZipNoDelimiter(arg1, arg2, separator)
      case 1 => listZipSingleCharDelimiter(arg1, arg2, delimiter, separator)
      case _ => listZipGeneralDelimiter(arg1, arg2, delimiter, separator)
    }
  }

  private def listZipNoDelimiter(arg1: String, arg2: String, separator: String): String = {
    val minLength = if (arg1.length < arg2.length) arg1.length else arg2.length
    val bufferLength = (2 + separator.length) * minLength - 1
    val buffer = new java.lang.StringBuilder(bufferLength)

    var i = 1
    var j = 1

    buffer.append(arg1.charAt(0))
    buffer.append(separator)
    buffer.append(arg2.charAt(0))
    while (i < arg1.length && j < arg2.length) {
      buffer.append(arg1.charAt(i))
      buffer.append(separator)
      buffer.append(arg2.charAt(j))
      i += 1
      j += 1
    }
    buffer.toString
  }

  private def listZipSingleCharDelimiter(arg1: String, arg2: String, delimiter: String, separator: String): String = {
    val buffer = new java.lang.StringBuilder()
    val del = delimiter.charAt(0)
    var i = 0
    var j = 0
    while (true) {
      while (i < arg1.length && arg1.charAt(i) != del) {
        buffer.append(arg1.charAt(i))
        i += 1
      }
      buffer.append(separator)
      while (j < arg2.length && arg2.charAt(j) != del) {
        buffer.append(arg2.charAt(j))
        j += 1
      }
      if (i == arg1.length || j == arg2.length) return buffer.toString
      buffer.append(delimiter)
      i += 1
      j += 1
    }
    buffer.toString
  }

  private def listZipGeneralDelimiter(arg1: String, arg2: String, delimiter: String, separator: String): String = {
    val buffer = new java.lang.StringBuilder()

    val firstList = IteratorCvp(dummyCvp, arg1, delimiter)
    val secondList = IteratorCvp(dummyCvp, arg2, delimiter)

    var needDelimiter = false
    while (firstList.hasNext && secondList.hasNext) {
      firstList.next()
      secondList.next()
      val firstValue = firstList.stringValue()
      val secondValue = secondList.stringValue()
      if (needDelimiter) {
        buffer.append(delimiter)
      } else {
        needDelimiter = true
      }
      buffer.append(firstValue)
      buffer.append(separator)
      buffer.append(secondValue)
    }
    buffer.toString
  }

  def listFilter(owner: ParseArith, ex1: sFun, ex2: sFun): sFun = {
    val filter = owner.createSubFilter()
    filter.compileFilter(ex2(dummyCvp))
    cvp => listFilterInner(cvp, filter, ex1(cvp), ex2(cvp))
  }

  def listFilterWithDelimiter(owner: ParseArith, ex1: sFun, ex2: sFun, ex3: sFun): sFun = {
    val filter = owner.createSubFilter()
    filter.compileFilter(ex2(dummyCvp))
    cvp => listFilterInner(cvp, filter, ex1(cvp), ex2(cvp), ex3(cvp))
  }

  private def listFilterInner(cvp: ColumnValueProvider, filter: ParseArith, arg1: String, arg2: String,
                              delimiter: String = ","): String = {
    val buffer = new java.lang.StringBuilder()

    val sourceList = IteratorCvp(cvp, arg1, delimiter)

    var needDelimiter = false
    while (sourceList.hasNext) {
      sourceList.next()
      if (filter.evalBooleanFunction(sourceList)) {
        if (needDelimiter) {
          buffer.append(delimiter)
        }
        needDelimiter = true
        buffer.append(sourceList.stringValue(SpecialColumns.ListItem))
      }
    }
    buffer.toString
  }

  def listMap(owner: ParseArith, ex1: sFun, ex2: sFun): sFun = {
    val filter = owner.createSubFilter()
    val calcType = filter.compileCalculation(ex2(dummyCvp)).charAt(0)
    val func = calcType match {
      case 'S' => cvp: ColumnValueProvider => filter.evalStringFunction(cvp)
      case 'D' => cvp: ColumnValueProvider => filter.evalDoubleFunction(cvp).toString
      case 'L' => cvp: ColumnValueProvider => filter.evalLongFunction(cvp).toString
      case 'I' => cvp: ColumnValueProvider => filter.evalIntFunction(cvp).toString
      case _ => _: ColumnValueProvider => ""
    }
    cvp => listMapInner(func, cvp, ex1(cvp), ex2(cvp))
  }

  def listMapWithDelimiter(owner: ParseArith, ex1: sFun, ex2: sFun, ex3: sFun): sFun = {
    val filter = owner.createSubFilter()
    val calcType = filter.compileCalculation(ex2(dummyCvp)).charAt(0)
    val func = calcType match {
      case 'S' => cvp: ColumnValueProvider => filter.evalStringFunction(cvp)
      case 'D' => cvp: ColumnValueProvider => filter.evalDoubleFunction(cvp).toString
      case 'L' => cvp: ColumnValueProvider => filter.evalLongFunction(cvp).toString
      case 'I' => cvp: ColumnValueProvider => filter.evalIntFunction(cvp).toString
      case _ => _: ColumnValueProvider => ""
    }
    cvp => listMapInner(func, cvp, ex1(cvp), ex2(cvp), ex3(cvp))
  }

  private def listMapInner(f: ColumnValueProvider => String, cvp: ColumnValueProvider, arg1: String, arg2: String,
                           delimiter: String = ","): String = {
    val listCvp = IteratorCvp(cvp, arg1, delimiter)
    val builder = new java.lang.StringBuilder()

    while (listCvp.hasNext) {
      listCvp.next()
      builder.append(f(listCvp))
      if (listCvp.hasNext) builder.append(delimiter)
    }
    builder.toString
  }

  def listTail(ex: sFun): sFun = {
    cvp =>
      listTailInner(ex(cvp))
  }

  def listTailWithDelimiter(ex1: sFun, ex2: sFun): sFun = {
    cvp =>
      listTailInner(ex1(cvp), ex2(cvp))
  }

  private def listTailInner(string: String, del: String = ","): String = {
    if (del.length == 0) return if (string.length == 0) string else string.substring(1)
    if (del.length == 1 || dontTreatAsRegex(del)) {
      val idx = if (del.length == 1) string.indexOf(del.charAt(0)) else string.indexOf(del)
      if (idx == -1) return ""
      string.substring(idx + del.length)
    }
    else {
      val matcher = Pattern.compile(del).matcher(string)
      if (!matcher.find()) return ""
      string.substring(matcher.end())
    }
  }

  def listLast(ex1: sFun): sFun = {
    cvp =>
      listLastInner(ex1(cvp))
  }

  def listLastWithDelimiter(ex1: sFun, ex2: sFun): sFun = {
    cvp =>
      listLastInner(ex1(cvp), ex2(cvp))
  }

  private def listLastInner(string: String, del: String = ","): String = {
    if (string.length == 0) {
      string
    } else if (del.length == 0) {
      string.substring(string.length - 1)
    } else if (del.length == 1) {
      string.substring(string.lastIndexOf(del.charAt(0)) + 1)
    } else if (dontTreatAsRegex(del)) {
      val tmp = string.lastIndexOf(del)
      if (tmp == -1) return string
      string.substring(tmp + 2)
    }
    else {
      val matcher = Pattern.compile(del).matcher(string)
      var i = string.length - 1
      while (i >= 0 && !matcher.find(i)) i -= 1
      if (i == -1) string else string.substring(matcher.end())
    }
  }


  def listIndex(ex1: sFun, ex2: sFun): iFun = {
    cvp => listIndexInner(ex1(cvp), ex2(cvp))
  }

  def listIndexWithDelimiter(ex1: sFun, ex2: sFun, ex3: sFun): iFun = {
    cvp => listIndexInner(ex1(cvp), ex2(cvp), ex3(cvp))
  }

  def listCount(ex1: sFun): sFun = {
     listCountWithDelimiter(ex1,cvp => { "," })
  }

  def listCountWithDelimiter(ex1: sFun, ex2: sFun): sFun = {
    case class CountHolder(var count : Int)
    var groupCount = scala.collection.mutable.HashMap.empty[String, CountHolder]
    cvp => {
      ex1(cvp).split(ex2(cvp)).foreach( x => {
          groupCount.get(x) match {
            case Some(x) => x.count += 1
            case None =>
              val c = CountHolder(1)
              groupCount += (x -> c)
          }
        }
      )
      groupCount.toList.sortWith((x,y) => x._1 < y._1).map(x => (x._1+';'+x._2.count)).mkString(ex2(cvp))
    }
  }

  private def listIndexInner(string: String, target: String, delimiter: String = ","): Int = {
    if (string.length == 0) {
      if (target.length == 0) return 1 else return -1
    }
    if (delimiter.length == 0) {
      if (target.length != 1) return -1
      val idx = string.indexOf(target.charAt(0))
      return if (idx < 0) idx else idx + 1
    }
    var count = 1
    if (delimiter.length == 1 || dontTreatAsRegex(delimiter)) {
      var begin = 0
      var end = string.indexOf(delimiter)
      do {
        if (end - begin == target.length && equalToSubString(string, begin, end, target)) return count
        count += 1
        begin = end + delimiter.length
        end = string.indexOf(delimiter, begin)
      } while (end != -1)

      if (string.length - begin == target.length && equalToSubString(string, begin, string.length, target)) return count
    }
    else {
      val pattern = Pattern.compile(delimiter)
      val matcher = pattern.matcher(string)
      var begin = 0
      var end = 0
      while (matcher.find()) {
        end = matcher.start()
        if (end - begin == target.length && equalToSubString(string, begin, end, target)) return count
        count += 1
        begin = matcher.end()
      }
      if (string.length - begin == target.length && equalToSubString(string, begin, string.length, target)) return count
    }
    -1
  }

  def listMin(ex: sFun): sFun = {
    cvp => {
      ex(cvp).split(',').min
    }
  }

  def listMax(ex: sFun): sFun = {
    cvp => {
      ex(cvp).split(',').max
    }
  }

  def listDist(ex: sFun): sFun = {
    cvp => {
      ex(cvp).split(',').distinct.mkString(",")
    }
  }

  def listNumSortDesc(ex: sFun): sFun = {
    cvp => {
      ex(cvp).split(',').toList.map(x => (x, x.toDouble)).sortWith((x, y) => x._2 > y._2).map(x => x._1).mkString(",")
    }
  }

  def listNumSortAsc(ex: sFun): sFun = {
    cvp => {
      ex(cvp).split(',').toList.map(x => (x, x.toDouble)).sortWith((x, y) => x._2 < y._2).map(x => x._1).mkString(",")
    }
  }

  def listSortDesc(ex: sFun): sFun = {
    cvp => {
      ex(cvp).split(',').toList.sortWith((x, y) => x > y).mkString(",")
    }
  }

  def listSortAsc(ex: sFun): sFun = {
    cvp => {
      ex(cvp).split(',').toList.sortWith((x, y) => x < y).mkString(",")
    }
  }

  def listReverse(ex: sFun): sFun = {
    cvp => {
      ex(cvp).split(",", -1).reverse.mkString(",")
    }
  }

  def listReverseWithSeparator(ex1: sFun, ex2: sFun): sFun = {
    cvp => {
      ex1(cvp).split(ex2(cvp), -1).reverse.mkString(ex2(cvp))
    }
  }

  //noinspection ScalaUnusedSymbol
  // Note that this function existed in ParseArith but was never called. Need to evaluate if the listreverse
  // function should use this
  private def listReverseInner(string: String, delimiter: String = ","): String = {
    if (delimiter.length == 0) return string.reverse
    val charArray = new Array[Char](string.length)
    var i = 0
    var j = 0
    if (delimiter.length > 1) {
      var tmp = 0
      var delIdx = string.indexOf(delimiter)
      while (delIdx != -1) {
        j = string.length - delIdx - delimiter.length
        while (tmp < delimiter.length) {
          charArray(j) = delimiter.charAt(tmp)
          tmp += 1
          j += 1
        }
        while (i < delIdx) {
          charArray(j) = string.charAt(i)
          j += 1
          i += 1
        }
        i += delimiter.length
        tmp = 0
        delIdx = string.indexOf(delimiter, i)
      }
      j = 0
      while (i < string.length) {
        charArray(j) = string.charAt(i)
        j += 1
        i += 1
      }
    }
    else {
      val del = delimiter.charAt(0)
      var delIdx = string.indexOf(del)
      while (delIdx != -1) {
        j = string.length - delIdx - delimiter.length
        charArray(j) = del
        j += 1
        while (i < delIdx) {
          charArray(j) = string.charAt(i)
          j += 1
          i += 1
        }
        i += delimiter.length
        delIdx = string.indexOf(del, i)
      }
      j = 0
      while (i < string.length) {
        charArray(j) = string.charAt(i)
        j += 1
        i += 1
      }
    }
    String.valueOf(charArray)
  }

  def listNumMax(ex: sFun): dFun = {
    cvp => {
      val x = ex(cvp).split(',').map(_.toDouble)
      var m = x.head
      x.tail.foreach(y => if (y > m) m = y)
      m
    }
  }

  def listNumMin(ex: sFun): dFun = {
    cvp => {
      val x = ex(cvp).split(',').map(_.toDouble)
      var m = x.head
      x.tail.foreach(y => if (y < m) m = y)
      m
    }
  }

  def listNumSum(ex: sFun): dFun = {
    cvp => {
      ex(cvp).split(',').map(_.toDouble).sum
    }
  }

  def listNumStd(ex: sFun): dFun = {
    cvp => {
      val x = ex(cvp).split(',').map(_.toDouble)
      val m = x.sum / x.length
      val sq = x.map(y => (y - m) * (y - m)).sum
      if (x.length < 1) Double.NaN else if (x.length == 1) 0.0 else math.sqrt(sq / (x.length - 1))
    }
  }

  def listNumAvg(ex: sFun): dFun = {
    cvp => {
      val x = ex(cvp).split(',').map(_.toDouble)
      if (x.length == 0) Double.NaN else x.sum / x.length
    }
  }

  def listFirst(ex1: sFun): sFun = {
    cvp => {
      listFirstInner(cvp, ex1(cvp))
    }
  }

  def listFirstWithDelimiter(ex1: sFun, ex2: sFun): sFun = {
    cvp => {
      listFirstInner(cvp, ex1(cvp), ex2(cvp))
    }
  }

  private def listFirstInner(cvp: ColumnValueProvider, sourceList: String, delimiter: String = ","): String = {
    if (sourceList.length == 0) return sourceList
    val listCvp = IteratorCvp(cvp, sourceList, delimiter)
    listCvp.next()
    listCvp.stringValue(SpecialColumns.ListItem)
  }

  def listSecond(ex: sFun): sFun = {
    cvp => listSecondInner(cvp, ex(cvp))
  }

  def listSecondWithDelimiter(ex1: sFun, ex2: sFun): sFun = {
    cvp => listSecondInner(cvp, ex1(cvp), ex2(cvp))
  }

  private def listSecondInner(cvp: ColumnValueProvider, sourceList: String, delimiter: String = ","): String = {
    val listCvp = IteratorCvp(cvp, sourceList, delimiter)
    if (listCvp.hasNext) {
      listCvp.next()
      if (listCvp.hasNext) {
        listCvp.next()
        return listCvp.stringValue(SpecialColumns.ListItem)
      }
    }
    ""
  }

  private val dontTreatAsRegex = (regex: String) => {
    (regex.length() == 2 && regex.charAt(0) == '\\' && ((regex.charAt(1) - '0') | ('9' - regex.charAt(1))) < 0 &&
      ((regex.charAt(1) - 'a') | ('z' - regex.charAt(1))) < 0 && ((regex.charAt(1) - 'A') | ('Z' - regex.charAt(1)))
      < 0) &&
      (regex.charAt(1) < Character.MIN_HIGH_SURROGATE || regex.charAt(1) > Character.MAX_LOW_SURROGATE)
  }

  //This function assumes that target.length == endIdx - startIdx
  @scala.annotation.tailrec
  private def equalToSubString(string: String, startIdx: Int, endIdx: Int, target: String): Boolean = {
    if (endIdx < 0) {
      equalToSubString(string, startIdx, string.length, target)
    }
    else {
      var i = startIdx
      var j = 0
      while (i < endIdx) {
        if (string.charAt(i) != target.charAt(j)) return false
        i += 1
        j += 1
      }
      true
    }
  }

}
