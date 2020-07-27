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

package gorsat

import breeze.linalg.{DenseMatrix, DenseVector}
import Regression.{LogisticRegression, LogisticRegressionModel, WaldTest}
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestLogisticRegressionScala extends FunSuite {
  val maxIter = 20
  val tol = 1e-5

  test("regression correctness") {
    compareGorAndHail(100, 10)
    compareGorAndHail(1000, 10)
    compareGorAndHail(10000, 10)
  }

  def compareGorAndHail(numberOfSamples: Int, numberOfTrials: Int): Unit = {
    for (counter <- 0 until numberOfTrials) {
      val numberOfDepVars = (counter % 5) + 1
      val x = Array.ofDim[Double](numberOfDepVars, numberOfSamples)
      for {
        i <- 0 until numberOfDepVars
        j <- 0 until numberOfSamples
      } x(i)(j) = 100 * Math.random() - 50
      val beta = new Array[Double](numberOfDepVars + 1).map(_=> 10 * Math.random() - 5)
      val y = x.zip(beta.tail).foldLeft(new Array[Double](numberOfSamples).map(_=> beta.head))((sums, pair) => {
        val (x_i, beta_i) = pair
        sums.zip(x_i).map(pair2 => {
          val (sum_i, x_ij) = pair2
          sum_i + beta_i * x_ij
        })
      }).map(xi => Math.random() < 1.0 / (1.0 + Math.exp(-xi)))
      val data = new Array[Double](numberOfSamples * (numberOfDepVars + 1))
      for (i <- 0 until numberOfSamples) data(i) = 1
      x.zipWithIndex.foreach(pair => System.arraycopy(pair._1, 0, data, (pair._2 + 1) * numberOfSamples, numberOfSamples))
      val xMatrix = new DenseMatrix[Double](numberOfSamples, numberOfDepVars + 1, data)
      val yVector = new DenseVector[Double](y.map(if(_) 1.0 else 0.0))

      //Done generating data.

      //GOR
      val gorLogReg = LogisticRegression.getGorLogisticRegressionObject(numberOfDepVars, numberOfSamples)
      val gorSuccess = gorLogReg.runRegression(x, y, numberOfSamples, tol, maxIter)

      //Hail
      val nullModel = new LogisticRegressionModel(DenseMatrix.fill[Double](numberOfSamples, 1)(1.0), yVector)
      val nullFit = nullModel.fit(None,  maxIter, tol)
      val test = WaldTest.test(xMatrix, yVector, nullFit, maxIter, tol)
      val stats = test.stats
      stats match {
        case Some(stats) => {
          Assert.assertTrue(gorSuccess)
          Assert.assertArrayEquals(gorLogReg.beta, stats.b.toArray, tol)
        }
        case _ => {
          Assert.assertTrue(!gorSuccess)
        }
      }
    }
  }
}
