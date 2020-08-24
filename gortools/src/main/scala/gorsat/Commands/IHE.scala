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

package gorsat.Commands

import gorsat.Analysis.IheAnalysis
import gorsat.Analysis.IheAnalysis.ColumnOptions
import gorsat.Commands.CommandParseUtilities._
import gorsat.IteratorUtilities.validHeader
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.GorContext

class IHE extends CommandInfo("IHE",
  CommandArguments("", "", 0, 0),
  CommandOptions(gorCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    val columnOptions = new ColumnOptions

    var useCol = columnsFromHeader("MajorAllele,SecondAllele,Depth,Adepth,Cdepth,Gdepth,Tdepth," +
      "MajorAllelex,SecondAllelex,Depthx,Adepthx,Cdepthx,Gdepthx,Tdepthx," +
      "MajorAllelexx,SecondAllelexx,Depthxx,Adepthxx,Cdepthxx,Gdepthxx,Tdepthxx", forcedInputHeader)

    try {
      columnOptions.majAlleleCol = useCol.head
      columnOptions.secAlleleCol = useCol(1)
      columnOptions.depthCol = useCol(2)
      columnOptions.aCol = useCol(3)
      columnOptions.cCol = useCol(4)
      columnOptions.gCol = useCol(5)
      columnOptions.tCol = useCol(6)

      columnOptions.majAlleleFCol = useCol(7)
      columnOptions.secAlleleFCol = useCol(8)
      columnOptions.depthFCol = useCol(9)
      columnOptions.aFCol = useCol(10)
      columnOptions.cFCol = useCol(11)
      columnOptions.gFCol = useCol(12)
      columnOptions.tFCol = useCol(13)

      columnOptions.majAlleleMCol = useCol(14)
      columnOptions.secAlleleMCol = useCol(15)
      columnOptions.depthMCol = useCol(16)
      columnOptions.aMCol = useCol(17)
      columnOptions.cMCol = useCol(18)
      columnOptions.gMCol = useCol(19)
      columnOptions.tMCol = useCol(20)

    } catch {
      case e: Exception => throw new GorParsingException("Error in trio-pileup columns - columns not found: ", e.getMessage)
    }

    useCol = columnsFromHeader("GT,pGT,LOD,GT2," +
      "GTx,pGTx,LODx,GT2x," +
      "GTxx,pGTxx,LODxx,GT2xx", forcedInputHeader)

    try {
      columnOptions.gtCol = useCol.head
      columnOptions.pgtCol = useCol(1)
      columnOptions.lodCol = useCol(2)
      columnOptions.gt2Col = useCol(3)

      columnOptions.gtFCol = useCol(4)
      columnOptions.pgtFCol = useCol(5)
      columnOptions.lodFCol = useCol(6)
      columnOptions.gt2FCol = useCol(7)

      columnOptions.gtMCol = useCol(8)
      columnOptions.pgtMCol = useCol(9)
      columnOptions.lodMCol = useCol(10)
      columnOptions.gt2MCol = useCol(11)


    } catch {
      case e: Exception => throw new GorParsingException("Error in trio-pileup columns - columns not found: ",  e.getMessage)
    }

    val extraHeader = "dIHE\tpNIHE"
    val combinedHeader = validHeader(forcedInputHeader + "\t" + extraHeader)

    val pipeStep: Analysis = IheAnalysis.IHEs(columnOptions)

    CommandParsingResult(pipeStep, combinedHeader)
  }
}
