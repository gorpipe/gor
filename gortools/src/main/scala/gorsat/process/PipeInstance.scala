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

package gorsat.process

import java.io.File
import java.nio.file.{Files, Paths}
import java.util
import java.util.Optional
import gorsat.Analysis._
import gorsat.Commands.CommandParseUtilities.{hasOption, rangeOfOption, stringValueOfOption}
import gorsat.Commands.{Analysis, _}
import gorsat.DynIterator.DynamicRowSource
import gorsat.Iterators.StdInputSourceIterator
import gorsat.Monitors.{CancelMonitor, MemoryMonitor, TimeoutMonitor}
import gorsat.Script.{ScriptEngineFactory, ScriptExecutionEngine, ScriptParsers}
import gorsat.Utilities.IteratorUtilities.validHeader
import gorsat._
import gorsatGorIterator.{MemoryMonitorUtil, gorsatGorIterator}
import process.GorJavaUtilities.CmdParams
import process.GorPipe.brsConfig
import org.gorpipe.exceptions.{GorParsingException, GorResourceException, GorSystemException, GorUserException}
import org.gorpipe.gor.model.{DriverBackedFileReader, FileReader, GenomicIterator}
import org.gorpipe.gor.monitor.GorMonitor
import org.gorpipe.gor.session.{GorContext, GorSession, ProjectContext}
import org.gorpipe.gor.util.{CommandSubstitutions, StringUtil}
import org.slf4j.LoggerFactory

import java.net.InetAddress

object PipeInstance {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val DEFAULT_REQUEST_ID: String = ""

  // Set the dyniterator iterator create function
  DynIterator.createGorIterator = (context: GorContext) => createGorIterator(context)

  def initialize(): Unit = {
    GorPipeCommands.register()
    GorInputSources.register()
    GorPipeMacros.register()
  }

  /**
    * This method returns a new instance of the interior class PipeInstance() and returns PipeInstance.
    *
    * @return PipeInstance
    */
  def createGorIterator(context: GorContext): PipeInstance = {
    new PipeInstance(context)
  }

  /**
   * Insert steps for column type inference if and where needed
   * Uses recursion to walks through the chain of steps, determining if types have been maintained to that point.
   * If types are needed for the current step and are not already there, insert an InferColumnTypes step.
   * Note that when this is executed, any column type data has not yet been materialized; the analysis is of what
   * will be present at execution time.
   *
   * @param pipeStep a chain of steps to be executed
   * @param typesMaintained does the execution of preceding steps provide column type info
   * @param untypedColumnsIn for the case typesMaintained=true, which columns are excluded from the type info
   *                         null means empty, none excluded. (ignored if typesMaintained=false).
   * @return an updated chain of steps, with type inference inserted where necessary
   */
  def injectTypeInferral(pipeStep: Analysis, typesMaintained: Boolean = false, untypedColumnsIn: Array[Int] = null): Analysis = {
    val next = pipeStep.pipeTo
    pipeStep.pipeTo = null

    // create the inferral step, if any, that needs insertion before this step
    val infer = if (pipeStep.isTypeInformationNeeded) {
      if (!typesMaintained) InferColumnTypes()
      else if (untypedColumnsIn != null && untypedColumnsIn.length > 0) {
        val infer1 = InferColumnTypes()
        infer1.colsToSet = untypedColumnsIn
        infer1
      } else null
    } else null

    if (next == null) {
      if (infer != null) infer | pipeStep else pipeStep

    } else {
      if (infer != null) {
        infer | pipeStep | injectTypeInferral(next, pipeStep.isTypeInformationMaintained, pipeStep.columnsWithoutTypes(null))
      } else {
        val untypedColumns = if (pipeStep.isTypeInformationMaintained) pipeStep.columnsWithoutTypes(untypedColumnsIn) else null
        pipeStep | injectTypeInferral(next, pipeStep.isTypeInformationMaintained && typesMaintained, untypedColumns)
      }
    }
  }
}

