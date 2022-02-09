package gorsat

import gorsat.Analysis.Select2
import gorsat.Commands.Analysis
import gorsat.Outputs.ToList
import org.gorpipe.gor.model.Row

import scala.collection.mutable.ListBuffer

object ScalaTestUtils {
  def selectToList(): Tuple2[Analysis, ListBuffer[Row]] = {
    val select = Select2(1,2,3,4)
    val buff = new ListBuffer[Row]
    val toList = ToList(buff)
    (select | toList, buff)
  }
}
