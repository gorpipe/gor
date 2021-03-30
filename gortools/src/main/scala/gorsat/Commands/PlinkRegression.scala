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

import gorsat.Commands.CommandParseUtilities._
import gorsat.external.plink.{PlinkArguments, PlinkProcessAdaptor, PlinkVcfProcessAdaptor}
import gorsat.process.GorJavaUtilities
import gorsat.process.GorJavaUtilities.{PhenoInfo, Phenotypes}
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

import java.nio.file.{Files, Paths}
import java.util.Optional

class PlinkRegression extends CommandInfo("PLINKREGRESSION",
  CommandArguments("-hc -firth -imp -dom -rec -cvs -vs -qn -vcf -1", "-residualize -covar -threshold -hwe -geno -maf", 1, 1),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String, commandRuntime:CommandRuntime): CommandParsingResult = {
    val hideCovarOption = "-hc"
    val firthOption = "-firth"
    val residualizeOption = "-residualize"
    val impOption = "-imp"
    val domOption = "-dom"
    val recOption = "-rec"
    val cvsOption = "-cvs"
    val vsOption = "-vs"
    val qnOption = "-qn"
    val vcfOption = "-vcf"
    val phenoOption = "-1"

    val covarOption = "-covar"
    val thresholdOption = "-threshold"
    val hweOption = "-hwe"
    val genoOption = "-geno"
    val mafOption = "-maf"

    val firth = hasOption(args, firthOption)

    val imputed = hasOption(args, impOption)
    val thresholdSet = hasOption(args, thresholdOption)
    val dom = hasOption(args, domOption)
    val rec = hasOption(args, recOption)
    val cvs = hasOption(args, cvsOption)
    val vs = hasOption(args, vsOption)
    val qn = hasOption(args, qnOption)
    val hc = hasOption(args, hideCovarOption)
    val vcf = hasOption(args, vcfOption)
    val ph = hasOption(args, phenoOption)

    if (!imputed && thresholdSet) throw new GorParsingException("The -threshold option is only allowed together with the -imp option.")

    val pheno = iargs(0)
    val covar = stringValueOfOptionWithDefault(args, covarOption, null)
    val threshold = doubleValueOfOptionWithDefaultWithRangeCheck(args, thresholdOption,0.9,0, 1).toFloat
    val hwe = doubleValueOfOptionWithDefaultWithRangeCheck(args, hweOption, -1).toFloat
    val geno = doubleValueOfOptionWithDefaultWithRangeCheck(args, genoOption, -1).toFloat
    val maf = doubleValueOfOptionWithDefaultWithRangeCheck(args, mafOption, -1).toFloat
    val residualize = stringValueOfOptionWithDefault(args, residualizeOption, "")

    val inHeaderCols = forcedInputHeader.split('\t')
    val colIndices = if(vcf) getColumnIndices(inHeaderCols, "(RS|ID).*", "REF.*", "ALT.*") else getColumnIndices(inHeaderCols, "(RS|ID).*", "REF.*", "ALT.*", "VALUE.*")
    if (colIndices.tail.contains(-1)) {
      throw new GorParsingException("There must be a reference allele column, alternative allele column and value column.")
    }

    val phenoinfo = getPhenotype(pheno, context)
    val phenonames = if(phenoinfo.isPresent) phenoinfo.get().phenotypeNames else Array[String]()
    val plinkArguments = new PlinkArguments(phenonames, pheno, covar, residualize, firth, hc, dom, rec, vs, qn, cvs, ph, hwe, geno, maf)

    val headerBuilder = new StringBuilder()
    headerBuilder.append(inHeaderCols(0))
    headerBuilder.append('\t')
    headerBuilder.append(inHeaderCols(1))
    headerBuilder.append('\t')
    if (colIndices(0) == -1) headerBuilder.append("ID") else headerBuilder.append(inHeaderCols(colIndices(0)))
    headerBuilder.append('\t')
    headerBuilder.append(inHeaderCols(colIndices(1)))
    headerBuilder.append('\t')
    headerBuilder.append(inHeaderCols(colIndices(2)))
    headerBuilder.append('\t')

    val phenotype = if(phenoinfo.isPresent) phenoinfo.get().phenotype else Phenotypes.BINARY
    if(phenotype.equals(Phenotypes.BINARY)) headerBuilder.append("A1\tFIRTH\tTEST\tOBS_CT\tOR\tLOG_OR_SE\tZ_STAT\tP\tERRCODE\tPHENO")
    else if(phenotype.equals(Phenotypes.MIXED)) headerBuilder.append("A1\tFIRTH?\tTEST\tOBS_CT\tOR\tLOG(OR)_SE\tZ_STAT\tP\tERRCODE\tPHENO")
    else headerBuilder.append("A1\tTEST\tOBS_CT\tBETA\tSE\tT_STAT\tP\tERRCODE\tPHENO")

    val header = headerBuilder.toString()
    val pip = if( vcf ) new PlinkVcfProcessAdaptor(context.getSession, plinkArguments, colIndices(1), colIndices(2), colIndices(0), if( colIndices.length == 4 ) colIndices(3) else -1, !imputed, threshold, vcf, forcedInputHeader, header)
    else new PlinkProcessAdaptor(context.getSession, plinkArguments, colIndices(1), colIndices(2), colIndices(0), if( colIndices.length == 4 ) colIndices(3) else -1, !imputed, threshold, vcf, header)
    CommandParsingResult(pip, header)
  }

  private def getPhenotype(pheno: String, gorContext: GorContext) : Optional[PhenoInfo] = {
    var phenoPath = Paths.get(pheno)
    if(!phenoPath.isAbsolute) {
      val root = gorContext.getSession.getProjectContext.getRoot
      val rootExtract = root.split("[ \t]+")(0)
      val rootPath = Paths.get(rootExtract)
      phenoPath = rootPath.resolve(phenoPath)
    }
    var phenoinfo: Optional[PhenoInfo] = Optional.empty()
    if(Files.exists(phenoPath)) {
      val phenoReader = Files.newBufferedReader(phenoPath)
      val phenoHeader = phenoReader.readLine()
      phenoinfo = Optional.of(GorJavaUtilities.getPhenotype(phenoHeader, phenoReader))
    }
    phenoinfo
  }

  private def getColumnIndices(inHeader: Array[String], cols: String*): Array[Int] = {
    val inHeaderToUpper = inHeader.map(_.toUpperCase())
    if (!cols.forall(col => inHeaderToUpper.count(_.matches(col)) <= 1)) {
      throw new GorParsingException("Ambiguous column names in header:\t" + inHeader.mkString("\t"))
    }
    cols.map(col => inHeaderToUpper.indexWhere(in => in.matches(col))).toArray
  }
}