/**
  * This class defines the pipe instance when a gor query is run, including all commands, logic, and methods for processing and parsing options in the query.
  */
class PipeInstance(context: GorContext, outputValidateOrder: Boolean = false) extends gorsatGorIterator(context) {

  // Define second constructor, needed for Java.
  def this(context: GorContext) = {
    this(context, false)
  }

  private var theIterator : GenomicIterator = _
  private var usedFiles : List[String] = Nil
  private var range: GenomicRange.Range = GenomicRange.all
  private var firstCommand: Int = 0
  private var nowithin = false
  private var pipeSteps: Array[String] = _
  private var theParams = ""
  private val session = context.getSession
  private var combinedHeader : String = _
  private var isClosed: Boolean = false

  var thePipeStep : Analysis = _
  var theInputSource: GenomicIterator = _

  PipeInstance.initialize()

  override def init(params : String, gm : GorMonitor): Unit = {
    context.getSession.getSystemContext.setMonitor(gm)
    scalaInit(params)
    isClosed = false
  }

  override def getRowSource: GenomicIterator = theIterator

  override def getHeader : String = combinedHeader
  override def getUsedFiles: List[String] = usedFiles

  def getIterator: GenomicIterator = theIterator

  def createPipestep(iparams : String, forcedInputHeader : String = ""): Analysis = {
    theParams = iparams
    if (theIterator != null) {
      close()
    }
    val args = CommandParseUtilities.quoteSafeSplit(iparams + " -stdin",' ')
    processArguments(args, isNorContext,forcedInputHeader)

    theIterator = null
    thePipeStep
  }

  override def scalaInit(iparams : String, forcedInputHeader : String = ""): Unit = {
    theParams = iparams
    if (theIterator != null) {
      close()
    }
    val args = Array(iparams)
    processArguments(args, isNorContext,forcedInputHeader)
  }

  override def hasNext : Boolean = {
    if (isClosed) {
      throw new GorSystemException("Iterator is closed", null)
    }
    theIterator.hasNext
  }

  override def next : String = {
    if (isClosed) {
      throw new GorSystemException("Iterator is closed", null)
    }
    theIterator.next().toString
  }

  def seek(chr : String, pos : Int): Unit = {  // We must re-initialize if seek is applied
    if (theIterator != null) close()
    val dynIterator = new DynamicRowSource(theParams, context)
    dynIterator.setPositionWithoutChrLimits(chr,pos)
    theIterator = dynIterator
    isClosed = false
  }

  override def close(): Unit = {
    if (theIterator != null) {
      theIterator.close()
      theIterator = null
      if (thePipeStep != null) thePipeStep.reportWantsNoMore()
    }
    if (context != null && context.getSession != null) context.getSession.close()
    isClosed = true
  }

  /**
    * This method takes an argument string, a boolean flag (to execute as a Nor query),
    * a boolean flag (whether this is fromMain or not), and a string header and returns an instance of rowSource.
    */
  override def processArguments(args: Array[String], executeNor: Boolean, forcedInputHeader: String = ""): GenomicIterator = {
    val options = new PipeOptions
    options.parseOptions(args)
    options.norContext = executeNor
    init(options.query, options.stdIn, forcedInputHeader, options.fileSignature, options.virtualFile)
  }

  def getSession : GorSession = {
    context.getSession
  }

  def getPipeStep : Analysis = thePipeStep

  def createFileReader(gorRoot: String): FileReader = {
    if (!StringUtil.isEmpty(gorRoot)) {
      new DriverBackedFileReader(null, gorRoot)
    } else {
      ProjectContext.DEFAULT_READER
    }
  }

  @Deprecated
  def subProcessArguments(pipeOptions: PipeOptions): GenomicIterator = {
    init(pipeOptions.query, pipeOptions.stdIn, "")
  }

