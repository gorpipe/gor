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
import gorsat.gorsatGorIterator.MapAndListUtilities
import gorsat.gorsatGorIterator.MapAndListUtilities.multiHashMap
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorSession

case class PedigreeLookup(session: GorSession, fileName : String, mapcol : Int, expandPedigree : Boolean) extends Analysis {
  var key : String = _
  var mapVal : String = ""
  var colMap : multiHashMap = _

  override def setup() {
    colMap = getPedigreeHashMap(fileName,expandPedigree)
  }
  override def process(r : Row) {

    key = r.colAsString(mapcol).toString

    Option(colMap.get(key)) match {
      case Some(x) => x.foreach(y => super.process(r.rowWithAddedColumn(y)))
      case None => super.process(r.rowWithAddedColumn(key+"\tP"))
    }
  }

  def getPedigreeHashMap(filename: String, expandPedigree: Boolean): multiHashMap = synchronized {
    val extFilename = "pedigreemap" + filename
    MapAndListUtilities.syncGetMultiHashMap(extFilename, session) match {
      case Some(theMap) => return theMap
      case None =>
        val multiMap = new java.util.HashMap[String, Array[String]]()
        val mapAsList = MapAndListUtilities.readArray(filename, session.getProjectContext.getFileReader)
        mapAsList.foreach(x => {
          val cols = x.split("\t", -1)
          val (p, f, m) = (cols(0), cols(1), cols(2))
          Option(multiMap.getOrDefault(p, null)) match {
            case Some(prevVals) => multiMap.put(p, ((p + "\tP") +: prevVals).reverse)
            case None => multiMap.put(p, Array(p + "\tP"))
          }
          Option(multiMap.getOrDefault(f, null)) match {
            case Some(prevVals) => multiMap.put(f, ((p + "\tF") +: prevVals).reverse)
            case None => multiMap.put(f, Array(p + "\tF"))
          }
          Option(multiMap.getOrDefault(m, null)) match {
            case Some(prevVals) => multiMap.put(m, ((p + "\tM") +: prevVals).reverse)
            case None => multiMap.put(m, Array(p + "\tM"))
          }
          if (expandPedigree) {
            Option(multiMap.getOrDefault(f, null)) match {
              case Some(prevVals) => multiMap.put(f, ((f + "\tP") +: prevVals).reverse.distinct)
              case None => multiMap.put(f, Array(f + "\tP"))
            }
            Option(multiMap.getOrDefault(m, null)) match {
              case Some(prevVals) => multiMap.put(m, ((m + "\tP") +: prevVals).reverse.distinct)
              case None => multiMap.put(m, Array(m + "\tP"))
            }
          }

        })
        session.getCache.getMultiHashMaps.put(extFilename, multiMap)
        return multiMap
    }
  }
}
