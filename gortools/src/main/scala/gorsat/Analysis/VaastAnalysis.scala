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
import org.gorpipe.gor.GorSession
import org.gorpipe.model.gava.VariantAssociation
import org.gorpipe.model.genome.files.gor.{GorMonitor, Row}
import org.gorpipe.model.gor.RowObj

case class VaastAnalysis(session: GorSession, caseList : List[String], ctrlList : List[String], maxIterations : Int) extends Analysis {

  var recessive = false
  var dominant = false
  var casepene: Int = -1
  var ctrlpene: Int = -1
  var noMaxAlleleCounts = false
  var protective = false
  var collapsingThreshold = 5
  var usePhase = false
  var maxAf = 0.3
  var bailOutAfter = 10
  var debug = false

  class VaastEngine {
    val vaast = new VariantAssociation()
    def setModel(cases : List[String], ctrls : List[String],
                 maxIterations : Int, baleOutAfter : Int, collapsingThreshold : Int,
                 recessive : Boolean, dominant : Boolean, casepene : Int, ctrlpene : Int,
                 usePhase : Boolean, protective : Boolean, maxAf : Double,
                 gm : GorMonitor, debug : Boolean) {
      vaast.setPnLists(cases.toArray, ctrls.toArray)
      vaast.setNumRandomIterations(maxIterations, baleOutAfter)
      vaast.setModel(recessive, dominant, ctrlpene, casepene, noMaxAlleleCounts, maxAf, protective)
      vaast.setCollapseThreshold(collapsingThreshold)
      vaast.setCancelMonitor(gm)
      vaast.setDebug(debug)
    }
    def initializeGroup() {
      vaast.initializeGroup()
    }
    def addVariant(variantString : String) {
      vaast.addVariant(variantString)
    }
    def calculatePvalue : String = {
      vaast.calculateValues()
    }
  }

  var geneCol = 4
  var posCol = 6
  var refCol = 7
  var altCol = 8
  var pnCol = 9
  var callCopiesCol = 10
  var phaseCol: Int = -1
  var scoreCol: Int = -1

  var geneSelArray: Array[Int] = List(0,1,2,geneCol).toArray
  var varIdArray: Array[Int] = List(0,posCol,refCol,altCol).toArray
  var varSelArray: Array[Int] = if (phaseCol == -1) List(pnCol,callCopiesCol,scoreCol).toArray else List(pnCol,callCopiesCol,phaseCol,scoreCol).toArray

  val vaastEngine = new VaastEngine()

  var lastGene = ""
  var seqVarID = ""
  var idCounter: Int = -1

  var idLookupMap = Map.empty[String, String ]

  def initializeColumnArrays(): Unit = {
    geneSelArray = List(0,1,2,geneCol).toArray
    varIdArray = List(0,posCol,refCol,altCol).toArray
    varSelArray = if (phaseCol == -1) List(pnCol,callCopiesCol,scoreCol).toArray else List(pnCol,callCopiesCol,phaseCol,scoreCol).toArray
  }

  override def setup() { vaastEngine.setModel(caseList,ctrlList,maxIterations,bailOutAfter,collapsingThreshold,
    recessive,dominant,casepene,ctrlpene,usePhase,protective,maxAf,session.getSystemContext.getMonitor,debug) }

  override def process(r : Row) {
    val gene = r.selectedColumns(geneSelArray)

    if (gene != lastGene) {
      if (lastGene != "") {
        val s = vaastEngine.calculatePvalue
        super.process(RowObj(lastGene+"\t"+s))
      }
      idLookupMap = Map.empty[String, String ]
      idCounter = -1
      lastGene = gene
      vaastEngine.initializeGroup()
    }

    val idLookupString = r.selectedColumns(varIdArray)
    idLookupMap.get(idLookupString) match {
      case Some(x) =>
        seqVarID = x
      case None =>
        idCounter += 1;
        idLookupMap += (idLookupString -> idCounter.toString);
        seqVarID = idCounter.toString
    }
    vaastEngine.addVariant(seqVarID+"\t"+r.selectedColumns(varSelArray))
  }
  override def finish() {
    if (lastGene != "") {
      val s = vaastEngine.calculatePvalue
      super.process(RowObj(lastGene+"\t"+s))
    }
  }
}