  @Deprecated
  def subProcessArguments(inputQuery: String, fileSignature: Boolean, virtualFile: String, scriptAnalyzer: Boolean, useStdin: Boolean, forcedInputHeader: String): GenomicIterator = {
    init(inputQuery, useStdin, forcedInputHeader, false, "")
  }

  def init(inputQuery: String): GenomicIterator = {
    init(inputQuery, false, "", false, "")
  }

  def init(inputQuery: String, useStdin: Boolean, forcedInputHeader: String): GenomicIterator = {
    init(inputQuery, useStdin, forcedInputHeader, false, "")
  }

  def createScriptEngine(context: GorContext): ScriptExecutionEngine = {
    ScriptEngineFactory.create(context)
  }

  def init(inputQuery: String, useStdin: Boolean, forcedInputHeader: String, fileSignature: Boolean, virtualFile: String): GenomicIterator = {

    DynIterator.createGorIterator = (ctx: GorContext) => PipeInstance.createGorIterator(ctx)

    isNorContext = context.getSession.getNorContext
    thePipeStep = PlaceHolder()

    var argString = CommandParseUtilities.removeComments(inputQuery)
    val gorCommands = CommandParseUtilities.quoteSafeSplitAndTrim(argString, ';') // In case this is a command line script

    if (fileSignature || virtualFile != null || ScriptParsers.isScript(gorCommands)) {
      val scriptExecutionEngine = createScriptEngine(context)
      if (virtualFile != null && virtualFile.nonEmpty) {
        System.out.println(scriptExecutionEngine.executeVirtualFile(virtualFile, gorCommands))
        System.exit(0)
      } else if (fileSignature) {
        System.out.println(scriptExecutionEngine.executeSuggestName(gorCommands))
        System.exit(0)
      } else {
        argString = scriptExecutionEngine.execute(gorCommands)
      }
    }

    pipeSteps = CommandParseUtilities.quoteSafeSplitAndTrim(argString, '|')

    if (pipeSteps.length == 0) {
      throw new GorParsingException("Error in GOR query - Empty query found. Are you missing the final gor line in your script?")
    }

    var gorString = pipeSteps(0)

    if (!useStdin) {
      gorString = fixGorString(gorString)
    }

    val inputSourceCommand: String = prepareInputSource(argString, gorString, useStdin)
    val inputHeader: String = preparePipeStep(argString, gorString, forcedInputHeader, inputSourceCommand)

    if (inputHeader == null || inputHeader.isEmpty) {
      throw new GorResourceException("Input source contains no header", theInputSource.getSourceName)
    }

    val types = theInputSource.getTypes
    val rowHeader = if (types!=null) RowHeader(inputHeader, types) else RowHeader(inputHeader)
    thePipeStep.setRowHeader(rowHeader)

    theIterator = new BatchedPipeStepIteratorAdaptor(theInputSource, thePipeStep, combinedHeader, brsConfig)

    theInputSource
  }

