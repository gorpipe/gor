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

import java.io.{File, FileWriter}
import java.nio.file.Files
import Regression.{LinearRegression, LogisticRegression}
import org.junit.runner.RunWith
import org.junit.{Assert, ComparisonFailure}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

import scala.reflect.ClassTag

@RunWith(classOf[JUnitRunner])
class UTestRegression extends AnyFunSuite {
  val tmpDir: File = Files.createTempDirectory("uTestRegression").toFile
  tmpDir.deleteOnExit()

  test("test logistic regression - gor") {
    //Write gor file
    val gorFile = new File(tmpDir, "gorFile.gor")
    val gorFileWriter = new FileWriter(gorFile)
    gorFileWriter.write("Chrom\tPos\tValues\nchr1\t1\t0.5,0.75,1,1.25,1.5,1.75,1.75,2,2.25,2.5,2.75,3,3.25,3.5,4,4.25,4.5,4.75,5,5.5\n")
    gorFileWriter.close()
    val gorFileName = gorFile.getAbsolutePath

    //Write pheno file
    val phenoFile = new File(tmpDir, "pheno.tsv")
    val phenoFileWriter = new FileWriter(phenoFile)
    phenoFileWriter.write("PN\tPassed\n1\t1\n2\t1\n3\t1\n4\t1\n5\t1\n6\t1\n7\t2\n8\t1\n9\t2\n10\t1\n11\t2\n12\t1\n13\t2\n14\t1\n15\t2\n16\t2\n17\t2\n18\t2\n19\t2\n20\t2\n")
    phenoFileWriter.close()
    val phenoFileName = phenoFile.getAbsolutePath

    val res = TestUtils.runGorPipe(String.join(" ", "gor ", gorFileName, "| regression -logistic -s ,", phenoFileName))
    Assert.assertEquals( "Chrom\tPos\tPhenotype\tCovariate\tbeta\tZ_stat\tP_value\nchr1\t1\tPassed\tIntercept\t-4.0777\t-2.3156\t0.020582\nchr1\t1\tPassed\tGenotype\t1.5046\t2.3932\t0.016703\n", res)
  }

  test("test logistic regression - comprehensive") {
    val numberOfSamples = 100
    val missingProbs = 0.1
    for (numberOfPhenoTypes <- 1 to 3;
         numberOfCovs <- 1 to 3;
         numberOfPositions <- 1 to 3) {
      testWithUnknownsLogistic(numberOfSamples, numberOfPhenoTypes, missingProbs, numberOfCovs, numberOfPositions)
    }
  }

  test("test linear regression - comprehensive") {
    val numberOfSamples = 100
    val missingProbs = 0.1
    for (numberOfPhenoTypes <- 1 to 3;
         numberOfCovs <- 1 to 3;
         numberOfPositions <- 1 to 3) {
      testWithUnknownsLinear(numberOfSamples, numberOfPhenoTypes, missingProbs, numberOfCovs, numberOfPositions)
    }
  }

