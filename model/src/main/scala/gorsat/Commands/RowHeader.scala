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

case class ColumnHeader(name: String, tpe: String = "") {}

class RowHeader(val columnNames: Array[String], val columnTypes: Array[String]) {
  override def toString: String = columnNames.mkString("\t")

  def toStringWithTypes: String = {
    columnNames.zip(columnTypes).mkString(", ")
  }

  def columnNamesWithTypes: Array[String] = {
    columnNames.indices.map(i => columnNames(i) + ", " + columnTypes(i)).toArray
  }

  /**
    * Look for types that have numeric references and resolve those by looking up
    * in the other RowHeader.
    * Note that <i>other</i> may have fewer columns than this, as the primary use case
    * for this is resolve column types for columns that are added to the row, where the
    * the type should match some reference column.
    *
    * @param other RowHeader that has defined types
    * @return RowHeader with types resolved according to other
    */
  def propagateTypes(other: RowHeader): RowHeader = {
    val newColumnTypes = columnTypes.map(x => {
      if(x.charAt(0).isDigit) {
        val ix = x.toInt
        other.columnTypes(ix)
      } else x
    })
    RowHeader(columnNames, newColumnTypes)
  }

  def getTypesOrDefault(default: String): Array[String] = {
    if(columnTypes == null) {
      columnNames.map(_ => default)
    } else {
      columnTypes.map(ct => if(ct == null || ct.isEmpty || !ct.charAt(0).isLetter) default else ct)
    }
  }

  /**
    * Returns true if any column is missing type information.
    */
  def isMissingTypes: Boolean = columnTypes.exists(c => c == null || c.isEmpty || !c.charAt(0).isLetter)
}

object RowHeader {
  def apply(): RowHeader = {
    apply("")
  }

  def apply(header: String): RowHeader = {
    val columnNames = header.split("\t")
    val colTypes = new Array[String](columnNames.length)
    new RowHeader(columnNames, colTypes)
  }

  def apply(header: String, colTypes: Array[String]): RowHeader = {
    val cols = header.split("\t")
    new RowHeader(cols, colTypes)
  }

  def apply(columnNames: Array[String], columnTypes: Array[String]): RowHeader = {
    new RowHeader(columnNames, columnTypes)
  }

  def apply(columnNames: Array[String]): RowHeader = {
    val colTypes = new Array[String](columnNames.length)
    new RowHeader(columnNames, colTypes)
  }

  def apply(columns: Seq[ColumnHeader]): RowHeader = {
    val columnNames = columns.map(col => col.name).toArray
    val columnTypes = columns.map(col => col.tpe).toArray
    new RowHeader(columnNames, columnTypes);
  }
}