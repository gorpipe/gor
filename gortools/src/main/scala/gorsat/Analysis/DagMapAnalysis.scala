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

package gorsat.Analysis

import gorsat.Commands.Analysis
import gorsat.gorsatGorIterator.{FileLineIterator, MapAndListUtilities}
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorSession
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.LineIterator
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

object DagMapAnalysis {

  private val logger = LoggerFactory.getLogger(this.getClass)


  case class DAGnode(lookupName: String, displayName: String) {
    var childrens: List[DAGnode] = Nil
  }

  case class DAG(caseInsensitive: Boolean) {
    var showDAGPath = false
    var nodes = Map.empty[String, DAGnode]
    var pathSeparator: String = "->"
    var dagLevel = 20


    def createOrFind(name: String): DAGnode = {
      val lookupName = if (caseInsensitive) name.toUpperCase else name
      nodes.get(lookupName) match {
        case Some(x) => x
        case None =>
          val x = DAGnode(lookupName, name)
          nodes += (lookupName -> x)
          x
      }
    }

    private def x_leafs(node: DAGnode, depth: Int, path: String): List[(String, Int, String)] = {
      if (depth > 20) {
        logger.warn("Depth > 20 detected!!! The graph used with INDAG is most likely not acyclic!")
        return Nil
      }

      if (depth > dagLevel) {
        return Nil
      }

      var l = List((node.displayName, depth, if (showDAGPath) path else ""))
      if (showDAGPath)
        node.childrens.foreach(c => l :::= x_leafs(c, depth + 1, path + pathSeparator + c.displayName))
      else
        node.childrens.foreach(c => l :::= x_leafs(c, depth + 1, ""))
      l
    }

    def leafs(node: String): Set[String] = x_leafs(createOrFind(node), 0, node).map(x => if (caseInsensitive) x._1.toUpperCase else x._1).filter(_ != "").toSet

    def leafsWithDepth(node: String): Set[(String, Int, String)] = x_leafs(createOrFind(node), 0, node).filter(_._1 != "").toSet
  }

  def readGraph(filename: String, session: GorSession, iterator: LineIterator, caseInsensitive: Boolean): DAG = {
    val m = MapAndListUtilities.getMultiHashMap(filename, iterator, caseInsensitive, session)
    val dag = DAG(caseInsensitive)
    m.asScala.foreach(x => {
      val parentNode = dag.createOrFind(x._1)
      val childNodes = x._2.map(y => dag.createOrFind(y))
      parentNode.childrens :::= childNodes.toList
    })
    dag
  }

  def readGraph(file: String, session: GorSession, caseInsensitive: Boolean = false): DAG = {
    readGraph(file, session, FileLineIterator(file, session.getProjectContext.getFileReader), caseInsensitive)
  }


  var dagGraphs = Map.empty[String, DAG]
  var dagSets = Map.empty[String, Set[String]]

  def dagLeafs(file: String, node: String, session: GorSession, caseInsensitive: Boolean = false): Set[String] = {
    dagGraphs.get(file) match {
      case Some(g) => g.leafs(node)
      case None =>
        val g = readGraph(file, session, caseInsensitive)
        val l = g.leafs(node)
        dagGraphs += (file -> g)
        l
    }
  }

  def dagLeafsWithDepth(file: String, node: String, session: GorSession, caseInsensitive: Boolean = false): Set[(String, Int, String)] = {
    dagGraphs.get(file) match {
      case Some(g) => g.leafsWithDepth(node)
      case None =>
        val g = readGraph(file, session, caseInsensitive)
        val l = g.leafsWithDepth(node)
        dagGraphs += (file -> g)
        l
    }
  }

  def dagSet(file: String, node: String, session: GorSession): Set[String] = {
    val lookupCode = file + "#gordagsep#" + node.toUpperCase
    dagSets.get(lookupCode) match {
      case Some(theSet) => theSet
      case None =>
        val x = dagLeafs(file, node.toUpperCase, session, caseInsensitive = true)
        dagSets += (lookupCode -> x)
        x
    }
  }

  case class DAGMultiMapLookup(session: GorSession, iteratorCommand: String, iterator: LineIterator, fileName: String, columns: List[Int], caseInsensitive: Boolean,
                               missingVal: String, returnMiss: Boolean, showDAGPAth: Boolean, pathSeparator: String, dagLevel: Int) extends Analysis {
    val returnMissing: Boolean = if (returnMiss) true else false
    val singleCol: Boolean = if (columns.length == 1) true else false
    var theDAG: DAG = _


    override def setup() {
      if (iteratorCommand != "") theDAG = readGraph(iteratorCommand, session, iterator, caseInsensitive)
      else theDAG = readGraph(fileName, session, caseInsensitive)
      theDAG.pathSeparator = pathSeparator
      theDAG.dagLevel = dagLevel
      theDAG.showDAGPath = showDAGPAth
    }

    override def process(r: Row) {
      val key = if (caseInsensitive) r.colAsString(columns.head).toString.toUpperCase else r.colAsString(columns.head).toString
      theDAG.nodes.get(key) match {
        case Some(_) =>
          val dagLeafs = theDAG.leafsWithDepth(key)
          val sorted = dagLeafs.toList.sortWith((x, y) => x._2 < y._2)
          sorted.foreach(x => super.process(RowObj(r.toString + "\t" + x._1 + "\t" + x._2 + (if (theDAG.showDAGPath) "\t" + x._3 else ""))))
        case None =>
          if (returnMiss) super.process(RowObj(r.toString + "\t" + missingVal))
      }
    }
  }
}
