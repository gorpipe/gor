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

// gorsatDynIterator.scala
// (c) deCODE genetics
// 18th June, 2012, Hakon Gudbjartsson

package gorsat

import gorsat.Commands.{CommandArguments, CommandParseUtilities}
import org.gorpipe.exceptions.{GorDataException, GorSystemException}
import org.gorpipe.gor.{GorContext, GorSession}
import org.gorpipe.model.genome.files.gor.{Line, Row}
import org.gorpipe.model.gor.iterators.{LineIterator, RowSource, TimedRowSource}
import org.gorpipe.model.gor.{Pipes, RowObj}
import org.gorpipe.util.Pair
import org.slf4j.LoggerFactory

object DynIterator {
import gorsatGorIterator._

val numNonArgs = 1
var createGorIterator : GorContext => gorsatGorIterator = _

def addStartSelector(cmd : String, seekChr : String, seekPos : Int, endPos : Int, seekOnly : Boolean, bufsize : Int, isHeader : Boolean, session : GorSession) : String = {
    val allowedCmds = session.getSystemContext.getCommandWhitelist.asInstanceOf[java.util.Map[String,Any]]
    var end = cmd.indexOf(' ')
    if( end == -1 ) end = cmd.length
    val cmdname = cmd.substring(0,end).toLowerCase

    if (cmd.toLowerCase.startsWith("gorrow ")) return cmd
    else if (cmd.toLowerCase.startsWith("gorrows ")) return cmd
    else if (cmd.toLowerCase.startsWith("nor ")) return cmd
    else if (cmd.toLowerCase.startsWith("norrows ")) return cmd
    else if (cmd.toLowerCase.startsWith("sdl")) return cmd
    else if (cmd.toLowerCase.startsWith("norsql")) return cmd
    var cmd2 = cmd

    val lcmd = cmd2.toLowerCase
    var offset = if( lcmd.startsWith("gor ") || lcmd.startsWith("cmd ") || lcmd.startsWith("sql ") ) 4 else if( allowedCmds != null && allowedCmds.keySet().stream().map(s => s.toLowerCase).anyMatch(s => s.equals(cmdname.toLowerCase)) ) cmdname.length+1 else -1
    if( isHeader ) {
      if( offset == -1 ) return "gor "+cmd
      else return cmd
    } else if( bufsize != -1 ) {
      val bufstr = "-b "+bufsize
      if( offset > 0 ) cmd2 = cmd2.slice(0,offset) + bufstr + cmd2.slice(offset-1,cmd2.length)
      else cmd2 = "gor "+bufstr+cmd2
      offset += bufstr.length+1
    }

    if (!seekOnly /* && !(cmd2.toLowerCase.contains("-pchr") || cmd2.toLowerCase.contains("-p chr")) */ ) {
      if( offset != -1 ) cmd2 = cmd2.slice(0,offset)+"-p #chrGOR#:#startGOR#-"+(if (endPos == -1) " " else "#endGOR# ")+cmd2.slice(offset,cmd2.length)
      else cmd2 = "gor -p #chrGOR#:#startGOR#-"+(if (endPos == -1) " " else "#endGOR# ")+cmd2
    }
    if (seekOnly) {
      if ( offset != -1 ) cmd2 = cmd2.slice(0,offset)+"-seek #chrGOR#:#startGOR# "+cmd2.slice(offset,cmd2.length)
      else cmd2 = "gor -seek #chrGOR#:#startGOR# "+cmd2
    }
    val ret = cmd2.replace("""#chrGOR#""",seekChr).replace("""#startGOR#""",seekPos.toString).replace("#endGOR#",""+endPos.toString)
    ret
}

class DynamicRowSource(iteratorCommand : String, context: GorContext, fixHeader : Boolean = true) extends TimedRowSource {
  private val logger = LoggerFactory.getLogger(this.getClass)

  protected var theSource : RowSource = _
  protected var itDyn : gorsatGorIterator = _
  protected var seekedChr = ""
  protected var seekedPos = 0
  protected var posSet : Boolean = false

  var avgSeekTimeMillis : Double = 0.0
  var usedFiles : List[String] = Nil
  val drsGorPipeSession: GorSession = context.getSession
  val maxCommandSize = 100000
  var nor = false
  var seekCount = 0

  override def getAvgSeekTimeMilliSecond: Double = avgSeekTimeMillis

  override def getAvgBasesPerMilliSecond: Double = theSource.getAvgBasesPerMilliSecond
  override def getAvgRowsPerMilliSecond: Double = theSource.getAvgRowsPerMilliSecond
  override def getAvgBatchSize: Double = theSource.getAvgBatchSize

