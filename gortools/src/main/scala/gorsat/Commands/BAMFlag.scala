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

import gorsat.Analysis.ExpandBamFlag
import gorsat.Commands.CommandParseUtilities._
import gorsat.Utilities.IteratorUtilities.validHeader
import org.gorpipe.gor.session.GorContext

class BAMFlag extends CommandInfo("BAMFLAG",
  CommandArguments("-v -h", "", 0, 0),
  CommandOptions( gorCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var flagCol = 3
    flagCol = columnFromHeader("Flag",forcedInputHeader, executeNor)

    var extraHeader = ""

    if (hasOption(args,"-v")) {
      extraHeader = "readIsPaired\tpairIsMapped\tseqIsUnmapped\tmateIsUnmapped\tqStrand\tmStrand\tfirstInPair\tsecondInPair\talignNotPrim\tfailsQC\tpcrOrOptDupl"
    } else {
      extraHeader = "rPair\tpairMapp\tsUnmapp\tmUnmapp\tqStrand\tmStrand\tfInPair\tsInPair\talNotPri\tfQC\tdupl"
    }

    var combinedHeader = validHeader(forcedInputHeader+"\t"+extraHeader)

    // TODO: Undocumented argument, fix?
    if (hasOption(args,"-h")) combinedHeader = null

    CommandParsingResult(ExpandBamFlag(flagCol), combinedHeader)
  }
}
