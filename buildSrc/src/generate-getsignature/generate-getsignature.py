import argparse as argparse
import itertools
import sys


fun_to_base = {
    "iFun": "Int",
    "lFun": "Long",
    "dFun": "Double",
    "sFun": "String",
    "bFun": "Boolean"
}

fun_to_typename = {
    "iFun": "IntFun",
    "lFun": "LongFun",
    "dFun": "DoubleFun",
    "sFun": "StringFun",
    "bFun": "BooleanFun"
}

def get_function_name(arg_types, return_type):
    base_types = [fun_to_base[x] for x in arg_types]
    prefix = "".join(base_types)
    if prefix == "":
        prefix = "Empty"
    function_name = "getSignature{0}2{1}".format(prefix, fun_to_base[return_type])
    return function_name


def generate_function(arg_types, return_type):
    function_name = get_function_name(arg_types, return_type)
    signature = "({0}) => {1}".format(", ".join(arg_types), return_type)
    typenames = [fun_to_typename[x] for x in arg_types]
    function = "  def %s(f: %s): String = getSignature(List(%s), %s)" % \
                         (function_name, signature, ", ".join(typenames), fun_to_typename[return_type])
    return function


def generate_functions_for_return_type(output, num_args, return_type):
    for each in itertools.product(fun_to_typename.keys(), repeat=num_args):
        output.write(generate_function(each, return_type))
        output.write("\n")


def generate_FunctionSignature(output):
    fileheader = """
// This file is generated from a Python script - do not modify!!!
package gorsat.parser

import gorsat.parser.FunctionTypes._
    
"""
    objectheader = """
object FunctionSignature {
    
"""
    predefined = """
  // The following are helper functions for getting the signature of a Calc function that
  // requires an owner, that is a ParseArith instance. The signature as far as the parser is
  // concerned is the same as if there is no owner argument. Note that the function returned
  // is a dummy - don't use it for anything but passing to the appropriate getSignature
  // function. Note also that you must use registerWithOwner when registering.
  
  def removeOwner[R](f: (ParseArith) => R): () => R = {
    def dummy(): R = f(null)
    dummy _
  }

  def removeOwner[A1, R](f: (ParseArith, A1) => R): (A1) => R = {
    def dummy(ex1: A1): R = f(null, ex1)
    dummy
  }

  def removeOwner[A1, A2, R](f: (ParseArith, A1, A2) => R): (A1, A2) => R = {
    def dummy(ex1: A1, ex2: A2): R = f(null, ex1, ex2)
    dummy
  }

  def removeOwner[A1, A2, A3, R](f: (ParseArith, A1, A2, A3) => R): (A1, A2, A3) => R = {
    def dummy(ex1: A1, ex2: A2, ex3: A3): R = f(null, ex1, ex2, ex3)
    dummy
  }

  def removeOwner[A1, A2, A3, A4, R](f: (ParseArith, A1, A2, A3, A4) => R): (A1, A2, A3, A4) => R = {
    def dummy(ex1: A1, ex2: A2, ex3: A3, ex4: A4): R = f(null, ex1, ex2, ex3, ex4)
    dummy
  }

  def getSignatureStringStringList2Boolean(f: (sFun, List[String]) => bFun) = getSignature(List(StringFun, StringList), BooleanFun)
  def getSignatureStringStringList2Int(f: (sFun, List[String]) => iFun) = getSignature(List(StringFun, StringList), IntFun)
    
"""
    output.write(fileheader)
    output.write(objectheader)
    output.write(predefined)
    for i in range(0, 5):
        for each in fun_to_typename.keys():
            generate_functions_for_return_type(output, i, each)
    output.write("}\n")


def generate_test_function(arg_types, return_type):
    function_name = get_function_name(arg_types, return_type)
    helper_name = function_name.replace("getSignature", "helper")
    signature_items = []
    for i, item in enumerate(arg_types):
        signature_items.append("a%d: %s" % (i+1, item)
                               )
    base_types = [fun_to_base[x] for x in arg_types]
    if len(base_types) == 0:
        base_types = ["e"]
        helper_name = "() => %s()" % helper_name
    expected = ":".join(base_types) + "2" + fun_to_base[return_type]
    assert_line = "FunctionSignature.%s(%s) == \"%s\"" % (function_name, helper_name, expected)
    function = "    result &= %s\n" % assert_line
    return function


def generate_test_functions_for_return_type(output, num_args, return_type):
    for each in itertools.product(fun_to_typename.keys(), repeat=num_args):
        output.write(generate_test_function(each, return_type))


def generate_test_helper(arg_types, return_type):
    function_name = get_function_name(arg_types, return_type)
    helper_name = function_name.replace("getSignature", "helper")
    signature_items = []
    for i, item in enumerate(arg_types):
        signature_items.append("a%d: %s" % (i+1, item))
    signature = ", ".join(signature_items)
    helper_line = "def %s(%s): %s = %sDummy" % (helper_name, signature, return_type, return_type)
    function = "  %s\n" % helper_line
    return function


def generate_test_helpers_for_return_type(output, num_args, return_type):
    for each in itertools.product(fun_to_typename.keys(), repeat=num_args):
        output.write(generate_test_helper(each, return_type))


def generate_UTestFunctionSignature(output):
    fileheader = """
// This file is generated from a Python script - do not modify!!!
package gorsat.parser

import org.gorpipe.gor.ColumnValueProvider
import gorsat.parser.FunctionTypes.{dFun, iFun, lFun, sFun, bFun}
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

    """
    objectheader = """
@RunWith(classOf[JUnitRunner])
class UTestFunctionSignature extends FunSuite {
    """
    predefined = """
  private def iFunDummy: iFun = (cvp: ColumnValueProvider) => 42
  private def lFunDummy: lFun = (cvp: ColumnValueProvider) => 1
  private def dFunDummy: dFun = (cvp: ColumnValueProvider) => 3.14
  private def sFunDummy: sFun = (cvp: ColumnValueProvider) => "bingo"
  private def bFunDummy: bFun = (cvp: ColumnValueProvider) => false

"""
    output.write(fileheader)
    output.write(objectheader)
    output.write(predefined)

    for i in range(0, 5):
        for each in fun_to_typename.keys():
            generate_test_helpers_for_return_type(output, i, each)
            output.write("  test(\"getSignature_%s_%d\") {\n" % (each, i+1))
            output.write("    var result = true\n")
            generate_test_functions_for_return_type(output, i, each)
            output.write("    assert(result)\n")
            output.write("  }\n")

    output.write("}\n")


def run():
    parser = argparse.ArgumentParser()
    parser.add_argument('filename')
    parser.add_argument('test')

    args = parser.parse_args()

    with open(args.filename, 'w') as f:
        generate_FunctionSignature(f)

    with open(args.test, 'w') as f:
        generate_UTestFunctionSignature(f)


if __name__ == "__main__":
    sys.exit(run())