  override def getCurrentBatchSize: Int = theSource.getCurrentBatchSize
  override def getCurrentBatchLoc: Int = theSource.getCurrentBatchLoc
  override def getCurrentBatchRow(i : Int): Row = theSource.getCurrentBatchRow(i)

  def isNor: Boolean = {
    nor
  }

  def hasNext2 : Boolean = {
    if (!mustReCheck) return myHasNext
    mustReCheck = false
    while (theSource.hasNext) {
      val r = theSource.next()
       if (r.chr >= seekedChr && r.pos >= seekedPos) {
        myNext = r
        myHasNext = true
        return true
      }
    }
    myHasNext = false
    false
  }

  override def hasNext : Boolean = {
    incStat("hasNext")
    if (!mustReCheck) return myHasNext
    if (itDyn == null) {
      itDyn = createGorIterator(context)
      itDyn.fixHeader = fixHeader
      itDyn.scalaInit(iteratorCommand)
      usedFiles = itDyn.usedFiles
      theSource = itDyn.theIterator
    }
    mustReCheck = false
    myHasNext = theSource.hasNext
    if (myHasNext) myNext = theSource.next() else myNext = null
    myHasNext
  }

  override def next() : Row = {
    incStat("next")
    if (hasNext) {
      mustReCheck = true
      myNext
    } else {
      throw new GorSystemException("hasNext: getRow call on false hasNext!", null)
    }
  }

  override def isBuffered: Boolean = true

  def modifiedCommand(cmd : String, seekChr: String, seekPos : Int, endPos : Int, seekOnly : Boolean, getHeader : Boolean = false) : String = {
    var header = getHeader
    val creates = CommandParseUtilities.quoteSafeSplit(cmd,';').map( _.trim )
    val prefix = creates.slice(0,creates.length-1).mkString(";")
    val gorcmd = creates(creates.length-1)
    val pipeSteps = CommandParseUtilities.quoteSafeSplit(gorcmd,'|').map( _.trim )
    var i = 0
    val buffsize = if( this.bufferSize == Pipes.rowsToProcessBuffer ) -1 else bufferSize
    while (i < pipeSteps.length) {
      if (i == 0) {
        var inest = pipeSteps(0).indexOf("<(")
        if( inest == -1 ) inest = pipeSteps(0).length
        var iyml = pipeSteps(0).toUpperCase.indexOf(".YML")
        if( iyml == -1 ) iyml = pipeSteps(0).length
        if( (iyml < inest) && drsGorPipeSession.getSystemContext.getReportBuilder != null ) {
          pipeSteps(0) = modifiedCommand( drsGorPipeSession.getSystemContext.getReportBuilder.parse(pipeSteps(0)), seekChr, seekPos, endPos, seekOnly, header )
          header = false
        } else pipeSteps(0) = addStartSelector(pipeSteps(0),seekChr,seekPos,endPos,seekOnly,buffsize,header,context.getSession)
      } else if (pipeSteps(i).toUpperCase.startsWith("MERGE")) {
        val args = CommandParseUtilities.quoteSafeSplitAndTrim(pipeSteps(i),' ')
        val (inputArguments, _) = CommandParseUtilities.validateCommandArguments(args, CommandArguments("-u -s -i","-e",numNonArgs))
        val rightFile = inputArguments(1).trim
        if (rightFile.slice(0,2)=="<(") {
          val mergeCommand = CommandParseUtilities.parseNestedCommandUntrimmed(rightFile)
          pipeSteps(i) = pipeSteps(i).replace("<("+mergeCommand+")","<("+modifiedCommand(mergeCommand,seekChr,seekPos,endPos,seekOnly)+")")
        } else {
          val mergeCommand = rightFile
          pipeSteps(i) = pipeSteps(i).replace(mergeCommand,"<("+addStartSelector(mergeCommand,seekChr,seekPos,endPos,seekOnly,buffsize,header,context.getSession)+")")
        }
      }
      i += 1
    }
    (if( prefix.length > 0 ) prefix+";" else "") + (if( header ) pipeSteps.map( s => { if(s.toLowerCase.contains("sdl")) s else s.replace("|","| top 0 |") }).mkString("| top 0 |")+"| top 0" else pipeSteps.mkString("|"))
  }
  def setPositionWithoutChrLimits(seekChr: String, seekPos : Int) {
    if (itDyn != null) { itDyn.close() }
    itDyn = createGorIterator(context)
    itDyn.fixHeader = fixHeader
    itDyn.scalaInit(modifiedCommand(iteratorCommand,seekChr,seekPos,-1,seekOnly = true))
    usedFiles = itDyn.usedFiles
    theSource = itDyn.theIterator
    seekedChr = seekChr
    seekedPos = seekPos
    mustReCheck = true
  }

