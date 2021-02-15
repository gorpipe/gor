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

/**
  * Contains the final command to be executed within an execution batch. Here the signature has been calculated and the
  * query is ready for the query handler.
  *
  * @param signature        Query signature based on query and external files
  * @param query            Query to be executed in the query handler
  * @param batchGroupName   Name of the current batch group
  * @param createName       Name of the create statement, mainly used for logging.
  * @param cacheFile        Optional cacheFile name
  */
case class ExecutionCommand(signature: String, query: String, batchGroupName: String, createName: String, cacheFile: String)
