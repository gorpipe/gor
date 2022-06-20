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

package gorsat.Regression

import breeze.linalg._
import breeze.numerics._
import cern.jet.stat.Probability

/**
  * This is code taken from the open source Hail framework, adapted to our needs.
  */
class WaldRegressionTestResult(val stats: Option[WaldStats]) {
}

class WaldRegressionTestResultWithFit(override val stats: Option[WaldStats], val fitStats: LogisticRegressionFit) extends WaldRegressionTestResult(stats) {
}

object WaldTest {

  def test(X: DenseMatrix[Double], y: DenseVector[Double], nullFit: LogisticRegressionFit, maxIter: Int = 25, tol: Double = 1e-6): WaldRegressionTestResultWithFit = {
    require(nullFit.fisher.isDefined)

    val model = new LogisticRegressionModel(X, y)
    val fit = model.fit(Some(nullFit), maxIter, tol)

    val waldStats = if (fit.converged) {
      try {
        val se = sqrt(diag(inv(fit.fisher.get)))
        val z = fit.b /:/ se
        val p = z.map(zi => 2 * Probability.normal(-math.abs(zi)))

        Some(WaldStats(fit.b, se, z, p))
      } catch {
        case e: breeze.linalg.MatrixSingularException => None
        case e: breeze.linalg.NotConvergedException => None
      }
    } else None

    new WaldRegressionTestResultWithFit(waldStats, fit)
  }
}

case class WaldStats(b: DenseVector[Double], se: DenseVector[Double], z: DenseVector[Double], p: DenseVector[Double]) {
}

class LogisticRegressionModel(X: DenseMatrix[Double], y: DenseVector[Double]) {
  require(y.length == X.rows)
  require(y.forall(yi => yi == 0 || yi == 1))
  require{ val sumY = sum(y); sumY > 0 && sumY < y.length }

  val n: Int = X.rows
  val m: Int = X.cols

  def bInterceptOnly(): DenseVector[Double] = {
    require(m > 0)
    val b = DenseVector.zeros[Double](m)
    val avg = sum(y) / n
    b(0) = math.log(avg / (1 - avg))
    b
  }

  def fit(optNullFit: Option[LogisticRegressionFit] = None, maxIter: Int = 25, tol: Double = 1E-6): LogisticRegressionFit = {

    val b = DenseVector.zeros[Double](m)
    val mu = DenseVector.zeros[Double](n)
    val score = DenseVector.zeros[Double](m)
    val fisher = DenseMatrix.zeros[Double](m, m)

    optNullFit match {
      case None =>
        b := bInterceptOnly()
        mu := sigmoid(X * b)
        score := X.t * (y - mu)
        fisher := X.t * (X(::, *) *:* (mu *:* (1d - mu)))
      case Some(nullFit) =>
        val m0 = nullFit.b.length

        val r0 = 0 until m0
        val r1 = m0 until m.intValue()

        val X0 = X(::, r0)
        val X1 = X(::, r1)

        b(r0) := nullFit.b
        mu := sigmoid(X * b)
        score(r0) := nullFit.score.get
        score(r1) := X1.t * (y - mu)
        fisher(r0, r0) := nullFit.fisher.get
        fisher(r0, r1) := X0.t * (X1(::, *) *:* (mu *:* (1d - mu)))
        fisher(r1, r0) := fisher(r0, r1).t
        fisher(r1, r1) := X1.t * (X1(::, *) *:* (mu *:* (1d - mu)))
    }

    var iter = 1
    var converged = false
    var exploded = false

    val deltaB = DenseVector.zeros[Double](m)

    while (!converged && !exploded && iter <= maxIter) {
      try {
        deltaB := fisher \ score

        if (max(abs(deltaB)) < tol) {
          converged = true
        } else {
          iter += 1
          b += deltaB
          mu := sigmoid(X * b)
          score := X.t * (y - mu)
          fisher := X.t * (X(::, *) *:* (mu *:* (1d - mu)))
        }
      } catch {
        case e: breeze.linalg.MatrixSingularException => exploded = true
        case e: breeze.linalg.NotConvergedException => exploded = true
      }
    }

    val logLkhd = sum(breeze.numerics.log((y *:* mu) + ((1d - y) *:* (1d - mu))))

    LogisticRegressionFit(b, Some(score), Some(fisher), logLkhd, iter, converged, exploded)
  }
}

case class LogisticRegressionFit(
                                  b: DenseVector[Double],
                                  score: Option[DenseVector[Double]],
                                  fisher: Option[DenseMatrix[Double]],
                                  logLkhd: Double,
                                  nIter: Int,
                                  converged: Boolean,
                                  exploded: Boolean) {

}
