/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj

import scala.collection.mutable.StringBuilder

case class BucketSplitAnalysis(splitColumn: Int, bucketSize: Int, separator: String, valueSize:Int, prefix: String, doValidation: Boolean) extends Analysis {

  var valuesCount : Long = -1   // Constant to keep track of number of values in the splitColumn (for validation).

  override def process(r: Row): Unit = {
    val values = r.colAsString(splitColumn)

    val preColumns = r.colsSlice(0, splitColumn)
    val postColumns = r.colsSlice(splitColumn + 1, r.numCols())

    if (valueSize > 0) {
      // Using value size

      if (doValidation) {
        validate(values.length, valueSize)
      }

      if (valueSize > 0 && (values.length % valueSize) != 0) {
        throw new GorDataException("Column size (" + values.length + ") must be multiple of value size (" + valueSize + ")")
      }

      if (valueSize > values.length) {
        throw new GorDataException("Value size (" + valueSize + ") is larger than column size (" + values.length + ")")
      }

      var bucketIndex = 1
      val bucketCharSize = valueSize * bucketSize
      val bucketBuffer = new StringBuilder(bucketCharSize)
      while ((bucketIndex - 1) * bucketCharSize < values.length) {
        bucketBuffer.append(values.subSequence((bucketIndex - 1) * bucketCharSize, Math.min(bucketIndex * bucketCharSize, values.length())))
        super.process(constructRow(preColumns, bucketBuffer, postColumns, prefix, bucketIndex))
        bucketBuffer.clear()
        bucketIndex += 1
      }
    } else if (separator != null && separator.length == 1) {
      // Using splitter

      if (doValidation) {
        var currentValueCount = if (values.length > 0) values.chars().filter(c => c == separator.charAt(0)).count + 1 else 0
        validate(currentValueCount, 1);
      }

      var bucketIndex = 1
      val bucketBuffer = new StringBuilder()
      var valueIndex = 0
      while (valueIndex >= 0 && valueIndex <= values.length) {
        val nextValueIndex = ordinalIndexOf(values, separator.charAt(0), valueIndex, bucketSize)
        bucketBuffer.append(values.subSequence(valueIndex, if (nextValueIndex > 0) nextValueIndex else values.length()))
        super.process(constructRow(preColumns, bucketBuffer, postColumns, prefix, bucketIndex))
        valueIndex = if (nextValueIndex > 0) nextValueIndex + 1 else values.length() + 1
        bucketBuffer.clear()
        bucketIndex += 1
      }
    }
  }

  /**
    * Finds the n-th index within a CharSequence
    *
    * @param charSequence   the CharSequence to search in.
    * @param separator      separator character.
    * @param startIndex     index to start the search.
    * @param ordinal        the ordinal (n-th index) to find.
    *
    * @return the <ordinal> index of <separator> in <charSequence>, starting from <startIndex>.
    *         If found instances of <seperator> are less than <ordinal> then -1 is returned.
    */
  def ordinalIndexOf(charSequence: CharSequence, separator: Char, startIndex:Int, ordinal: Int): Int = {
    var index = startIndex
    var count = 0;
    while (index < charSequence.length() && count < ordinal) {
      if (charSequence.charAt(index).equals(separator)) count += 1
      index += 1
    }

    if (count == ordinal) {
      // Success
      index - 1   // -1 as we always increment in the while loop, also for the last found.
    } else {
      // Failure, did not found enough indices.
      -1
    }
  }

  /**
    * Helper function to construct row objects.
    */
  def constructRow(preColumns:CharSequence, bucket:CharSequence, postColumns:CharSequence, prefix:CharSequence, bucketIndex:Int): Row =  {
    val strbuff = new StringBuilder()
    strbuff.append(preColumns)
    strbuff.append('\t')
    strbuff.append(bucket)
    if (postColumns != null && postColumns.length > 0) {
      strbuff.append('\t')
      strbuff.append(postColumns)
    }
    strbuff.append('\t')
    strbuff.append(prefix)
    strbuff.append(bucketIndex)

    RowObj(strbuff.toString)
  }

  /**
    * Helper function to do common validation.
    */
  def validate(currentValuesCount: Long, valueSize: Int) {
    if (currentValuesCount == 0) {
      throw new GorDataException("The value column can not be emtpy")
    }

    if (valuesCount < 0) {
      valuesCount = currentValuesCount
    }

    if (valuesCount != currentValuesCount) {
      throw new GorDataException("The current line has different number of values in the split column (" + currentValuesCount / valueSize + " vs " + valuesCount / valueSize + ")")
    }
  }

}
