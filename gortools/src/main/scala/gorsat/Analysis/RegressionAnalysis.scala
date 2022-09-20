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

package gorsat.Analysis

import gorsat.Commands.Analysis
import gorsat.gorsatGorIterator.MapAndListUtilities
import gorsat.process.GorJavaUtilities
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.session.GorSession

abstract class RegressionAnalysis[T: Manifest](lookUpSignature: String, session: GorSession, valCol: Int, sepVal: Option[String], imputed: Boolean, phenoFile: String, covarFile: Option[String]) extends Analysis {

  var ri: RegressionInfo =_

  override def setup(): Unit = {
    ri = session.getCache.getObjectHashMap.computeIfAbsent(lookUpSignature, _ => {
      val phenoFileTrav = MapAndListUtilities.getStringTraversable(phenoFile, session)
      val phenoNames = phenoFileTrav.head.split('\t').tail
      val (identities, phenoFileTransposed) = getIdentitiesAndPhenoFileTransposed(phenoFileTrav.tail)
      val size = phenoFileTransposed.length
      val (phenoValuesTable, phenoFilter) = getPhenotypeData(phenoFileTransposed)
      val (covarNames, covars) = getCovars(identities)

      RegressionInfo(phenoValuesTable, phenoNames, phenoFilter, covarNames, covars, size)
    }).asInstanceOf[RegressionInfo]
  }

  private def getIdentitiesAndPhenoFileTransposed(phenoTable: Iterable[String]) = {
    phenoTable.map(_.split('\t')).map(cols => (cols.head, cols.tail)).toArray.unzip
  }

  private def getPhenotypeData(phenoFileTransposed: Array[Array[String]]): (Array[Array[T]], Array[Array[Boolean]]) = {
    phenoFileTransposed.transpose.map(_.map(parsePhenoValue).unzip).unzip
  }

  protected def parsePhenoValue(s: String): (T, Boolean)

  protected def getCovars(identities: Array[String]): (Array[String], Array[Array[Double]]) = {
    covarFile match {
      case Some(covarFileName) =>
        val lines = MapAndListUtilities.getStringTraversable(covarFileName, session)
        val covarNames = lines.head.split('\t').tail
        val idToCovars = lines.tail.map(_.split('\t'))
          .map(entries => (entries.head, entries.tail.map(_.toDouble))).toMap
        val covarTable = identities.map(id => idToCovars.get(id) match {
          case Some(covs) => covs
          case None => throw new GorDataException("Covariates for sample\t" + id + "\t missing.")
        }).transpose
        (covarNames, covarTable)
      case None => (Array.empty, Array.empty)
    }
  }

  def doubleFilter[S]: (Array[S], Array[Boolean], Array[Boolean], Array[S]) => Int =
    (in, filter1, filter2, out) => {
      var i = 0
      var j = 0
      while (i < in.length) {
        if (filter1(i) && filter2(i)) {
          out(j) = in(i)
          j += 1
        }
        i += 1
      }
      j
    }

  lazy val chars2dose: (Char, Char) => Double = (char1: Char, char2: Char) => {
    GorJavaUtilities.pArray(char1) + 2 * GorJavaUtilities.pArray(char2)
  }

  val setGenotypesAndFilter: (String, Array[Double], Array[Boolean]) => Unit = {
    sepVal match {
      case Some(sep) => setGenotypesAndFilterSep(sep)
      case None =>
        if (imputed) setGenotypesAndFilterImputed
        else setGenotypesAndFilterDefault
    }
  }

  lazy val setGenotypesAndFilterSep: String =>  (String, Array[Double], Array[Boolean]) => Unit =
    sep => {
      (values, dos, filter) => {
        val len = ri.size
        var tfIdx = 0
        var vIdx = 0
        while (tfIdx < len) {
          val nextIdx = values.indexOf(sep, vIdx)
          val cand = if (nextIdx > 0) values.substring(vIdx, nextIdx) else values.substring(vIdx)
          if (cand == "") {
            filter(tfIdx) = false
          } else {
            filter(tfIdx) = true
            dos(tfIdx) = cand.toDouble
          }
          vIdx = nextIdx + sep.length
          tfIdx += 1
        }
      }
    }

  lazy val setGenotypesAndFilterImputed: (String, Array[Double], Array[Boolean]) => Unit =
    (values, dos, filter) => {
      val len = ri.size
      var tfIdx = 0
      var vIdx = 0
      while (tfIdx < len) {
        val (c1, c2) = (values(vIdx), values(vIdx + 1))
        if (c1 == ' ') {
          filter(tfIdx) = false
        } else {
          filter(tfIdx) = true
          dos(tfIdx) = chars2dose(c1, c2)
        }
        vIdx += 2
        tfIdx += 1
      }
    }

  lazy val setGenotypesAndFilterDefault: (String, Array[Double], Array[Boolean]) => Unit =
    (values, dos, filter) => {
      val len = ri.size
      var tfIdx = 0
      var vIdx = 0
      while (tfIdx < len) {
        dos(tfIdx) = values(vIdx).toDouble
        filter(tfIdx) = values(vIdx) != '3' //3 denotes missing value.
        vIdx += 1
        tfIdx += 1
      }
    }

  protected def setupRegressionData(genoType: Array[Double], filter: Array[Boolean], phenos: Array[T], idx: Int): (Array[Array[Double]], Array[T], Int) = {
    val x = ri.xs(idx)
    val y = ri.ys(idx)
    val phenoFilter = ri.phenoFilter(idx)
    val numberOfSamples = doubleFilter(genoType, filter, phenoFilter, x.head)
    x.tail zip ri.covars foreach { case (xc, cc) =>
      doubleFilter(cc, filter, phenoFilter, xc)
    }
    doubleFilter(phenos, filter, phenoFilter, y)
    (x, y, numberOfSamples)
  }

  case class RegressionInfo(
                                phenoTable: Array[Array[T]],
                                phenoNames: Array[String],
                                phenoFilter: Array[Array[Boolean]],
                                covarNames: Array[String],
                                covars: Array[Array[Double]],
                                size: Int
                              ) {
    val betaLength: Int = covars.length + 2
    val betaNames: Array[String] = Array.ofDim(betaLength)
    betaNames(0) = "Intercept"
    betaNames(1) = "Genotype"
    if (covarNames.length > 0) System.arraycopy(covarNames, 0, betaNames, 2, covarNames.length)

    val xs: Array[Array[Array[Double]]] = Array.ofDim(phenoNames.length, betaLength - 1, size)
    val ys: Array[Array[T]] = Array.ofDim[T](phenoNames.length, size)
    val genoFilter: Array[Boolean] = Array.ofDim(size)
    val genos: Array[Double] = Array.ofDim(size)
  }
}
