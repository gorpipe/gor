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

package gorsat.parser

import cern.jet.stat.Probability
import gorsat.parser.FunctionSignature._
import gorsat.parser.FunctionTypes.{dFun, iFun}
import org.apache.commons.math3.distribution.ChiSquaredDistribution
import org.gorpipe.gor.util.GChiSquared2by2

object StatisticalFunctions {
  def register(functions: FunctionRegistry): Unit = {
    functions.register("NEGBINOMIAL", getSignatureDoubleDoubleDouble2Double(negativeBinomial), negativeBinomial _)
    functions.register("NEGBINOMIALC", getSignatureDoubleDoubleDouble2Double(negativeBinomialComplemented), negativeBinomialComplemented _)
    functions.register("BINOMIAL", getSignatureDoubleDoubleDouble2Double(binomial), binomial _)
    functions.register("BINOMIALC", getSignatureDoubleDoubleDouble2Double(binomialComplemented), binomialComplemented _)
    functions.register("GAMMA", getSignatureDoubleDoubleDouble2Double(gamma), gamma _)
    functions.register("GAMMAC", getSignatureDoubleDoubleDouble2Double(gammaComplemented), gammaComplemented _)
    functions.register("BETAC", getSignatureDoubleDoubleDouble2Double(betaComplemented), betaComplemented _)
    functions.register("BETA", getSignatureDoubleDoubleDouble2Double(beta), beta _)
    functions.register("POISSON", getSignatureDoubleDouble2Double(poisson), poisson _)
    functions.register("POISSONC", getSignatureDoubleDouble2Double(poissonComplemented), poissonComplemented _)
    functions.register("STUDENT", getSignatureDoubleDouble2Double(studentT), studentT _)
    functions.register("INVSTUDENT", getSignatureDoubleDouble2Double(studentTInverse), studentTInverse _)
    functions.register("CHISQUARECOMPL", getSignatureDoubleDouble2Double(chiSquareComplemented), chiSquareComplemented _)
    functions.register("CHI2", getSignatureDoubleDouble2Double(chiSquareComplemented), chiSquareComplemented _)
    functions.register("CHISQUARE", getSignatureDoubleDouble2Double(chiSquare), chiSquare _)
    functions.register("ERF", getSignatureDouble2Double(erf), erf _)
    functions.register("ERFC", getSignatureDouble2Double(erfc), erfc _)
    functions.register("NORMAL", getSignatureDouble2Double(normal), normal _)
    functions.register("INVNORMAL", getSignatureDouble2Double(invnormal), invnormal _)
    functions.register("PVAL", getSignatureIntIntIntInt2Double(pval), pval _)
    functions.register("PVALONE", getSignatureIntIntIntInt2Double(pvalOne), pvalOne _)
    functions.register("CHI", getSignatureIntIntIntInt2Double(chi), chi _)
    functions.register("INVCHISQUARE", getSignatureDoubleInt2Double(invChiSquare), invChiSquare _)
  }

  def chi(ex1: iFun, ex2: iFun, ex3: iFun, ex4: iFun): dFun = {
    cvp =>
      GChiSquared2by2.computeLogLikelihoodChiSquared(ex1(cvp), ex2(cvp), ex3(cvp), ex4(cvp))
  }

  def pvalOne(ex1: iFun, ex2: iFun, ex3: iFun, ex4: iFun): dFun = {
    cvp => {
      ParseUtilities.twoByTwoPvalOneTailed(ex1(cvp), ex2(cvp), ex3(cvp), ex4(cvp))
    }
  }

  def pval(ex1: iFun, ex2: iFun, ex3: iFun, ex4: iFun): dFun = {
    cvp => {
      ParseUtilities.twoByTwoPvalTwoTailed(ex1(cvp), ex2(cvp), ex3(cvp), ex4(cvp))
    }
  }

  def erf(ex: dFun): dFun = {
    cvp => {
      Probability.errorFunction(ex(cvp))
    }
  }

  def erfc(ex: dFun): dFun = {
    cvp => {
      Probability.errorFunctionComplemented(ex(cvp))
    }
  }

  def normal(ex: dFun): dFun = {
    cvp => {
      Probability.normal(ex(cvp))
    }
  }

  def invnormal(ex: dFun): dFun = {
    cvp => {
      Probability.normalInverse(ex(cvp))
    }
  }

  def chiSquareComplemented(ex1: dFun, ex2: dFun): dFun = {
    cvp => {
      Probability.chiSquareComplemented(ex1(cvp), ex2(cvp))
    }
  }

  def chiSquare(ex1: dFun, ex2: dFun): dFun = {
    cvp => {
      Probability.chiSquare(ex1(cvp), ex2(cvp))
    }
  }

  def invChiSquare(ex1: dFun, ex2: iFun): dFun = {
    cvp => {
      val dist = new ChiSquaredDistribution(ex2(cvp))
      dist.inverseCumulativeProbability(ex1(cvp))
    }
  }

  def studentT(ex1: dFun, ex2: dFun): dFun = {
    cvp => {
      Probability.studentT(ex1(cvp), ex2(cvp))
    }
  }

  def studentTInverse(ex1: dFun, ex2: dFun): dFun = {
    cvp => {
      Probability.studentTInverse(ex1(cvp), ex2(cvp).toInt)
    }
  }

  def poisson(ex1: dFun, ex2: dFun): dFun = {
    cvp => {
      Probability.poisson(ex1(cvp).toInt, ex2(cvp))
    }
  }

  def poissonComplemented(ex1: dFun, ex2: dFun): dFun = {
    cvp => {
      Probability.poissonComplemented(ex1(cvp).toInt, ex2(cvp))
    }
  }

  def beta(ex1: dFun, ex2: dFun, ex3: dFun): dFun = {
    cvp => {
      Probability.beta(ex1(cvp), ex2(cvp), ex3(cvp))
    }
  }

  def betaComplemented(ex1: dFun, ex2: dFun, ex3: dFun): dFun = {
    cvp => {
      Probability.betaComplemented(ex1(cvp), ex2(cvp), ex3(cvp))
    }
  }

  def gamma(ex1: dFun, ex2: dFun, ex3: dFun): dFun = {
    cvp => {
      Probability.gamma(ex1(cvp), ex2(cvp), ex3(cvp))
    }
  }

  def gammaComplemented(ex1: dFun, ex2: dFun, ex3: dFun): dFun = {
    cvp => {
      Probability.gammaComplemented(ex1(cvp), ex2(cvp), ex3(cvp))
    }
  }

  def binomial(ex1: dFun, ex2: dFun, ex3: dFun): dFun = {
    cvp => {
      Probability.binomial(ex1(cvp).toInt, ex2(cvp).toInt, ex3(cvp))
    }
  }

  def binomialComplemented(ex1: dFun, ex2: dFun, ex3: dFun): dFun = {
    cvp => {
      Probability.binomialComplemented(ex1(cvp).toInt, ex2(cvp).toInt, ex3(cvp))
    }
  }

  def negativeBinomial(ex1: dFun, ex2: dFun, ex3: dFun): dFun = {
    cvp => {
      Probability.negativeBinomial(ex1(cvp).toInt, ex2(cvp).toInt, ex3(cvp))
    }
  }

  def negativeBinomialComplemented(ex1: dFun, ex2: dFun, ex3: dFun): dFun = {
    cvp => {
      Probability.negativeBinomialComplemented(ex1(cvp).toInt, ex2(cvp).toInt, ex3(cvp))
    }
  }
}