  def testWithUnknownsLogistic(numberOfSamples: Int, numberOfPhenoTypes: Int, missingProbs: Double, numberOfCovs: Int =  0, numberOfVariants: Int = 1): Unit = {
    //Probabilities
    val probs = Array.tabulate(numberOfVariants)(_ => getRandomProbabilities(numberOfSamples))
    val charEncodedDosages = probs.map(_.map(pr => pr.tail.map(x => (Math.round((1 - x) * 93) + 33).toByte)))
    val dosages = charEncodedDosages.map(_.map(_.map(x => 1 - (x - 33d) / 93)).map(pr => pr(0) + 2 * pr(1)))

    val genoFilters = Array.tabulate(numberOfVariants)(_=> Array.tabulate(numberOfSamples)(_=> Math.random() > missingProbs))

    val gorFileName: String = writeGorFile(numberOfVariants, charEncodedDosages, genoFilters)

    val hasCovs = numberOfCovs > 0
    val covs = Array.tabulate(numberOfCovs)(_=> getRandomVector(numberOfSamples, 0, 10))
    val betas = Array.tabulate(numberOfPhenoTypes)(_ => Array.tabulate(numberOfCovs + 1)(_ => Math.random() * 10 - 5))
    val xs = dosages.map(x => x +: covs)
    val ys = betas.map(getPhenoTypes(_, covs, numberOfSamples))

    val phenoFilters = Array.tabulate(numberOfPhenoTypes)(_=> Array.tabulate(numberOfSamples)(_=> Math.random() > missingProbs))

    val func: (Boolean, Boolean) => String = (p, f) => {
      if (f) {
        if (p) "2" else "1"
      } else "3"
    }
    val phenoFileName = writePhenoFile(numberOfPhenoTypes, phenoFilters, ys, func)

    val gorQuery = if (hasCovs) {
      val covFileName = writeCovFile(numberOfCovs, covs)
      String.join(" ", "gor", gorFileName, "| regression -logistic", phenoFileName, "-covar", covFileName, "-imp")
    } else {
      String.join(" ", "gor", gorFileName, "| regression -logistic", phenoFileName, "-imp")
    }

    val res = TestUtils.runGorPipe(gorQuery)

    val parseFunc = (x: Array[Array[Double]]) => {
      if (x.nonEmpty) Some((x(0), x(1), x(2)))
      else None
    }
    val results = parseResults(res, numberOfPhenoTypes, numberOfCovs + 2, parseFunc)

    val run = (x: Array[Array[Double]], y: Array[Boolean], r: LogisticRegression) => {
      val converged = r.runRegression(x, y, y.length, 1e-5, 20)
      if (converged) {
        Some((r.beta.clone(), r.zStats.clone(), r.pValues.clone()))
      } else None
    }
    val glr = LogisticRegression.getGorLogisticRegressionObject(numberOfCovs + 1, numberOfSamples)
    val wantedResults = getCorrectResults(numberOfCovs, numberOfSamples, xs, genoFilters, ys, phenoFilters, glr, run)

    val validate = (wPhenoRes: Option[(Array[Double], Array[Double], Array[Double])], phenoRes: Option[(Array[Double], Array[Double], Array[Double])]) => {
      (wPhenoRes, phenoRes) match {
        case (Some((wb, wz, wp)), Some((b, z, p))) =>
          Assert.assertArrayEquals(wb.map("%.5g".format(_).toDouble), b, 0)
          Assert.assertArrayEquals(wz.map("%.5g".format(_).toDouble), z, 0)
          Assert.assertArrayEquals(wp.map("%.5g".format(_).toDouble), p, 0)
        case (None, None) => //Ok
        case _ => throw new ComparisonFailure("Either both should converge or both diverge.", "", "")
      }
    }
    compare(results, wantedResults, validate)
  }

