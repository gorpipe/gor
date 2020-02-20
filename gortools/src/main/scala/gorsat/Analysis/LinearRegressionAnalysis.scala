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

import gorsat.Regression.LinearRegression
import org.gorpipe.gor.GorSession
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj

class LinearRegressionAnalysis(lookUpSignature: String, session: GorSession, valCol: Int, sepVal: Option[String], imputed: Boolean, phenoFile: String, covarFile: Option[String])
  extends RegressionAnalysis[Double](lookUpSignature, session, valCol, sepVal, imputed, phenoFile, covarFile) {

  var linRegObjects: Array[LinearRegression] =_

  override def setup(): Unit = {
    super.setup()
    linRegObjects = Array.tabulate(ri.phenoNames.length)(_ => new LinearRegression(ri.size, ri.betaLength - 1))
  }

  override def process(r: Row) {
    val values = r.colAsString(valCol).toString
    r.removeColumn(valCol)
    val baseColumnsAsString = r.toString

    val genoType = ri.genos
    val filter = ri.genoFilter

    setGenotypesAndFilter(values, genoType, filter)

    ri.phenoTable.zipWithIndex.par.map({ case (phenos, idx) =>
      val (x, y, numberOfSamples) = setupRegressionData(genoType, filter, phenos, idx)

      val linRegObj = linRegObjects(idx)
      linRegObj.setData(x, numberOfSamples)
      linRegObj.runRegression(y)

      val baseAndPhenoColumns = baseColumnsAsString + "\t" + ri.phenoNames(idx)
      writeOut(baseAndPhenoColumns, linRegObj)
    }).seq.foreach(outRows => outRows.foreach(outRow => nextProcessor.process(outRow)))
  }

  private def writeOut(baseAndPhenoColumns: String, linRegObj: LinearRegression): Traversable[Row] = {
    ri.betaNames.zipWithIndex.map({
      case (name, idx) =>
        val lineBuilder = new StringBuilder(baseAndPhenoColumns)
        lineBuilder.append('\t')
        lineBuilder.append(name)
        lineBuilder.append('\t')
        lineBuilder.append("%.5g".format(linRegObj.beta(idx)))
        lineBuilder.append('\t')
        lineBuilder.append("%.5g".format(linRegObj.betaError(idx)))
        lineBuilder.append('\t')
        lineBuilder.append("%.5g".format(linRegObj.tStats(idx)))
        lineBuilder.append('\t')
        lineBuilder.append("%.5g".format(linRegObj.pValues(idx)))
        RowObj.apply(lineBuilder)
    })
  }

  override def parsePhenoValue(s: String): (Double, Boolean) = {
    val sNumOpt = try {
      Some(s.toDouble)
    } catch {
      case _: NumberFormatException => None
    }
    sNumOpt match {
      case Some(x) => (x, !(x.isNaN || x.isInfinite))
      case None => (Double.NaN, false)
    }
  }
}