  def preparePipeStep(argString: String, gorString: String, forcedInputHeader: String, inputSourceCommand: String): String = {
    var command = ""
    var inputHeader = ""
    try {
      var pushdown: Boolean = true
      for (i <- firstCommand until pipeSteps.length) {
        // This is an embarrassing for-loop
        var aPipeStep: Analysis = null
        val fullCommand = pipeSteps(i)
        command = commandFromPipeStep(fullCommand)

        if (pushdown) {
          if (command.equals("WHERE")) {
            pushdown = pushdownFilter(fullCommand.substring(6).trim)
          } else if (command.equals("CALC")) {
            pushdown = pushdownCalc(fullCommand.substring(5).trim)
          } else if (command.equals("REPLACE")) {
            pushdown = pushdownCalc(fullCommand.substring(8).trim)
          } else if (command.equals("TOP")) {
            pushdown = pushdownTop(fullCommand.substring(4).trim)
          } else if (command.equals("WRITE")) {
            pushdown = pushdownWrite(fullCommand.substring(6).trim)
          } else if (command.equals("CMD")) {
            pushdown = pushdownCmd(fullCommand.substring(4).trim)
          } else {
            pushdown = theInputSource.pushdownGor(fullCommand)
          }

          // Fetch inputSource header only after all pushable predicates have been pushed down
          if (!pushdown) inputHeader = checkHeader(forcedInputHeader, inputSourceCommand, firstCommand, gorString)
        }

        if (!pushdown) {
          val cacheDir = null: String
          val whiteListCmdSet = if (context.getSession.getSystemContext.getCommandWhitelist == null) {
            new util.HashMap[String, CmdParams]()
          } else {
            context.getSession.getSystemContext.getCommandWhitelist.asInstanceOf[util.Map[String, CmdParams]]
          }

          val (pstep, fc, isrc) = parseCommand(command, argString, i, pipeSteps, firstCommand, theInputSource,
            isNorContext, cacheDir, whiteListCmdSet)
          aPipeStep = pstep
          firstCommand = fc
          theInputSource = isrc

          if (aPipeStep != null) {
            if (!nowithin && range.chromosome != "" && range.stop >= 0 && i == firstCommand) {
              aPipeStep = WithIn(range.chromosome, range.start, range.stop) | aPipeStep
            }
            if (i == firstCommand) {
              thePipeStep = aPipeStep
            } else {
              if (thePipeStep == null || thePipeStep.isInstanceOf[PlaceHolder]) {
                thePipeStep = aPipeStep
              } else {
                thePipeStep | aPipeStep
              }
            }
          }
        }
      }

      if (pushdown) inputHeader = checkHeader(forcedInputHeader, inputSourceCommand, firstCommand, gorString)

      if (!nowithin && range.chromosome != "" && range.stop >= 0 && pipeSteps.length == firstCommand) {
        thePipeStep = WithIn(range.chromosome, range.start, range.stop) | thePipeStep
      }

      if (!isNorContext && outputValidateOrder) {
        thePipeStep = thePipeStep | CheckOrder("Output")
      }
    } catch {
      case e: Throwable =>
        if (theInputSource != null) {
          theInputSource.close()
        }
        throw e
    }

    thePipeStep = PipeInstance.injectTypeInferral(thePipeStep)

    // Add timeout monitor
    thePipeStep = thePipeStep | TimeoutMonitor()

    // Add cancel monitor
    if (context.getSession.getSystemContext.getMonitor != null) {
      thePipeStep = CancelMonitor(context.getSession.getSystemContext.getMonitor) | thePipeStep | CancelMonitor(context.getSession.getSystemContext.getMonitor)
    }

    inputHeader
  }

