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

package gorsat.Commands

import gorsat.Analysis.{LinearRegressionAnalysis, LogisticRegressionAnalysis}
import gorsat.Commands.CommandParseUtilities._
import gorsat.Utilities.IteratorUtilities.validHeader
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class Regression extends CommandInfo("REGRESSION",
  CommandArguments("-logistic -linear -imp", "-covar -s", 1, 1),
  CommandOptions(gorCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    val inputHeader = forcedInputHeader

    val valCol = columnFromHeader("values", inputHeader.toLowerCase, executeNor)

    val lookupSignature = iargs.mkString("#")

    val linear = hasOption(args, "-linear")
    val logistic = hasOption(args, "-logistic")

    if (!(linear ^ logistic)) {
      throw new GorParsingException("You must specify exactly one of the options -logistic and -linear")
    }

    val headerBuilder = inputHeader.split("\t").zipWithIndex.filter(_._2 != valCol).map(_._1)
      .foldLeft(new StringBuilder)((builder, col) => {
        builder.append(col)
        builder.append('\t')
      })

    if (linear) headerBuilder.append("Phenotype\tCovariate\tbeta\tStd_Error\tZ_stat\tP_value")
    else headerBuilder.append("Phenotype\tCovariate\tbeta\tZ_stat\tP_value")

    val phenoFile = iargs(0)

    val covarFile = if (hasOption(args, "-covar")) Some(stringValueOfOption(args, "-covar")) else None

    val sep = if (hasOption(args, "-s")) Some(stringValueOfOption(args, "-s")) else None

    val imputed = hasOption(args, "-imp")

    (imputed, sep) match {
      case (true, Some(x)) => throw new GorParsingException("It is not allowed to use both -imp and -s")
      case _=> //Ok.
    }

    val combinedHeader = validHeader(headerBuilder.toString)

    val pipeStep = if (linear) new LinearRegressionAnalysis(lookupSignature, context.getSession, valCol, sep, imputed, phenoFile, covarFile) else new LogisticRegressionAnalysis(lookupSignature, context.getSession, valCol, sep, imputed, phenoFile, covarFile)
    CommandParsingResult(pipeStep, combinedHeader)
  }
}
