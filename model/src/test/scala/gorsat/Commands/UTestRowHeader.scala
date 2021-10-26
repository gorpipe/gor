package gorsat.Commands

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestRowHeader extends AnyFlatSpec {
  "RowHeader" should "have empty types after construction from string" in {
    val r = RowHeader("chrom\tpos\tA\tB\tC")
    assert(r.columnTypes(0) == null)
    assert(r.columnTypes(1) == null)
    assert(r.columnTypes(2) == null)
    assert(r.columnTypes(3) == null)
    assert(r.columnTypes(4) == null)
  }

  it should "have types after construction with types" in {
    val r = RowHeader("chrom\tpos\tA\tB\tC", Array("S", "I", "D", "L", "S"))
    assert(r.columnTypes(0) == "S")
    assert(r.columnTypes(1) == "I")
    assert(r.columnTypes(2) == "D")
    assert(r.columnTypes(3) == "L")
    assert(r.columnTypes(4) == "S")
  }

  it should "have types after construction with ColumnHeader values" in {
    val r = RowHeader(List(ColumnHeader("Chrom", "S"), ColumnHeader("Pos", "I"), ColumnHeader("A", "D")));
    assert(r.columnTypes(0) == "S")
    assert(r.columnTypes(1) == "I")
    assert(r.columnTypes(2) == "D")
  }

  "toString" should "return empty string for empty header" in {
    val r = RowHeader("")
    assert(r.toString == "")
  }

  it should "return header string" in {
    val header = "chrom\tpos\tA\tB\tC"
    val r = RowHeader(header, Array("S", "I", "D", "L", "S"))
    assert(r.toString == header)
  }

  "propagateTypes" should "propagate missing types" in {
    val header = "chrom\tpos\tA\tB\tC"
    val r1 = RowHeader(header, Array("S", "I", "D", "L", "S"))
    val r2 = RowHeader(header, Array("0", "1", "S", "3", "4"))

    val r3 = r2.propagateTypes(r1)

    assert(r3.columnTypes(0) == "S")
    assert(r3.columnTypes(1) == "I")
    assert(r3.columnTypes(2) == "S")
    assert(r3.columnTypes(3) == "L")
    assert(r3.columnTypes(4) == "S")
  }

  "isMissingTypes" should "return true when no types are present" in {
    val r = RowHeader("chrom\tpos\tA\tB\tC")
    assert(r.isMissingTypes)
  }

  it should "return false when types are present" in {
    val r = RowHeader("chrom\tpos\tA\tB\tC", Array("S", "I", "D", "L", "S"))
    assert(!r.isMissingTypes)
  }

  it should "return true when types are present except for one" in {
    val r = RowHeader("chrom\tpos\tA\tB\tC", Array("S", "I", "D", "L", ""))
    assert(r.isMissingTypes)
  }

  it should "return true when types are references" in {
    val r = RowHeader("chrom\tpos\tA\tB\tC", Array("0", "1", "2", "3", "4"))
    assert(r.isMissingTypes)
  }

  "getTypesOrDefault" should "return types when all types are known" in {
    val r = RowHeader("chrom\tpos\tA\tB\tC", Array("S", "I", "D", "L", "S"))
    assert(r.getTypesOrDefault("S") sameElements Array("S", "I", "D", "L", "S"))
  }

  it should "return default when types are not known" in {
    val r = RowHeader("chrom\tpos\tA\tB\tC")
    assert(r.getTypesOrDefault("S") sameElements Array("S", "S", "S", "S", "S"))
  }

  it should "return default when types are references" in {
    val r = RowHeader("chrom\tpos\tA\tB\tC", Array("0", "1", "2", "3", "4"))
    assert(r.getTypesOrDefault("S") sameElements Array("S", "S", "S", "S", "S"))
  }
}
