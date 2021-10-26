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

package gorsat.process

import java.io.{File, PrintWriter}
import gorsat.DynIterator
import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestSourceProvider extends AnyFlatSpec {

  // todo this shouldn't be here, but is needed for nested query tests
  DynIterator.createGorIterator = PipeInstance.createGorIterator

  var filename: String = _

  override def withFixture(test: NoArgTest) = {
    val file = File.createTempFile("SourceProviderTest-",".tsv")
    new PrintWriter(file) {
      try {
        write("Column\n1\n2\n3\n")
      }
      finally {
        close()
      }
    }
    filename = file.getCanonicalPath
    try super.withFixture(test)
    finally {
      file.deleteOnExit()
      filename = ""
    }
  }

  "provisionSources" should "populate header for simple file in nor context" in {
    val context = new GenericSessionFactory().create().getGorContext
    val sp = SourceProvider(filename, context, executeNor = true, isNor = true)
    assert(sp.header == "Column")
  }

  it should "populate header for simple file in non-nor context" in {
    val context = new GenericSessionFactory().create().getGorContext
    val sp = SourceProvider("../tests/data/gor/genes.gor", context, executeNor = false, isNor = false)
    assert(sp.header == "Chrom\tgene_start\tgene_end\tGene_Symbol")
    sp.source.close()
  }

  it should "populate header with a nested query" in {
    val context = new GenericSessionFactory().create().getGorContext
    val sp = SourceProvider(s"<(nor -h $filename | top 10)", context, executeNor = false, isNor = false)
    assert(sp.header == "ChromNOR\tPosNOR\tColumn")
  }

}
