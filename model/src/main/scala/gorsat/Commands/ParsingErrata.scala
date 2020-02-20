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

trait ParsingErrata {
  type U <: AnyRef
  type T <: U
  /*case class CommandArguments(options: String,
                              valueOptions: String,
                              minimumNumberOfArguments: Int = -1,
                              maximumNumberOfArguments: Int = -1,
                              ignoreIllegalArguments: Boolean = false)*/

  case class DefaultFunctor[+T](default: String, func:(Array[String],CommandArguments) => (Array[String], Array[String]))
  case class DefaultFunctorInt[+T](default: String, func:(Array[String],CommandArguments) => Int)
  case class DefaultFunctorBool[+T](default: String, func:(Array[String],CommandArguments) => Boolean)
  case class DefaultFunctorStr[+T](default: String, func:(Array[String],CommandArguments) => String)

  /*  object DefaultFunctor {
      def apply(default: String, func:(Array[String],CommandArguments) => (Array[T],Array[T])) = {
        val p = new DefaultFunctor[T](default,func)
        p
      }
    }*/

}
