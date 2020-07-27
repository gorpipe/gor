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

package gorsat.parser

import scala.collection.mutable

/**
 * A FunctionRegistry is used to hold all the functions that can be used in an expression
 * parsed by ParseArith.
 * <br><br>
 * The functions are registered under a case sensitive name - it is up to the parser
 * to provide case insensitivity if that is desired.
 * Essentially, any function can be registered with a simple register call, but
 * at the time of writing only functions with 0 up to 4 arguments are supported.
 * If more arguments are required at some point in the future, new variants
 * of the register function need to be added.
 * <br><br>
 * Note that ParseArith expects functions to have a ParseArith reference as the first
 * argument. Functions registered here can leave that out if they don't use it - the
 * registry will create a wrapper that provides this argument and ignores it.
 * <br><br>
 * To register a function, call <br>`register("NAME", signature, func _)`<br><br>
 * It is vital that the signature matches the function - a simple way to ensure that
 * is to use the getSignature helpers from FunctionSignature. There are variants
 * of getSignature for every possible combination of parameters recognized by the
 * parser - using those to generate the signature ensures that it matches the function.
 */
class FunctionRegistry {
  // Map of all function names, mapping the base name to a list of types
  private val allFunctions = mutable.Map[String, List[FunctionWrapper]]()

  def register[R](name: String, signature: String, f: () => R): Unit = {
    def helper(owner: ParseArith): R = {
      f()
    }
    val wrapper = FunctionWrapper(name, signature, helper _)
    addFunction(name, wrapper)
  }

  def registerWithOwner[R](name: String, signature: String, f: ParseArith => R): Unit = {
    val wrapper = FunctionWrapper(name, signature, f)
    addFunction(name, wrapper)
  }

  def register[A1, R](name: String, signature: String, f: A1 => R): Unit = {
    def helper(owner: ParseArith, ex1: A1): R = {
      f(ex1)
    }
    val wrapper = FunctionWrapper(name, signature, helper _)
    addFunction(name, wrapper)
  }

  def registerWithOwner[A1, R](name: String, signature: String, f: (ParseArith, A1) => R): Unit = {
    val wrapper = FunctionWrapper(name, signature, f)
    addFunction(name, wrapper)
  }

  def register[A1, A2, R](name: String, signature: String, f: (A1, A2) => R): Unit = {
    def helper(owner: ParseArith, ex1: A1, ex2: A2): R = {
      f(ex1, ex2)
    }
    val wrapper = FunctionWrapper(name, signature, helper _)
    addFunction(name, wrapper)
  }

  def registerWithOwner[A1, A2, R](name: String, signature: String, f: (ParseArith, A1, A2) => R): Unit = {
    val wrapper = FunctionWrapper(name, signature, f)
    addFunction(name, wrapper)
  }

  def register[A1, A2, A3, R](name: String, signature: String, f: (A1, A2, A3) => R): Unit = {
    def helper(owner: ParseArith, ex1: A1, ex2: A2, ex3: A3): R = {
      f(ex1, ex2, ex3)
    }
    val wrapper = FunctionWrapper(name, signature, helper _)
    addFunction(name, wrapper)
  }

  def registerWithOwner[A1, A2, A3, R](name: String, signature: String, f: (ParseArith, A1, A2, A3) => R): Unit = {
    val wrapper = FunctionWrapper(name, signature, f)
    addFunction(name, wrapper)
  }

  def register[A1, A2, A3, A4, R](name: String, signature: String, f: (A1, A2, A3, A4) => R): Unit = {
    def helper(owner: ParseArith, ex1: A1, ex2: A2, ex3: A3, ex4: A4): R = {
      f(ex1, ex2, ex3, ex4)
    }
    val wrapper = FunctionWrapper(name, signature, helper _)
    addFunction(name, wrapper)
  }

  def registerWithOwner[A1, A2, A3, A4, R](name: String, signature: String, f: (ParseArith, A1, A2, A3, A4) => R): Unit = {
    val wrapper = FunctionWrapper(name, signature, f)
    addFunction(name, wrapper)
  }

  def registerWithOwner[A1, A2, A3, A4, A5, A6, A7, R](name: String, signature: String, f: (ParseArith, A1, A2, A3, A4, A5, A6, A7) => R): Unit = {
    val wrapper = FunctionWrapper(name, signature, f)
    addFunction(name, wrapper)
  }

  def registerWithOwner[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R](name: String, signature: String, f: (ParseArith, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) => R): Unit = {
    val wrapper = FunctionWrapper(name, signature, f)
    addFunction(name, wrapper)
  }



  /**
   * Look up a function wrapper based on the mangled name of a function. The mangled
   * name has the signature of its argument types and return type appended to the
   * function name.
   * @param mangledName The mangled function name
   * @return A function wrapper for the function
   */
  def lookupWrapper(mangledName: String): FunctionWrapper = {
    val baseName = mangledName.reverse.split("_", 2)(1).reverse
    val variants = allFunctions(baseName)
    variants.filter(p => p.mangledName == mangledName).head
  }

  /**
   * Gets a list of the signature variants available for a function with the given name,
   * or an empty list if no such function exists.
   * @param name The name of the function
   * @return A list (potentially empty) of strings, representing the function signatures
   */
  def getVariants(name: String): List[String] = {
    allFunctions.getOrElse(name, List.empty).map(wrapper => wrapper.signature)
  }

  /**
   * Gets a list of the signature variants available for a function with the given name,
   * with the given return type or an empty list if no such function exists.
   * <br><br>
   * This function is similar to `getVariants`, except the list is filtered
   * by the return type.
   * @param name The name of the function
   * @param returnType The string representation of the desired return type. See the constants
   *                   in FunctionTypes.
   * @return A list (potentially empty) of strings, representing the function signatures
   */
  def getVariantsByReturnType(name: String, returnType: String): List[String] = {
    val all = getVariants(name)
    all.filter(x => x.split(FunctionTypes.ReturnSeparator)(1) == returnType)
  }

  private def addFunction(name: String, wrapper: FunctionWrapper): Unit = {
    val existingVariants = allFunctions.getOrElse(name, List[FunctionWrapper]())
    allFunctions(name) = wrapper :: existingVariants
  }
}

object FunctionRegistry {
  def apply(): FunctionRegistry = new FunctionRegistry()
}