  def prepareInputSource(argString: String, gorString: String, useStdin: Boolean): String = {

    val inputSourceCommand = commandFromPipeStep(gorString)
    val inputSourceInfo = GorInputSources.getInfo(inputSourceCommand)

    try {
      var inputSourceResult: InputSourceParsingResult = null
      firstCommand = 1
      var arguments = CommandParseUtilities.quoteSafeSplitAndTrim(gorString, ' ')
      arguments = expandGetValue(arguments.slice(1, arguments.length))

      if (inputSourceInfo != null) {
        inputSourceResult = inputSourceInfo.init(context, combinedHeader, argString, arguments)
      } else {
        inputSourceResult = processWhitelistedInputSource(inputSourceCommand, arguments)
      }

      if (inputSourceResult != null) {
        theInputSource = inputSourceResult.inputSource
        combinedHeader = inputSourceResult.header
        isNorContext = inputSourceResult.isNorContext
        nowithin = inputSourceResult.noWithin
        val mergeSteps = inputSourceResult.mergeSteps
        if (mergeSteps != null && mergeSteps.length > 0) {
          pipeSteps = mergeSteps ++ pipeSteps.slice(1, pipeSteps.length)
        }

        if (inputSourceResult.genomicRange != null) {
          range = inputSourceResult.genomicRange
        }

        if (inputSourceResult.usedFiles != null) {
          usedFiles ++= inputSourceResult.usedFiles
        }
      } else if (useStdin) {
        firstCommand = 0
        theInputSource = StdInputSourceIterator()
      } else {
        // Error input source not found
        throw new GorParsingException(s"Input source $inputSourceCommand is not a valid input source.")
      }

      theInputSource match {
        case processSource: ProcessRowSource =>
          isNorContext = processSource.nor
          fixHeader = fixHeader && !isNorContext
        case _ =>
      }
    } catch {
      case gue: GorUserException =>
        if (!gue.isCommandSet) {
          gue.setCommandName(inputSourceCommand)
          gue.setCommandIndex(firstCommand)
          gue.setCommandStep(gorString)
          gue.setRequestID(context.getSession.getRequestId)
          gue.setQuery(StringUtil.limitSize(theParams, 1000, 0.5))
          gue.setExtraInfo(gue.getExtraInfo() + " Hostname=" + InetAddress.getLocalHost().getHostName())
        }
        throw gue
    }
    inputSourceCommand
  }

  private def expandGetValue(arguments: Array[String]) = {
    arguments.map(arg => {
      if (arg.toUpperCase.startsWith("GETVALUE(") && arg.endsWith(")")) {
        val getValueArgs = arg.substring(9, arg.length - 1)
        val lastCommaIx = getValueArgs.lastIndexOf(',')
        val query = CommandParseUtilities.replaceSingleQuotes(getValueArgs.substring(0, lastCommaIx))
        val col = getValueArgs.substring(lastCommaIx + 1, getValueArgs.length).trim.toInt
        new OptionEvaluator(context).getValue(query, col)
      } else {
        arg
      }
    })
  }

  def checkHeader(forcedInputHeader: String, inputSourceCommand: String, firstCommand: Int, gorString: String): String = {
    var inputHeader = ""
    // TODO: We need to fix this, e.g. move exception handler out from parseCommand
    try {
      inputHeader = if (forcedInputHeader == null || forcedInputHeader == "") theInputSource.getHeader else forcedInputHeader
    } catch {
      case gue: GorUserException =>
        gue.setCommandName(inputSourceCommand)
        gue.setCommandIndex(firstCommand)
        gue.setCommandStep(gorString)
        gue.setRequestID(context.getSession.getRequestId)
        gue.setQuery(StringUtil.limitSize(theParams, 1000, 0.5))
        gue.setExtraInfo(gue.getExtraInfo() + " Hostname=" + InetAddress.getLocalHost().getHostName())
        throw gue
    }
    combinedHeader = if (fixHeader) validHeader(inputHeader) else inputHeader
    inputHeader
  }

  def pushdownFilter(filter: String): Boolean = {
    theInputSource.pushdownFilter(filter)
  }

  def pushdownCalc(calc: String): Boolean = {
    val words = calc.split(" ")
    val colName = words(0)
    val cmdStart = if (words.length > 2 && (words(1) == "=" || words(1).toUpperCase == "AS")) 2 else 1
    val formula = words.slice(cmdStart, words.length).mkString(" ")
    theInputSource.pushdownCalc(formula, colName)
  }

  def pushdownTop(top: String): Boolean = {
    try {
      val limit = Integer.parseInt(top)
      theInputSource.pushdownTop(limit)
    } catch {
      case _: NumberFormatException => false
    }
  }

  def pushdownWrite(filename: String): Boolean = {
    theInputSource.pushdownWrite(filename)
  }

  def pushdownCmd(filename: String): Boolean = {
    theInputSource.pushdownCmd(filename)
  }