  def setRange(seekChr: String, seekPos : Int, endPos : Int) {
    incStat("setRange")
    if (itDyn != null) { itDyn.close() }
    itDyn = createGorIterator(context)
    itDyn.fixHeader = fixHeader
    itDyn.scalaInit(modifiedCommand(iteratorCommand,seekChr,seekPos,endPos,seekOnly = false))
    usedFiles = itDyn.usedFiles

    theSource = itDyn.theIterator
    seekedChr = seekChr
    seekedPos = seekPos
    mustReCheck = true
  }

  override def setPosition(seekChr: String, seekPos : Int) {
    incStat("setPosition")
    val t = System.nanoTime()
    setRange(seekChr,seekPos,-1)
    avgSeekTimeMillis = (seekCount*avgSeekTimeMillis+System.nanoTime()-t)/(seekCount+1)/1000000
    seekCount += 1
  }

  override def moveToPosition(seekChr : String, seekPos : Int, maxReads: Int = 10000) {
    incStat("moveToPosition")
    seekedChr = seekChr
    seekedPos = seekPos
    super.moveToPosition(seekChr,seekPos,maxReads)
  }

  def close() {
    if (itDyn != null) itDyn.close() // calls theSource.close
  }

  override def getHeader : String = {
    var header = super.getHeader
    if (header != "") return header
    val i = iteratorCommand.indexOf("<(")
    val itCmd = (if( i > -1 ) iteratorCommand.substring(i) else iteratorCommand).replace("| top 0 ", "")

    drsGorPipeSession.getCache.getHeaderFileMap.putIfAbsent(itCmd, new Pair[String, Array[String]]())

    val headerFiles = drsGorPipeSession.getCache.getHeaderFileMap.get(itCmd)

    headerFiles.synchronized {
      if( headerFiles.getFormer != null ) {
        header = headerFiles.getFormer
        setHeader(header)
        usedFiles = headerFiles.getLatter.toList
        logger.debug(s"Header cache hit: $itCmd -> $header")
      } else {
        logger.debug(s"Header cache miss: $itCmd")

        var headerItDyn: gorsatGorIterator = null
        headerItDyn = createGorIterator(context.createNestedContext(null, null, "getHeader"))
        headerItDyn.fixHeader = fixHeader
        var iteratorCommand2 = iteratorCommand
        if (iteratorCommand2.contains(" -seek ")) {
          val largs = CommandParseUtilities.quoteSafeSplit(iteratorCommand2, ' ')
          val chrpos = CommandParseUtilities.stringValueOfOption(largs, "-seek")
          iteratorCommand2 = iteratorCommand2.replace("-seek " + chrpos, "")
        }
        val mcmd = modifiedCommand(iteratorCommand2, "chr1", 0, -1, seekOnly = false, getHeader = true)
        headerItDyn.scalaInit(mcmd)
        nor = headerItDyn.isNorContext
        header = headerItDyn.getHeader
        headerFiles.setFormer(header)
        headerFiles.setLatter(headerItDyn.usedFiles.toArray)

        headerItDyn.close()
        setHeader(header)
        usedFiles = headerFiles.getLatter.toList
      }
    }
    header
  }

  def getLineHeader : String = {
    getHeader
  }
}

  class DynamicNorSource(iteratorCommand : String, context: GorContext) extends DynamicRowSource(iteratorCommand, context) with LineIterator {
    override def nextLine : String = { super.next().otherCols }
    override def getLineHeader : String = { super.getHeader.split("\t").slice(2,1000).mkString("\t") }
  }
  class DynamicNorGorSource(iteratorCommand : String, context: GorContext) extends DynamicRowSource(iteratorCommand, context) {
    override def next() : Row = { val x = super.next(); RowObj("chr1",0,x.toString) }
    override def getHeader : String = { "ChromNOR\tPosNOR\t"+super.getHeader }
  }

  class DynamicGorNorSource(iteratorCommand : String, context: GorContext) extends DynamicRowSource(iteratorCommand, context) {
    override def next() : Row = {
      val nr = super.next()
      try {
        val chrom = nr.colAsString(2).toString
        val pos = nr.colAsInt(3)
        val rest = nr.colsSlice(4, nr.numCols()).toString
        val r = RowObj.apply(chrom, pos, rest)
        r
      } catch {
        case e: ArrayIndexOutOfBoundsException =>
          val exception = new GorDataException("Invalid GOR row")
          exception.setRow(nr.otherCols())
          throw exception
      }
    }
    override def getHeader : String = {
      val h = super.getHeader
      h.substring(h.indexOf('\t',h.indexOf('\t')+1)+1) }
  }

}