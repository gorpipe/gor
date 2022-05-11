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

package gorsat.Script

import gorsat.gorsatGorIterator.MapAndListUtilities.singleHashMap


/**
  * Result from performin a macro expansion. The expansion can return any number of new execution blocks which are added to
  * the list of active execution blocks. All virtual files resulting from the macro expansion are returned and any aliases
  * if applicable.
  *
  * @param createCommands   Map of new execution blocks
  * @param virtualFiles     New virtual files resulting from macro expansion
  * @param aliases          New aliases resulting from macro expansion
  */
case class MacroParsingResult(createCommands: java.util.LinkedHashMap[String, ExecutionBlock],
                              virtualFiles: Map[String, String],
                              aliases: singleHashMap = null)