  def fixGorString(gorString: String): String = {
    val command = commandFromPipeStep(gorString)

    // First check if we have input source
    // Check if we have a whitelisted command
    val inputSourceInfo = GorInputSources.getInfo(command)
    if (inputSourceInfo == null) {
      val whiteListCmdSet = context.getSession.getSystemContext.getCommandWhitelist.asInstanceOf[util.Map[String, GorJavaUtilities.CmdParams]]
      if (whiteListCmdSet == null || !getIgnoreCase(whiteListCmdSet.keySet, command).isPresent) {
        return "gor " + gorString
      }
    }

    gorString
  }

  def processWhitelistedInputSource(commandName: String, args: Array[String]): InputSourceParsingResult = {
    val whiteListCmdSet = context.getSession.getSystemContext.getCommandWhitelist.asInstanceOf[util.Map[String, GorJavaUtilities.CmdParams]]
    if (whiteListCmdSet != null) {
      var finalCommand: String = null
      var commandType: String = null
      var isNorContext = false
      val command = commandName.toLowerCase
      var range = GenomicRange.empty

      if (hasOption(args, "-p")) {
        range = rangeOfOption(args, "-p")
      }

      var scoping = hasOption(args, "-c")
      val filter = if (hasOption(args, "-f")) stringValueOfOption(args, "-f") else null

      val icmd = getIgnoreCase(whiteListCmdSet.keySet, command)

      if (icmd.isPresent) {
        val cmdparams = whiteListCmdSet.get(icmd.get)
        val cmd = cmdparams.getCommand
        val rest = if (range == GenomicRange.empty) args.mkString(" ") else args.slice(2, args.length).mkString(" ")
        scoping = cmdparams.isFormula

        if (cmd.contains("#{params}") || cmd.startsWith("sql ")) {
          finalCommand = cmd.replace("#{params}", rest)
        } else {
          finalCommand = cmd + " " + rest
        }
        isNorContext = cmdparams.isNor
        val cmdtype = cmdparams.getType
        if (cmdtype.isPresent) {
          commandType = cmdtype.get
        }
      }

      if (finalCommand != null) {
        finalCommand = CommandSubstitutions.projectReplacement(finalCommand, context.getSession)
        val inputSource = if (finalCommand.startsWith("sql ")) {
          val cmds = CommandParseUtilities.quoteSafeSplit(finalCommand.substring(4), ' ')
          finalCommand = CommandSubstitutions.filterCmd(cmds, filter).trim
          var source = if (hasOption(args, "-s")) stringValueOfOption(args, "-s") else if (commandType != null) commandType else null
          val k = finalCommand.indexOf("-s")
          if (k != -1) {
            var e = k + 2
            while (finalCommand.charAt(e) == ' ') e += 1
            e = CommandParseUtilities.quoteSafeIndexOf(finalCommand, " ", par = false, e)
            if (e == -1) e = finalCommand.length
            source = finalCommand.substring(k + 2, e).trim
            finalCommand = finalCommand.substring(0, k) + finalCommand.substring(e)
          }
          GorJavaUtilities.getDbIteratorSource(finalCommand.trim, null, source, !isNorContext, scoping)
        } else {
          val projectRoot = context.getSession.getProjectContext.getRealProjectRoot
          new ProcessRowSource(finalCommand, commandType, isNorContext, context.getSession, range, filter, false)
        }
        return InputSourceParsingResult(inputSource, "", isNorContext, range)
      }
    }

    null
  }

  def getIgnoreCase(c: util.Collection[String], str: String): Optional[String] = c.stream.filter((s: String) => s.equalsIgnoreCase(str)).findFirst

  def commandFromPipeStep(pipeStep: String): String = {
    if (pipeStep.indexOf(' ') >= 0) pipeStep.slice(0, pipeStep.indexOf(' ')).toUpperCase else pipeStep.toUpperCase
  }



