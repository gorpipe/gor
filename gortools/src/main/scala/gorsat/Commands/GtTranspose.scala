package gorsat.Commands

import gorsat.Analysis.GtTransposeFactory
import gorsat.Buckets.PnBucketParsing
import gorsat.Commands.CommandParseUtilities._
import gorsat.DynIterator.DynamicNorSource
import gorsat.gorsatGorIterator.MapAndListUtilities
import gorsat.process.SourceProvider
import org.gorpipe.exceptions.{GorDataException, GorParsingException}
import org.gorpipe.gor.session.{GorContext, GorSession}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

class GtTranspose extends CommandInfo("GTTRANSPOSE",
  CommandArguments("-cols", "-sep -vs", 3, 3),
  CommandOptions(gorCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    val inputHeader = forcedInputHeader
    val session = context.getSession

    val sep = if (hasOption(args, "-sep")) {
      val cand = stringValueOfOption(args, "-sep")
      if (cand.length > 1) throw new GorParsingException("The separator must be a single character.")
      Some(cand.head)
    } else None

    val width = if (hasOption(args, "-vs")) {
      Some(intValueOfOption(args, "-vs"))
    } else None

    val cols = hasOption(args, "-cols")

    val btFileName = iargs(0).trim
    val pnFileName = iargs(1).trim
    val markerFileName = iargs(2).trim

    var btSource: SourceProvider = null
    var pnSource: SourceProvider = null
    var markerSource: SourceProvider = null
    try {
      btSource = getSourceProvider(btFileName, context,  executeNor)
      val btHeader = btSource.header
      if (btHeader.count(_ == '\t') != 1) throw new GorParsingException("The bucket tag file must contain exactly two columns.")
      val btArray = getContAsStringArray(btFileName, btSource, session)

      pnSource = getSourceProvider(pnFileName, context, executeNor)
      val pnHeader = pnSource.header
      if (pnHeader.contains('\t')) throw new GorParsingException("The pn file must contain exactly one column.")
      val pnArray = getContAsStringArray(pnFileName, pnSource, session)

      val lus = btFileName + "#" + btSource.iteratorCommand + "#" + pnFileName + "#" + pnSource.iteratorCommand
      val bucketToPnIdxLists = session.getCache.getObjectHashMap
        .computeIfAbsent(lus, _=> getBucketToPnIdxList(btArray, pnArray)).asInstanceOf[Map[String, (Array[Int], Array[Int])]]

      markerSource = getSourceProvider(markerFileName, context, executeNor)
      val (markerHeader, markerToIdxMap) = getHeaderAndMarkerToIdxMap(markerFileName, markerSource, session)
      val inputCols = inputHeader.split('\t').map(_.toUpperCase)
      val indices = markerHeader.split('\t').map(_.toUpperCase).map(col => {
        val idx = inputCols.indexOf(col)
        if (idx == -1) throw new GorParsingException(s"No column named $col in $inputHeader")
        idx
      })

      val inputColsTUC = inputCols.map(_.toUpperCase)
      val bCol = inputColsTUC.indexOf("BUCKET")
      val vCol = inputColsTUC.indexOf("VALUES")

      val outputHeader = if (cols) {
        val source = getSourceProvider(markerFileName, context, executeNor)
        val builder = new StringBuilder("CHROM\tPOS\tPN")
        getMarkersForHeader(markerFileName, source, session)
          .foreach(rsId => {
            builder.append('\t')
            builder.append(rsId)
          })
        builder.toString
      } else "CHROM\tPOS\tPN\tVALUES"
      val pipeStep = GtTransposeFactory(pnArray, bucketToPnIdxLists, markerToIdxMap, bCol, vCol, indices, sep, width, cols).getAnalysis()
      CommandParsingResult(pipeStep, outputHeader)
    } catch {
      case e: Exception => throw e
    } finally {
      if (btSource.dynSource != null) btSource.dynSource.close()
      if (pnSource != null && pnSource.dynSource != null) pnSource.dynSource.close()
      if (markerSource != null && markerSource.dynSource != null) markerSource.dynSource.close()
    }
  }

  def getSourceProvider(arg: String, context: GorContext, executeNor: Boolean): SourceProvider = {
    val name = {
      val cand = arg.trim
      val candTUC = cand.toUpperCase
      if (!(cand.slice(0, 2) == "<(")) {
        "<(nor " + cand + " )"
      } else cand
    }
    SourceProvider(name, context, executeNor = executeNor, isNor = true)
  }

  def readToArray(iter: DynamicNorSource): Array[String] = {
    val builder = new ArrayBuffer[String]()
    while (iter.hasNext) {
      builder += iter.nextLine
    }
    builder.toArray
  }

  def getMapToIdx(iter: DynamicNorSource): Map[String,Int] = {
    val builder = Map.newBuilder[String, Int]
    var idx = 0
    while (iter.hasNext) {
      val line = iter.nextLine
      builder += (line -> idx)
      idx += 1
    }
    builder.result()
  }

  def getBucketToPnIdxList(btArray: Array[String], pns: Array[String]): Map[String, (Array[Int], Array[Int])] = {
    val pbt = PnBucketParsing.parse(btArray).indexByBucket().filter(pns)
    val bucketToIdxLists = Array.tabulate(pbt.numberOfBuckets)(_ => (ArrayBuffer.empty[Int], ArrayBuffer.empty[Int]))
    var pnAbsIdx = 0
    while (pnAbsIdx < pbt.numberOfPns) {
      val bucketIdx = pbt.pnIdxToBuckIdx(pnAbsIdx)
      val bucketPos = pbt.pnIdxToBuckPos(pnAbsIdx)
      val (absIds, relIdx) = bucketToIdxLists(bucketIdx)
      absIds += pnAbsIdx
      relIdx += bucketPos
      pnAbsIdx += 1
    }
    for ((name, idx) <- pbt.buckNameToIdx) yield {
      val (absIds, relIds) = bucketToIdxLists(idx)
      (name, (absIds.toArray, relIds.toArray))
    }
  }

  def getContAsStringArray(fileName: String, source: SourceProvider, session: GorSession): Array[String] = {
    if (source.iteratorCommand == "") {
      MapAndListUtilities.getStringArray(fileName, session)
    } else {
      MapAndListUtilities.getStringArray(fileName, source.dynSource.asInstanceOf[DynamicNorSource], session)
    }
  }

  def getMarkersForHeader(fileName: String, sourceProvider: SourceProvider, session: GorSession): Array[String] = {
    val buffer = ArrayBuffer[String]()
    val iterator = if (sourceProvider.iteratorCommand == "") {
      sourceProvider.source
    } else {
      sourceProvider.dynSource
    }

    while (iterator.hasNext()) {
      val row = iterator.next()
      val str = row.otherCols()
      buffer.append(str.split('\t').mkString("_"))
    }
    buffer.toArray
  }

  def getHeaderAndMarkerToIdxMap(fileName: String, sourceProvider: SourceProvider, session: GorSession): (String, Map[String, Int]) = {
    val iterator = if (sourceProvider.iteratorCommand == "") {
      sourceProvider.source
    } else {
      sourceProvider.dynSource
    }
    var counter = 0
    var map = Map[String, Int]()
    while (iterator.hasNext()) {
      val s = iterator.next().otherCols()
      map = map + (s -> counter)
      counter += 1
    }

    (sourceProvider.header, map)
  }
}