  def testWithUnknownsLinear(numberOfSamples: Int, numberOfPhenoTypes: Int, missingProbs: Double, numberOfCovs: Int =  0, numberOfVariants: Int = 1): Unit = {
    //Probabilities
    val probs = Array.tabulate(numberOfVariants)(_ => getRandomProbabilities(numberOfSamples))
    val charEncodedDosages = probs.map(_.map(pr => pr.tail.map(x => (Math.round((1 - x) * 93) + 33).toByte)))
    val dosages = charEncodedDosages.map(_.map(_.map(x => 1 - (x - 33d) / 93)).map(pr => pr(0) + 2 * pr(1)))

    val genoFilters = Array.tabulate(numberOfVariants)(_=> Array.tabulate(numberOfSamples)(_=> Math.random() > missingProbs))

    val gorFileName: String = writeGorFile(numberOfVariants, charEncodedDosages, genoFilters)

    val hasCovs = numberOfCovs > 0
    val covs = Array.tabulate(numberOfCovs)(_=> getRandomVector(numberOfSamples, 0, 10))
    val xs = dosages.map(x => x +: covs)
    val ys = Array.tabulate(numberOfPhenoTypes)(_ => getRandomVector(numberOfSamples, 0, 10))

    val phenoFilters = Array.tabulate(numberOfPhenoTypes)(_=> Array.tabulate(numberOfSamples)(_=> Math.random() > missingProbs))

    val run = (x: Array[Array[Double]], y: Array[Double], r: LinearRegression) => {
      r.setData(x, y.length)
      r.runRegression(y)
      (r.beta.clone(), r.betaError.clone(), r.tStats.clone(), r.pValues.clone())
    }
    val lr = new LinearRegression(numberOfSamples, numberOfCovs + 1)

    val wantedResults = getCorrectResults(numberOfCovs, numberOfSamples, xs, genoFilters, ys, phenoFilters, lr, run)

    val func: (Double, Boolean) => String = (p, f) => {
      if (f) p.toString
      else Double.NaN.toString
    }

    val phenoFileName = writePhenoFile(numberOfPhenoTypes, phenoFilters, ys, func)

    val gorQuery = if (hasCovs) {
      val covFileName = writeCovFile(numberOfCovs, covs)
      String.join(" ", "gor", gorFileName, "| regression -linear", phenoFileName, "-covar", covFileName, "-imp")
    } else {
      String.join(" ", "gor", gorFileName, "| regression -linear", phenoFileName, "-imp")
    }

    val res = TestUtils.runGorPipe(gorQuery)

    val parseFunc = (x: Array[Array[Double]]) => (x(0), x(1), x(2), x(3))

    val results = parseResults(res, numberOfPhenoTypes, numberOfCovs + 2, parseFunc)

    val validate = (x: (Array[Double], Array[Double], Array[Double], Array[Double]), y: (Array[Double], Array[Double], Array[Double], Array[Double])) => {
      Assert.assertArrayEquals(x._1.map("%.5g".format(_).toDouble), y._1, 0)
      Assert.assertArrayEquals(x._2.map("%.5g".format(_).toDouble), y._2, 0)
      Assert.assertArrayEquals(x._3.map("%.5g".format(_).toDouble), y._3, 0)
      Assert.assertArrayEquals(x._4.map("%.5g".format(_).toDouble), y._4, 0)
    }
    compare(results, wantedResults, validate)
  }

  private def compare[T](results: Array[Array[T]],
                      wantedResults: Array[Array[T]], validate: (T, T) => Unit) = {
    (wantedResults zip results).foreach({
      case (wVariantRes, variantRes) =>
        (wVariantRes zip variantRes).foreach({
          case (wPhenoRes, phenoRes) => validate(wPhenoRes, phenoRes)
        })
    })
  }

  private def writeGorFile(numberOfVariants: Int, charEncodedDosages: Array[Array[Array[Byte]]], genoFilters: Array[Array[Boolean]]) = {
    val valueColumns = (charEncodedDosages zip genoFilters).map {
      case (dos, filter) =>
        (dos zip filter).foldLeft(new StringBuilder){
          case (builder, (dos, true)) =>
            builder.append(dos(0).toChar)
            builder.append(dos(1).toChar)
          case (builder, _) =>
            builder.append("  ")
        }
    }

    val keyColumns = Array.tabulate(numberOfVariants)(pos => {
      "chr1\t" + (pos + 1) + "\tA\tC\t"
    })

    val gorFile = new File(tmpDir, "gorFile.gor")
    val gorFileName = gorFile.getAbsolutePath
    val gorFileWriter = new FileWriter(gorFile)
    gorFileWriter.write("CHROM\tPOS\tREF\tALT\tVALUES\n")
    (keyColumns zip valueColumns).foreach(kv => {
      gorFileWriter.write(kv._1 + kv._2 + "\n")
    })
    gorFileWriter.close()
    gorFileName
  }

  private def writePhenoFile[T](numberOfPhenoTypes: Int, phenoFilters: Array[Array[Boolean]], ys: Array[Array[T]], func: (T, Boolean) => String): String = {
    val phenoFile = new File(tmpDir, "phenoFile.tsv")
    val phenoFileWriter = new FileWriter(phenoFile)
    phenoFileWriter.write("IID\t" + Range(0, numberOfPhenoTypes).map("pheno_" + _).mkString("\t") + "\n")
    val phenoT = ys.transpose
    val phenoFilterT = phenoFilters.transpose

    (phenoT zip phenoFilterT).zipWithIndex.map({
      case ((ph, fi), id) =>
        id + "\t" +
          (ph zip fi).map({
            case (p, f) => func(p, f)
          }).mkString("\t")
    }).foreach(l => phenoFileWriter.write(l + "\n"))
    phenoFileWriter.close()
    phenoFile.getAbsolutePath
  }