  /**
    * This method takes a command string, and a string that represents the leftHeader and returns the pipe header.
    */
  def actualGetPipeHeader(command: String, leftHeader: String): String = {
    val it = PipeInstance.createGorIterator(context)
    var header = leftHeader
    try {
      if (leftHeader == "") {
        it.scalaInit(command)
        header = it.getHeader
      } else {
        it.createPipestep(command, leftHeader)
        header = it.getHeader
      }
    } finally {
      it.close()
    }
    header
  }

  def parseCommand(command: String, argString: String, i: Int, pipeSteps: Array[String], firstCmd: Int,
                   inputSource: GenomicIterator, executeNor: Boolean, cacheDir: String,
                   whiteListCmdSet: java.util.Map[String, CmdParams]): (Analysis, Int, GenomicIterator) = {

    // Handle if there is an empty command
    if (command.trim.isEmpty) {
      val e = new GorParsingException("Empty command found! Could be an instance of two pipes with nothing in between (e.g. ... | | ...). Query: " + argString)
      e.setCommandIndex(i + 1)
      throw e
    }

    val paramString = pipeSteps(i).slice(command.length, pipeSteps(i).length).trim
    var firstCommand = firstCmd
    var newInputSource: GenomicIterator = inputSource
    var aPipeStep: Analysis = null
    var pipeStepFound = false

    try {
      val commandInfo = GorPipeCommands.getInfo(command)

      if (commandInfo != null) {
        // test if we are executing a nor supported command in nor context
        if (executeNor) {
          if (!commandInfo.commandOptions.norCommand) {
            throw new GorParsingException(s"Error in nor query - trying to execute $command in a nor query which is not supported: ")
          }
        } else {
          if (!commandInfo.commandOptions.gorCommand) {
            throw new GorParsingException(s"Error in gor query - trying to execute $command in a gor query which is not supported: ")
          }
        }

        val commandArgs = CommandParseUtilities.quoteSafeSplit(paramString, ' ')
        if (!commandInfo.isPlaceholder) {

          //If the parsing needs access to the current
          val commandRuntime = CommandRuntime(thePipeStep, cacheDir, inputSource)

          val args = expandGetValue(commandArgs)
          val result = commandInfo.init(context, executeNor, combinedHeader, argString, args, commandRuntime)
          aPipeStep = result.step

          if (commandInfo.commandOptions.cancelCommand && context.getSession.getSystemContext.getMonitor != null) {
            aPipeStep = CancelMonitor(context.getSession.getSystemContext.getMonitor) | aPipeStep
          }

          if (commandInfo.commandOptions.verifyCommand && !result.excludeValidation) {
            aPipeStep = CheckOrder(command) | aPipeStep
          }

          if (MemoryMonitorUtil.memoryMonitorActive && commandInfo.commandOptions.memoryMonitorCommand) {
            aPipeStep = MemoryMonitor(command) | aPipeStep
          }

          if (result.header != null && result.header != "") {
            combinedHeader = result.header
          }

          if (result.usedFiles != null) {
            usedFiles :::= result.usedFiles.toList
          }

          if (result.newInputSource != null) {
            newInputSource = result.newInputSource
            val newHeader = newInputSource.getHeader
            if (newHeader != null) combinedHeader = validHeader(newHeader)
            thePipeStep = PlaceHolder()
            firstCommand = i + 1
          }
        } else {
          commandInfo.validateArguments(commandArgs)
          val (placeholderPipeStep, placeholderHeader, placeholderInputSource, placeholderFirstCommand) = handlePlaceholderCommands(command, i, paramString,
            cacheDir, whiteListCmdSet)

          aPipeStep = placeholderPipeStep
          if (placeholderHeader != null) combinedHeader = validHeader(placeholderHeader)
          if (placeholderInputSource != null) newInputSource = placeholderInputSource
          if (placeholderFirstCommand >= 0) firstCommand = placeholderFirstCommand
        }

        pipeStepFound = true
      }

      // Here we will process external whitelisted commands
      if (!pipeStepFound) {
        val (newIterator, newHeader) = processWhitelistedCommands(command, whiteListCmdSet, paramString, inputSource, executeNor)
        combinedHeader = newHeader
        thePipeStep = PlaceHolder()
        newInputSource = newIterator
        firstCommand = i + 1
      }

    } catch {
      case gue: GorUserException =>
        inputSource.close()
        gue.setCommandName(command)
        gue.setCommandIndex(i + 1)
        gue.setCommandStep(command + " " + paramString)
        gue.setRequestID(context.getSession.getRequestId)
        gue.setQuery(StringUtil.limitSize(theParams, 1000, 0.5))
        gue.setExtraInfo(gue.getExtraInfo() + " Hostname=" + InetAddress.getLocalHost().getHostName())

        throw gue
      case ex: Throwable =>
        inputSource.close()
        throw ex
    }

    (aPipeStep, firstCommand, newInputSource)
  }

