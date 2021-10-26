/**
  * Created by hjaltii on 31/07/17.
  */

import gorsat.TestUtils
import org.gorpipe.test.SlowTests
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
@Category(Array(classOf[SlowTests]))
class UTestGorIterator extends AnyFunSuite {

  test("Test equality of old and new iterator") {
    val query1 = "gor ../tests/data/gor/dbsnp_test.gorz | join -snpseg ../tests/data/gor/genes.gorz"
    val query2 = "gor ../tests/data/gor/genes.gorz | join -segsnp ../tests/data/gor/dbsnp_test.gorz"
    val query3 = "gor ../tests/data/gor/genes.gorz | join -segseg ../tests/data/gor/genes.gorz"
    val query4 = "gor ../tests/data/gor/dbsnp_test.gorz | join -snpsnp ../tests/data/gor/dbsnp_test.gorz"
    val query5 = "gor ../tests/data/gor/dbsnp_test.gorz | join -snpsnp <( gor ../tests/data/gor/dbsnp_test.gorz | rownum | where mod(rownum,3) = 0)"
    val query6 = "gor ../tests/data/gor/dbsnp_test.gorz | rownum | where mod(rownum,3) = 0 | join -snpsnp ../tests/data/gor/dbsnp_test.gorz"
    val query7 = "gor ../tests/data/gor/genes.gorz | rownum | where mod(rownum,25) = 0 | join -segseg ../tests/data/gor/genes.gorz"
    val query8 = "gor ../tests/data/gor/genes.gorz | join -segseg <( gor ../tests/data/gor/genes.gorz | rownum | where mod(rownum,25) = 0)"

    val result1 = TestUtils.runGorPipe(query1)
    val result2 = TestUtils.runGorPipe(query2)
    val result3 = TestUtils.runGorPipe(query3)
    val result4 = TestUtils.runGorPipe(query4)
    val result5 = TestUtils.runGorPipe(query5)
    val result6 = TestUtils.runGorPipe(query6)
    val result7 = TestUtils.runGorPipe(query7)
    val result8 = TestUtils.runGorPipe(query8)

    System.setProperty("gor.iterators.useAdaptiveMTP", "false")

    TestUtils.assertGorpipeResults(result1,query1)
    TestUtils.assertGorpipeResults(result2,query2)
    TestUtils.assertGorpipeResults(result3,query3)
    TestUtils.assertGorpipeResults(result4,query4)
    TestUtils.assertGorpipeResults(result5,query5)
    TestUtils.assertGorpipeResults(result6,query6)
    TestUtils.assertGorpipeResults(result7,query7)
    TestUtils.assertGorpipeResults(result8,query8)

    System.setProperty("gor.iterators.useAdaptiveMTP", "true")
  }
}
