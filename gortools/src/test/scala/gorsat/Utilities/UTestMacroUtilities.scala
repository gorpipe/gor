/*
 *  Copyright (C) 2018 WuXi NextCode Inc.
 *  Copyright (C) 2024 GeneDx, LLC
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

package gorsat.Utilities

import gorsat.Commands.CommandParseUtilities
import gorsat.Utilities.MacroUtilities.applyAliases
import org.gorpipe.exceptions.GorParsingException
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.concurrent.{Signaler, ThreadSignaler, TimeLimits}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.time.{Millis, Second, Span}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestMacroUtilities extends AnyFunSuite with TimeLimits {

  /* Note on time-limited tests
   * Some potential failures (and fixed bugs) are infinite loop situations, so corresponding tests are time-boxed.
   * failAfter(...)
   *   defines the outcome as failure if time is in excess, but does not ensure the test will
   *   exit promptly (or ever).
   * implicit val signaler: Signaler = ThreadSignaler
   *   requests that the test be interrupted after the limit expires, but may depend on the code under test to
   *   sleep or yield for the interrupt to take effect and the test to terminate
   */


  /* timeout during testing is generous compared with target time, because test concurrency might run slower */
  private def testTimeLimit(): Span = {
    return Span(5000, Millis)
  }

  /** helper to split (possibly multi-line) query for processing in tested functions
   * The tested functions do not expect newline characters, but we prefer to formulate
   * test queries with newlines for readability
   */
  private def getCommandsFrom(query_with_macros: String) = {
    val fixedQuery = query_with_macros.replace("\n", " ")
    val argString = CommandParseUtilities.quoteSafeSplitAndTrim(fixedQuery, ' ').mkString(" ")
    val gorCommands = CommandParseUtilities.quoteSafeSplitAndTrim(argString, ';')
    gorCommands
  }

  /**
   * test that fairly complex use of macros works now (problem of ENGKNOW-1482)
   */
  test("extractAndApplyAliases_multiMacrosAndParams") {

    val query_with_macros =
      """
        ^    def #log# = user_data/the_user/log2.tsv;
        ^    def #rectmp# = user_data/the_user/YML/file_log_record.yml;
        ^    def #note# = 'xxx';
        ^    def #add_rec_wait#($1,$2) = merge <(#rectmp#(file='$1',ac='update',note=#note#,w=$2) );
        ^    def #file1# = user_data/the_user/file1b.gorz;
        ^    def #file2# = user_data/the_user/file2.gorz;
        ^    create #a_result# = GORROWS -p chr1:0-10 | calc a #note#;
        ^    create #result2# = GORROWS -p chr1:0-10 | calc a '#log#';
        ^    create #add# = norrows 0
        ^      | #add_rec_wait#(#file1#,[#a_result#])
        ^      | #add_rec_wait#(#file2#,[#result2#])
        ^      | #add_rec_wait#(#file1#,[#a_result#])
        ^""".stripMargin('^')
    val gorCommands: Array[String] = getCommandsFrom(query_with_macros)
    assert(gorCommands.length == 9)
    val aliases = MacroUtilities.extractAliases(gorCommands)
    assert(aliases.size() == 6)

    var postCommands = applyAliases(gorCommands, aliases)
    assert(postCommands.length == 3)
    Assert.assertEquals("create #a_result# = GORROWS -p chr1:0-10 | calc a 'xxx'".trim, postCommands(0).trim)
    Assert.assertEquals("create #result2# = GORROWS -p chr1:0-10 | calc a 'user_data/the_user/log2.tsv'".trim, postCommands(1).trim)
    var resultQuery =
      """
        ^create #add# = norrows 0
        ^      | merge <(user_data/the_user/YML/file_log_record.yml(file='user_data/the_user/file1b.gorz',ac='update',note='xxx',w=[#a_result#]) )
        ^      | merge <(user_data/the_user/YML/file_log_record.yml(file='user_data/the_user/file2.gorz',ac='update',note='xxx',w=[#result2#]) )
        ^      | merge <(user_data/the_user/YML/file_log_record.yml(file='user_data/the_user/file1b.gorz',ac='update',note='xxx',w=[#a_result#]) )
        ^""".stripMargin('^')
    var result2 = getCommandsFrom(resultQuery)(0)
    Assert.assertEquals(result2.trim, postCommands(2).trim)
  }

  /**
   * test as documentation of possibly unexpected behavior:
   * a macro can have a name with spaces and (some) punctuation characters, such as
   * "add rec & wait"
   */
  test("extractAndApplyAliases_macro with punctuation and space") {

    val query_with_macros =
      """
        ^    def #log# = user_data/the_user/log2.tsv;
        ^    def #rectmp# = user_data/the_user/YML/file_log_record.yml;
        ^    def #note# = 'xxx';
        ^    def add rec & wait($1,$2) = merge <(#rectmp#(file='$1',ac='update',note=#note#,w=$2) );
        ^    def #file1# = user_data/the_user/file1b.gorz;
        ^    def #file2# = user_data/the_user/file2.gorz;
        ^    create #a_result# = GORROWS -p chr1:0-10 | calc a #note#;
        ^    create #result2# = GORROWS -p chr1:0-10 | calc a '#log#';
        ^    create #add# = norrows 0
        ^      | add rec & wait(#file1#,[#a_result#])
        ^      | add rec & wait(#file2#,[#result2#])
        ^      | add rec & wait(#file1#,[#a_result#])
        ^""".stripMargin('^')
    val gorCommands: Array[String] = getCommandsFrom(query_with_macros)
    assert(gorCommands.length == 9)
    val aliases = MacroUtilities.extractAliases(gorCommands)
    assert(aliases.size() == 6)

    var postCommands = applyAliases(gorCommands, aliases)
    assert(postCommands.length == 3)
    Assert.assertEquals("create #a_result# = GORROWS -p chr1:0-10 | calc a 'xxx'".trim, postCommands(0).trim)
    Assert.assertEquals("create #result2# = GORROWS -p chr1:0-10 | calc a 'user_data/the_user/log2.tsv'".trim, postCommands(1).trim)
    var resultQuery =
      """
        ^create #add# = norrows 0
        ^      | merge <(user_data/the_user/YML/file_log_record.yml(file='user_data/the_user/file1b.gorz',ac='update',note='xxx',w=[#a_result#]) )
        ^      | merge <(user_data/the_user/YML/file_log_record.yml(file='user_data/the_user/file2.gorz',ac='update',note='xxx',w=[#result2#]) )
        ^      | merge <(user_data/the_user/YML/file_log_record.yml(file='user_data/the_user/file1b.gorz',ac='update',note='xxx',w=[#a_result#]) )
        ^""".stripMargin('^')
    var result2 = getCommandsFrom(resultQuery)(0)
    Assert.assertEquals(result2.trim, postCommands(2).trim)
  }

  /**
   * test as documentation of possibly unexpected behavior:
   * a macro can have a name that looks like a numeric constant, which could get a user into trouble
   */
  test("extractAndApplyAliases_macro with digits") {

    val query_with_macros =
      """
        ^    def 3.14 = 2.71828;
        ^    create #add# = norrows 0 | calc not_pi 3.14
        ^""".stripMargin('^')
    val gorCommands: Array[String] = getCommandsFrom(query_with_macros)
    assert(gorCommands.length == 2)
    val aliases = MacroUtilities.extractAliases(gorCommands)
    assert(aliases.size() == 1)

    var postCommands = applyAliases(gorCommands, aliases)
    assert(postCommands.length == 1)
    Assert.assertEquals("create #add# = norrows 0 | calc not_pi 2.71828".trim, postCommands(0).trim)
  }

  /** test that case is ignored in macro names */
  test("extractAndApplyAliases_caseInsensitive") {

    val query_with_macros =
      """
        ^    def #Log# = user_data/the_user/log2.tsv;
        ^    def #recTmp# = user_data/the_user/YML/file_log_record.yml;
        ^    def #NOTE# = 'xxx';
        ^    def #add_rec_WAIT#($1,$2) = merge <(#RECTMP#(file='$1',ac='update',note=#note#,w=$2) );
        ^    def #file1# = user_data/the_user/file1b.gorz;
        ^    def #file2# = user_data/the_user/file2.gorz;
        ^    create #a_result# = GORROWS -p chr1:0-10 | calc a #note#;
        ^    create #result2# = GORROWS -p chr1:0-10 | calc a '#LOG#';
        ^    create #add# = norrows 0
        ^      | #add_rec_wait#(#file1#,[#a_result#])
        ^      | #add_rec_wait#(#FILE2#,[#result2#])
        ^      | #add_rec_wait#(#file1#,[#a_result#])
        ^""".stripMargin('^')
    val gorCommands: Array[String] = getCommandsFrom(query_with_macros)
    assert(gorCommands.length == 9)
    val aliases = MacroUtilities.extractAliases(gorCommands)
    assert(aliases.size() == 6)

    var postCommands = applyAliases(gorCommands, aliases)
    assert(postCommands.length == 3)
    Assert.assertEquals("create #a_result# = GORROWS -p chr1:0-10 | calc a 'xxx'".trim, postCommands(0).trim)
    Assert.assertEquals("create #result2# = GORROWS -p chr1:0-10 | calc a 'user_data/the_user/log2.tsv'".trim, postCommands(1).trim)
    var resultQuery =
      """
        ^create #add# = norrows 0
        ^      | merge <(user_data/the_user/YML/file_log_record.yml(file='user_data/the_user/file1b.gorz',ac='update',note='xxx',w=[#a_result#]) )
        ^      | merge <(user_data/the_user/YML/file_log_record.yml(file='user_data/the_user/file2.gorz',ac='update',note='xxx',w=[#result2#]) )
        ^      | merge <(user_data/the_user/YML/file_log_record.yml(file='user_data/the_user/file1b.gorz',ac='update',note='xxx',w=[#a_result#]) )
        ^""".stripMargin('^')
    var result2 = getCommandsFrom(resultQuery)(0)
    Assert.assertEquals(result2.trim, postCommands(2).trim)
  }

  /** test several properties of macro replacement
   * - symbol names without # demarcation work, as values for whole tokens
   * - symbol names without # demarcation do not get substituted in strings or partial words
   * - for the same symbol defined more than once, the last appearance is effective
   * - different capitalizations of the same symbol mean the same thing
   */
  test("extractAndApplyAliases_redefineCaseInsensitive") {

    val query_with_macros =
      """
        ^    def #addanswer# = | calc answer yesorno ;
        ^    def yesorno = 'no' ;
        ^    def YesOrNo = 'yes' ;
        ^    def #norvtable# = nor [#vtable#];
        ^    #norvtable# #addanswer# | calc yesorno_question 'yesorno ?'
        ^""".stripMargin('^')
    val gorCommands: Array[String] = getCommandsFrom(query_with_macros)
    Assert.assertEquals(5, gorCommands.length)
    val aliases = MacroUtilities.extractAliases(gorCommands)
    Assert.assertEquals(4, aliases.size())

    var postCommands = applyAliases(gorCommands, aliases)
    assert(postCommands.length == 1)
    Assert.assertEquals("nor [#vtable#] | calc answer 'yes' | calc yesorno_question 'yesorno ?'".trim, postCommands(0).trim)
  }

  /**
   * test that macro with name #...# is substituted in path but the name without is not
   */
  test("extractAndApplyAliases_hashAndNonHashNames") {

    val query_with_macros =
      """
        ^    def #YML# = my_yml;
        ^    def the_user = some_other_user;
        ^    nor user_data/the_user/#YML#/file_log_record.yml;
        ^""".stripMargin('^')
    val gorCommands: Array[String] = getCommandsFrom(query_with_macros)
    assert(gorCommands.length == 3)
    val aliases = MacroUtilities.extractAliases(gorCommands)
    assert(aliases.size() == 2)

    var postCommands = applyAliases(gorCommands, aliases)
    Assert.assertEquals(1, postCommands.length)
    Assert.assertEquals("nor user_data/the_user/my_yml/file_log_record.yml".trim, postCommands(0).trim)
  }

  /** test that regex metacharacters in replacement content survive */
  test("extractAndApplyAliases_macrosWithRegexChars") {

    val query_with_macros =
      """
        ^    def #addprice# = | calc price '$5' ;
        ^    def #addset# = | calc set '{a,b?,c*}' ;
        ^    def #norvtable# = nor [#vtable#];
        ^    #norvtable# #addprice# #addset#
        ^""".stripMargin('^')
    val gorCommands: Array[String] = getCommandsFrom(query_with_macros)
    assert(gorCommands.length == 4)
    val aliases = MacroUtilities.extractAliases(gorCommands)
    assert(aliases.size() == 3)

    var postCommands = applyAliases(gorCommands, aliases)
    assert(postCommands.length == 1)
    Assert.assertEquals("nor [#vtable#] | calc price '$5' | calc set '{a,b?,c*}'".trim, postCommands(0).trim)
  }

  test("extractAndApplyAliases_simpleRecursionYieldsError") {
    /*
      A query where a macro expansion includes its own symbol is not allowed
      and should terminate with exception without long delay
     */
    val query_with_macros =
      """
      ^ def #log# = base_directory/#log#;
      ^ nor #log#
      ^""".stripMargin('^')
    val gorCommands: Array[String] = getCommandsFrom(query_with_macros)
    assert(gorCommands.length == 2)
    val aliases = MacroUtilities.extractAliases(gorCommands)
    assert(aliases.size() == 1)

    implicit val signaler: Signaler = ThreadSignaler
    failAfter(testTimeLimit()) {
      assertThrows[GorParsingException] {
        var postCommands = applyAliases(gorCommands, aliases)
      }
    }
  }

  test("accidentalRecursionYieldsError") {
    /*
        A query with a missing semicolon may yield a recursion by accident.
        Should terminate with exception without long delay
     */
    val query_with_macros =
      """
        ^    def #log# = user_data/the_user/log2.tsv;
        ^    def #rectmp# = user_data/the_user/YML/file_log_record.yml;
        ^    def #note# = 'xxx'
        ^    def #add_rec_wait#($1,$2) = merge <(#rectmp#(file='$1',ac='update',note=#note#,w=$2) );
        ^    def #file1# = user_data/the_user/file1b.gorz;
        ^    def #file2# = user_data/the_user/file2.gorz;
        ^    create #a_result# = GORROWS -p chr1:0-10 | calc a #note#;
        ^    create #result2# = GORROWS -p chr1:0-10 | calc a '#log#';
        ^    create #add# = norrows 0
        ^      | #add_rec_wait#(#file1#,[#a_result#])
        ^      | #add_rec_wait#(#file2#,[#result2#])
        ^      | #add_rec_wait#(#file1#,[#a_result#])
        ^""".stripMargin('^')
    val gorCommands: Array[String] = getCommandsFrom(query_with_macros)
    Assert.assertEquals(8, gorCommands.length)
    val aliases = MacroUtilities.extractAliases(gorCommands)
    Assert.assertEquals(5, aliases.size())

    implicit val signaler: Signaler = ThreadSignaler
    failAfter(testTimeLimit()) {
      assertThrows[GorParsingException] {
        var postCommands = applyAliases(gorCommands, aliases)
      }
    }
  }

  test("accidentalRecursionYieldsError 2") {
    /*
        A query with a missing semicolon may yield a recursion by accident.
        Should terminate with exception without long delay
     */
    val query_with_macros =
      """
        ^    def #log# = log2.tsv;
        ^    def #rectmp# = file_log_record.yml;
        ^    def #note# = 'xxx'
        ^    def #add_rec_wait#() = merge <(#rectmp#(ac='update',note=#note#,w='3') );
        ^    def #file1# = file1b.gorz;
        ^    create #add# = norrows 0
        ^    | merge <(#rectmp#(file='#file1#',ac='update',note=#note#) ) ;
        ^    norrows 1
        ^""".stripMargin('^')

    val gorCommands: Array[String] = getCommandsFrom(query_with_macros)
    Assert.assertEquals(6, gorCommands.length)
    val aliases = MacroUtilities.extractAliases(gorCommands)
    Assert.assertEquals(4, aliases.size())

    implicit val signaler: Signaler = ThreadSignaler
    failAfter(testTimeLimit()) {
      assertThrows[GorParsingException] {
        var postCommands = applyAliases(gorCommands, aliases)
      }
    }
  }

  test("circularRecursionYieldsError") {
    /*
      A query where macro a invokes b and macro b invokes a should terminate with exception quickly (<1 sec)
      This is about catching a user error quickly with a useful return
     */
    val query_with_macros =
      """
        ^ def #log# = base_directory/#log_filename#;
        ^ def #log_filename# = #log#.tsv;
        ^ nor #log#
        ^""".stripMargin('^')
    val gorCommands: Array[String] = getCommandsFrom(query_with_macros)
    assert(gorCommands.length == 3)
    val aliases = MacroUtilities.extractAliases(gorCommands)
    assert(aliases.size() == 2)

    implicit val signaler: Signaler = ThreadSignaler
    failAfter(testTimeLimit()) {
      assertThrows[GorParsingException] {
        var postCommands = applyAliases(gorCommands, aliases)
      }
    }
  }

  /**
   * Tests an unusual but legal construct where expansion of one macro yields invocation of another
   * It is not a clearly necessary feature to support this but it is existing behavior and we should
   * detect if it changes.
   * The behavior could be useful where alternative macros are to be included and a single
   * control point should select among them*/
  test("portmanteauMacroInvocation") {
    val query_with_macros =
      """
        ^ def #data_aws# = base_directory/log.tsv;
        ^ def #data_oci# = base_directory_oci/data.tsv;
        ^ def #plat# = oci;
        ^ nor #data_#plat##
        ^""".stripMargin('^')
    val gorCommands: Array[String] = getCommandsFrom(query_with_macros)
    assert(gorCommands.length == 4)
    val aliases = MacroUtilities.extractAliases(gorCommands)
    assert(aliases.size() == 3)
    var postCommands = applyAliases(gorCommands, aliases)
    assert(postCommands.length == 1)
    Assert.assertEquals("nor base_directory_oci/data.tsv", postCommands(0))
  }

  /**
   * The portmanteau expansion pattern described above can yield indirect recursion that would be hard to
   * detect with a simple check.  This is admittedly contrived */
  test("portmanteauRecursionYieldsError") {
    val query_with_macros =
      """
        ^ def #aaaa# = #b##c# #b##c# ;
        ^ def #b# = #aa;
        ^ def #c# = aa#;
        ^ #aaaa#
        ^""".stripMargin('^')
    val gorCommands: Array[String] = getCommandsFrom(query_with_macros)
    assert(gorCommands.length == 4)
    val aliases = MacroUtilities.extractAliases(gorCommands)
    assert(aliases.size() == 3)

    implicit val signaler: Signaler = ThreadSignaler
    failAfter(testTimeLimit()) {
      assertThrows[GorParsingException] {
        var postCommands = applyAliases(gorCommands, aliases)
      }
    }

  }




}