  private def writeCovFile(numberOfCovs: Int, covs: Array[Array[Double]]): String = {
    val covFile = new File(tmpDir, "covFile.tsv")
    val covFileWriter = new FileWriter(covFile)
    covFileWriter.write("IID\t" + Range(0, numberOfCovs).map(i => "cov_" + i).mkString("\t") + "\n")
    covs.transpose.zipWithIndex.foreach({ case (cov, id) =>
      covFileWriter.write(id + "\t" + cov.mkString("\t") + "\n")
    })
    covFileWriter.close()
    covFile.getAbsolutePath
  }

  private def getCorrectResults[R, S: ClassTag, T: ClassTag](numberOfCovs: Int, numberOfSamples: Int, xs: Array[Array[Array[Double]]], genoFilters: Array[Array[Boolean]],
                                                   ys: Array[Array[S]], phenoFilters: Array[Array[Boolean]], regObj: R, run: (Array[Array[Double]], Array[S], R) => T): Array[Array[T]] = {
    (xs zip genoFilters).map({
      case (x, genoFilter) =>
        (ys zip phenoFilters).map({
          case (y, phenoFilter) =>
            val totalFilter = (genoFilter zip phenoFilter).map(x => x._1 && x._2)
            val xFilt = (x.transpose zip totalFilter).filter(_._2).map(_._1).transpose
            val yFilt = (y zip totalFilter).filter(_._2).map(_._1)
            run(xFilt, yFilt, regObj)
        })
    })
  }

  def parseResults[T: ClassTag](res: String, numberOfPhenoTypes: Int, betaLen: Int, func: Array[Array[Double]] => T): Array[Array[T]] = {
    res.split('\n').tail.map(line => {
      line.split('\t').drop(6).map(_.toDouble)
    }).grouped(betaLen).toArray.map(_.transpose).map(x => func(x)).grouped(numberOfPhenoTypes).toArray
  }

  def getRandomProbabilities(n: Int): Array[Array[Double]] = {
    Array.tabulate(n)(_ => getProbabilityTriple)
  }

  def getProbabilityTriple: Array[Double] = {
    val toReturn = Array.tabulate(3)(_=> Math.random())
    val sum = toReturn.foldLeft(0d)((sum, x) => sum + x)
    toReturn.map(x => x / sum)
  }

  def encode(probs: Array[Array[Double]], filter: Array[Boolean]): String = {
    (probs zip filter).foldLeft(new StringBuilder())({ case (builder, (pr,f)) =>
      if (f) {
        builder.append(Math.round((1 - pr(1)) * 93 + 33).toChar)
        builder.append(Math.round((1 - pr(2)) * 93 + 33).toChar)
      } else builder.append("  ")
    }).toString
  }

  def getPhenoTypes(beta: Array[Double], x: Array[Array[Double]], n: Int): Array[Boolean] = {
    (x zip beta.tail).foldLeft(Array.fill(n)(beta.head))((v, xb) => (v zip xb._1).map(vx => vx._1 + xb._2 * vx._2))
      .map(z => 1 / (1 + Math.exp(-z))).map(Math.random() < _)
  }

  def dosages(probs: Array[Array[Double]]): Array[Double] = {
    probs.map(prs => {
      val c1 = Math.round((1 - prs(1)) * 93) + 33
      val c2 = Math.round((1 - prs(2)) * 93) + 33
      val p1 = 1 - (c1 - 33d) / 93
      val p2 = 1 - (c2 - 33d) / 93
      p1 + 2 * p2
    })
  }

  def getRandomVector(len: Int, a: Double, b: Double): Array[Double] = {
    Array.tabulate(len)(_ => a + b * Math.random())
  }
}
