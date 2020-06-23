package gorsat.Analysis

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.Files

import gorsat.Commands.Analysis
import gorsat.{DynIterator, TestUtils}
import gorsat.process.{GenericSessionFactory, GorPipeCommands, GorPipeMacros, PipeInstance}
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestGtTransposeAnalysis extends FunSuite with BeforeAndAfter {
  var dir: File = _
  val context = new GenericSessionFactory().create().getGorContext

  before {
    DynIterator.createGorIterator = PipeInstance.createGorIterator
    GorPipeCommands.register()
    GorPipeMacros.register()
    dir = Files.createTempDirectory("uTestGtTranspose").toFile
  }

  test("factory errors") {
    val factory1 = GtTransposeFactory(null, null, null, -1, -1, null, Some(','), Some(1), cols = false)
    var success = false
    try {
      factory1.getAnalysis()
    } catch {
      case _: GorParsingException => success = true
      case _=>
    }
    assert(success)

    success = false
    val factory2 = GtTransposeFactory(null, null, null, -1, -1, null, None, None, cols = false)
    try {
      factory2.getAnalysis()
    } catch {
      case _: GorParsingException => success = true
      case _=>
    }
    assert(success)
  }

  test("fixed width") {
    val pns = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}", s"PN${10 * i + 7}")).toArray
    val bucketToPnIdxList = Map("BUCKET1" -> (Array(0, 1), Array(2, 6)),
      "BUCKET2" -> (Array(2, 3), Array(2, 6)),
      "BUCKET3" -> (Array(4, 5), Array(2, 6)),
      "BUCKET4" -> (Array(6, 7), Array(2, 6)),
      "BUCKET5" -> (Array(8, 9), Array(2, 6)),
      "BUCKET6" -> (Array(10, 11), Array(2, 6)),
      "BUCKET7" -> (Array(12, 13), Array(2, 6)),
      "BUCKET8" -> (Array(14, 15), Array(2, 6)),
      "BUCKET9" -> (Array(16, 17), Array(2, 6)),
      "BUCKET10" -> (Array(18, 19), Array(2, 6)))
    val markerToIdxMap = Map(("RS1" -> 0), ("RS2" -> 1))

    val gtTrans = FixedWidthGtTransposeAnalysis(pns, bucketToPnIdxList, markerToIdxMap, bCol = 3, vCol = 4, Array(2), cols = false, 2)

    val wantedRows = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}\t2222", s"PN${10 * i + 7}\t6666")).map(line => "chrA\t1\t" + line).toArray
    val results = processRows(gtTrans, 2, 10, 10, (_,_,k) => s"$k$k")

    assert(wantedRows.length == results.length)
    results zip wantedRows foreach {
      case (actualLine, wantedLine) => assert(actualLine.toString == wantedLine)
    }
  }

  test("sep") {
    val pns = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}", s"PN${10 * i + 7}")).toArray
    val bucketToPnIdxList = Map("BUCKET1" -> (Array(0, 1), Array(2, 6)),
      "BUCKET2" -> (Array(2, 3), Array(2, 6)),
      "BUCKET3" -> (Array(4, 5), Array(2, 6)),
      "BUCKET4" -> (Array(6, 7), Array(2, 6)),
      "BUCKET5" -> (Array(8, 9), Array(2, 6)),
      "BUCKET6" -> (Array(10, 11), Array(2, 6)),
      "BUCKET7" -> (Array(12, 13), Array(2, 6)),
      "BUCKET8" -> (Array(14, 15), Array(2, 6)),
      "BUCKET9" -> (Array(16, 17), Array(2, 6)),
      "BUCKET10" -> (Array(18, 19), Array(2, 6)))
    val markerMap = Range(0, 2).map(i => s"RS${i + 1}").zipWithIndex.toMap
    val gtTrans = SepGtTransposeAnalysis(pns, bucketToPnIdxList, markerMap, bCol = 3, vCol = 4, Array(2), cols = false, sep = ',')

    val wantedRows = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}\t2,2", s"PN${10 * i + 7}\t6,6")).map(r => "chrA\t1\t" + r).toArray
    val gtFunc = (k: Int) => k.toString
    val results = processRows(gtTrans, 2, 10, 10, (_,_,k) => gtFunc(k), sep = ",")

    assert(wantedRows.length == results.length)
    results zip wantedRows foreach {
      case (actualLine, wantedLine) => assert(actualLine.toString == wantedLine)
    }
  }

  test("sep - all empty") {
    val pns = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}", s"PN${10 * i + 7}")).toArray
    val bucketToPnIdxList = Map("BUCKET1" -> (Array(0, 1), Array(2, 6)),
      "BUCKET2" -> (Array(2, 3), Array(2, 6)),
      "BUCKET3" -> (Array(4, 5), Array(2, 6)),
      "BUCKET4" -> (Array(6, 7), Array(2, 6)),
      "BUCKET5" -> (Array(8, 9), Array(2, 6)),
      "BUCKET6" -> (Array(10, 11), Array(2, 6)),
      "BUCKET7" -> (Array(12, 13), Array(2, 6)),
      "BUCKET8" -> (Array(14, 15), Array(2, 6)),
      "BUCKET9" -> (Array(16, 17), Array(2, 6)),
      "BUCKET10" -> (Array(18, 19), Array(2, 6)))
    val markerMap = Range(0, 2).map(i => s"RS${i + 1}").zipWithIndex.toMap
    val gtTrans = SepGtTransposeAnalysis(pns, bucketToPnIdxList, markerMap, bCol = 3, vCol = 4, Array(2), cols = false, sep = ',')

    val gtFunc = (_: Int) => ""
    val results = processRows(gtTrans, 2, 10, 10, (_,_,k) => gtFunc(k), sep = ",").toArray

    val wantedRows = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}\t,", s"PN${10 * i + 7}\t,")).map(r => "chrA\t1\t" + r)

    assert(wantedRows.length == results.length)
    results zip wantedRows foreach {
      case (actualLine, wantedLine) => assert(actualLine.toString == wantedLine)
    }
  }

  test("sep - all empty - choose all") {
    val pns = Range(0, 100).map(i => s"PN$i").toArray
    val bucketToPnIdxList = Map("BUCKET1" -> (Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)),
      "BUCKET2" -> (Array(10, 11, 12, 13, 14, 15, 16, 17, 18, 19), Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)),
      "BUCKET3" -> (Array(20, 21, 22, 23, 24, 25, 26, 27, 28, 29), Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)),
      "BUCKET4" -> (Array(30, 31, 32, 33, 34, 35, 36, 37, 38, 39), Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)),
      "BUCKET5" -> (Array(40, 41, 42, 43, 44, 45, 46, 47, 48, 49), Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)),
      "BUCKET6" -> (Array(50, 51, 52, 53, 54, 55, 56, 57, 58, 59), Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)),
      "BUCKET7" -> (Array(60, 61, 62, 63, 64, 65, 66, 67, 68, 69), Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)),
      "BUCKET8" -> (Array(70, 71, 72, 73, 74, 75, 76, 77, 78, 79), Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)),
      "BUCKET9" -> (Array(80, 81, 82, 83, 84, 85, 86, 87, 88, 89), Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)),
      "BUCKET10" -> (Array(90, 91, 92, 93, 94, 95, 96, 97, 98, 99), Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)))
    val markerMap = Range(0, 2).map(i => s"RS${i + 1}").zipWithIndex.toMap
    val gtTrans = SepGtTransposeAnalysis(pns, bucketToPnIdxList, markerMap, bCol = 3, vCol = 4, Array(2), cols = false, sep = ',')

    val wantedRows = Range(0, 100).map(i => s"chrA\t1\tPN$i\t,").toArray

    val gtFunc = (_: Int) => ""
    val results = processRows(gtTrans, 2, 10, 10, (_,_,k) => gtFunc(k), sep = ",")
    assert(wantedRows.length == results.length)
    results zip wantedRows foreach {
      case (actualLine, wantedLine) => assert(actualLine.toString == wantedLine)
    }
  }

  test("fixed width - just from some buckets") {
    val pns = Array("PN20", "PN55")
    val bucketToPnIdxList = Map("BUCKET2" -> (Array(0), Array(9)),
      "BUCKET5" -> (Array(1), Array(4)))
    val markerMap = Range(0, 2).map(i => s"RS${i + 1}").zipWithIndex.toMap
    val gtTrans = FixedWidthGtTransposeAnalysis(pns, bucketToPnIdxList, markerMap, bCol = 3, vCol = 4, Array(2), cols = false, width = 1)

    val wantedRows = List("chrA\t1\tPN20\t99", "chrA\t1\tPN55\t44")

    val gtFunc = (k: Int) => k.toString
    val results = processRows(gtTrans, 2, 10, 10, (_,_,k) => gtFunc(k))
    assert(wantedRows.length == results.length)
    results zip wantedRows foreach {
      case (actualLine, wantedLine) => assert(actualLine.toString == wantedLine)
    }
  }

  test("from gorpipe") {
    val btCont = Range(0, 100).map(i => s"PN${i+1}\tBUCKET${i/10 + 1}")
    val btPath = writeFile("btFile1.tsv", btCont, "#TAG\tBUCKET")

    val gorCont = Range(0, 20).map(i => s"chr1\t${i/10 + 1}\tRS${i/10 + 1}\tBUCKET${i%10 + 1}\t00112233445566778899")
    val gorPath = writeFile("gor1.gor", gorCont, "CHROM\tPOS\tRSID\tBUCKET\tVALUES")

    val pnCont = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}", s"PN${10 * i + 7}"))
    val pnPath = writeFile("pn1.tsv", pnCont)

    val markerCont = Range(0, 2).map(i => s"RS${i + 1}")
    val markerPath = writeFile("markers1.tsv", markerCont, "#RSID")

    val wantedRows = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}\t2222", s"PN${10 * i + 7}\t6666")).map(r => "chrA\t1\t" + r).toArray

    val query = s"gor $gorPath | gttranspose $btPath $pnPath $markerPath -vs 2"
    val allRows = TestUtils.runGorPipe(query).split("\n")
    val header = allRows.head
    val results = allRows.tail
    assert(header == "CHROM\tPOS\tPN\tVALUES")
    assert(wantedRows.length == results.length)
    results zip wantedRows foreach {
      case (actualLine, wantedLine) => assert(actualLine == wantedLine)
    }
  }

  test("from gorpipe - cols") {
    val btCont = Range(0, 100).map(i => s"PN${i+1}\tBUCKET${i/10 + 1}")
    val btPath = writeFile("btFile2.tsv", btCont, "#TAG\tBUCKET")

    val gorCont = Range(0, 20).map(i => s"chr1\t${i/10 + 1}\tRS${i/10 + 1}\tBUCKET${i%10 + 1}\t00112233445566778899")
    val gorPath = writeFile("gor2.gor", gorCont, "CHROM\tPOS\tRSID\tBUCKET\tVALUES")

    val pnCont = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}", s"PN${10 * i + 7}"))
    val pnPath = writeFile("pn2.tsv", pnCont)

    val markerCont = Range(0, 2).map(i => s"RS${i + 1}")
    val markerPath = writeFile("markers2.tsv", markerCont, "#RSID")

    val wantedRows = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}\t22\t22", s"PN${10 * i + 7}\t66\t66")).map(r => "chrA\t1\t" + r).toArray

    val query = s"gor $gorPath | gttranspose $btPath $pnPath $markerPath -vs 2 -cols"
    val allRows = TestUtils.runGorPipe(query).split("\n")
    val header = allRows.head
    val results = allRows.tail
    assert(header == "CHROM\tPOS\tPN\tRS1\tRS2")
    assert(wantedRows.length == results.length)
    results zip wantedRows foreach {
      case (actualLine, wantedLine) => assert(actualLine == wantedLine)
    }
  }

  test("from gorpipe - gor file as rs file") {
    val btCont = Range(0, 100).map(i => s"PN${i+1}\tBUCKET${i/10 + 1}")
    val btPath = writeFile("btFile3.tsv", btCont, "#TAG\tBUCKET")

    val gorCont = Range(0, 20).map(i => s"chr1\t${i/10 + 1}\tA\tC\tBUCKET${i%10 + 1}\t0123456789")
    val gorPath = writeFile("gor3.gor", gorCont, "CHROM\tPOS\tREF\tALT\tBUCKET\tVALUES")

    val pnCont = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}", s"PN${10 * i + 7}"))
    val pnPath = writeFile("pn3.tsv", pnCont)

    val markerCont = Range(0, 2).map(i => s"chr1\t${i+1}\tA\tC")
    val markerPath = writeFile("markers3.gor", markerCont, "CHROM\tPOS\tREF\tALT")

    val wantedRows = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}\t22", s"PN${10 * i + 7}\t66")).map(r => "chrA\t1\t" + r).toArray

    val query = s"gor $gorPath | gttranspose $btPath $pnPath $markerPath -vs 1"
    val allRows = TestUtils.runGorPipe(query).split("\n")
    val header = allRows.head
    val results = allRows.tail
    assert(header == "CHROM\tPOS\tPN\tVALUES")
    assert(wantedRows.length == results.length)
    results zip wantedRows foreach {
      case (actualLine, wantedLine) => assert(actualLine == wantedLine)
    }
  }

  test("from gorpipe - gor file as rs file and cols") {
    val btCont = Range(0, 100).map(i => s"PN${i+1}\tBUCKET${i/10 + 1}")
    val btPath = writeFile("btFile3.tsv", btCont, "#TAG\tBUCKET")

    val gorCont = Range(0, 20).map(i => s"chr1\t${i/10 + 1}\tA\tC\tBUCKET${i%10 + 1}\t00112233445566778899")
    val gorPath = writeFile("gor3.gor", gorCont, "CHROM\tPOS\tREF\tALT\tBUCKET\tVALUES")

    val pnCont = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}", s"PN${10 * i + 7}"))
    val pnPath = writeFile("pn3.tsv", pnCont)

    val markerCont = Range(0, 2).map(i => s"chr1\t${i+1}\tA\tC")
    val markerPath = writeFile("markers3.gor", markerCont, "CHROM\tPOS\tREF\tALT")

    val wantedRows = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}\t22\t22", s"PN${10 * i + 7}\t66\t66")).map(r => "chrA\t1\t" + r).toArray

    val query = s"gor $gorPath | gttranspose $btPath $pnPath $markerPath -vs 2 -cols"
    val allRows = TestUtils.runGorPipe(query).split("\n")
    val header = allRows.head
    val results = allRows.tail
    assert(header == "CHROM\tPOS\tPN\tchr1:1:A:C\tchr1:2:A:C")
    assert(wantedRows.length == results.length)
    results zip wantedRows foreach {
      case (actualLine, wantedLine) => assert(actualLine == wantedLine)
    }
  }

  test("from gorpipe - sep") {
    val btCont = Range(0, 100).map(i => s"PN${i+1}\tBUCKET${i/10 + 1}")
    val btPath = writeFile("btFile4.tsv", btCont, "#TAG\tBUCKET")

    val gorCont = Range(0, 20).map(i => s"chr1\t${i/10 + 1}\tA\tC\tBUCKET${i%10 + 1}\t0,1,2,3,4,5,6,7,8,9")
    val gorPath = writeFile("gor4.gor", gorCont, "CHROM\tPOS\tREF\tALT\tBUCKET\tVALUES")

    val pnCont = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}", s"PN${10 * i + 7}"))
    val pnPath = writeFile("pn4.tsv", pnCont)

    val markerCont = Range(0, 2).map(i => s"chr1\t${i+1}\tA\tC")
    val markerPath = writeFile("markers4.gor", markerCont, "CHROM\tPOS\tREF\tALT")

    val wantedRows = Range(0, 10).flatMap(i => List(s"PN${10 * i + 3}\t2\t2", s"PN${10 * i + 7}\t6\t6")).map(r => "chrA\t1\t" + r).toArray

    val query = s"gor $gorPath | gttranspose $btPath $pnPath $markerPath -cols -sep ,"
    val allRows = TestUtils.runGorPipe(query).split("\n")
    val header = allRows.head
    val results = allRows.tail
    assert(header == "CHROM\tPOS\tPN\tchr1:1:A:C\tchr1:2:A:C")
    assert(wantedRows.length == results.length)
    results zip wantedRows foreach {
      case (actualLine, wantedLine) => assert(actualLine == wantedLine)
    }
  }

  def writeFile(name: String, content: Traversable[String], header: String = ""): String = {
    val file = new File(dir, name)
    val writer = new BufferedWriter(new FileWriter(file))
    if (header != "") {
      writer.write(header)
      writer.write("\n")
    }
    content.foreach(line => writer.write(line + "\n"))
    writer.close()
    file.getAbsolutePath
  }

  def processRows(ps: Analysis, numberOfMarkers: Int, numberOfBuckets: Int, numberInBucket: Int,
                  gtFunc: (Int, Int, Int) => String, sep: String = ""): List[Row] = {
    val as = AnalysisSink()
    val myPs = ps | as
    Range(0, numberOfMarkers).foreach(i => {
      val prefix = s"chr1\t${i+1}\tRS${i+1}"
      Range(0, numberOfBuckets).foreach(j => {
        val values = Range(0, numberInBucket).map(gtFunc(i,j,_)).mkString(sep)
        val row = prefix + s"\tBUCKET${j+1}\t" + values
        myPs.process(RowObj(row))
      })
    })
    myPs.securedFinish(null)
    as.rows
  }
}
