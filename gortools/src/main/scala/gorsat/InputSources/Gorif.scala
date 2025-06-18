package gorsat.InputSources

import gorsat.Commands.CommandParseUtilities.hasOption
import gorsat.Commands.{CommandArguments, CommandParseUtilities, InputSourceInfo, InputSourceParsingResult}
import gorsat.Utilities.Utilities
import org.gorpipe.gor.session.GorContext


object Gorif {
  val options: List[String] = Gor.options
  val valueOptions: List[String] = Gor.valueOptions ::: List("-dh")
}

class Gorif extends InputSourceInfo("GORIF",
  CommandArguments(Gorif.options.mkString(" "), Gorif.valueOptions.mkString(" "), 1)) {

  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String]): InputSourceParsingResult = {
    // Filter out filepath if not exists
    val nonExistentFiles = iargs.filter(arg =>
      !CommandParseUtilities.isNestedCommand(arg) &&
      !context.getSession.getProjectContext.getFileReader.existsWithMetaDataUpdate(arg)).toList
    val checkedIargs = iargs.filterNot(nonExistentFiles.contains)

    if (checkedIargs.isEmpty) {
      Utilities.handleNoValidFilePaths(args, isNor = false)
    }else {
      // remove the none existent file paths and -dh (and value) from args
      var updatedArgs = args.filterNot(nonExistentFiles.contains)
      if (hasOption(args, "-dh")){
        updatedArgs = updatedArgs.patch(args.indexOf("-dh"), Nil, 2)
      }
      new Gor().processArguments(context, argString, checkedIargs, updatedArgs)
    }
  }
}