  def processWhitelistedCommands(command: String, whiteListCmdSet: util.Map[String, CmdParams], paramString: String, inputSource: GenomicIterator, executeNor: Boolean): (GenomicIterator, String) = {
    val icmd = GorJavaUtilities.getIgnoreCase(whiteListCmdSet.keySet(), command)
    if (icmd.isPresent) {
      val cmdalias = icmd.get()
      val cmdparams = whiteListCmdSet.get(cmdalias)
      val command = CommandSubstitutions.insertProjectContext(cmdparams.getCommand, paramString, context)
      val skipheader = cmdparams.skipHeader()
      val skip = cmdparams.skipLines()
      val allowerror = cmdparams.allowError()
      val server = cmdparams.useHttpServer()
      val pip: GenomicIterator = new ProcessIteratorAdaptor(context, command, cmdparams.getAliasName, inputSource, thePipeStep, combinedHeader, skipheader, skip, allowerror, executeNor)
      val newHeader = pip.getHeader
      if (newHeader != null) combinedHeader = validHeader(newHeader)
      (pip, newHeader)
    } else {
      throw new GorParsingException(s"Error in command - $command not found in white listed commands: ")
    }
  }

  def handlePlaceholderCommands(command: String, commandNumber: Int, paramString: String, cacheDir: String,
                                whiteListCmdSet: java.util.Map[String, CmdParams]): (Analysis, String, GenomicIterator, Int) = {

    var newInputSource: GenomicIterator = null
    var header: String = null
    var pipeStep: Analysis = null
    var firstCommand = -1

    // Special handling of PIPESTEPS?
    if (command.toUpperCase == "PIPESTEPS") {
      val qr = context.getSession.getSystemContext.getReportBuilder.parse(paramString)
      val nPipeSteps = CommandParseUtilities.quoteSafeSplitAndTrim(qr, '|')
      var fCmd = 0
      for (i <- nPipeSteps.indices) {
        val cmd = nPipeSteps(i).substring(0, nPipeSteps(i).indexOf(' ')).trim.toUpperCase
        val (ps, fc, isrc) = parseCommand(cmd, qr, i, nPipeSteps, fCmd, newInputSource, isNorContext, cacheDir, whiteListCmdSet)
        fCmd = fc
        newInputSource = isrc
        if (i == fCmd) {
          pipeStep = ps
        } else {
          pipeStep | ps
        }
      }
      firstCommand = commandNumber + fCmd
    } else if (command.toUpperCase == "TOGOR") {
      isNorContext = false
      pipeStep = new NorToGorPipeStep(combinedHeader)
      header = pipeStep.getHeader()
    }

    (pipeStep, header, newInputSource, firstCommand)
  }

  def firstStep : Analysis = {
    thePipeStep;
  }

  def lastStep : Analysis = {
    var step = thePipeStep
    while(step.pipeTo != null) {
      step = step.pipeTo
    }

    step
  }
}

