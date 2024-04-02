package io.github.mwz.structurizr.mkdocs

import munit.FunSuite

class MarkdownTest extends FunSuite {
  test("escape should not change the string if no escapable characters are present") {
    val input          = "This is a test"
    val expectedOutput = "This is a test"
    val actualOutput   = markdown.escape(input)
    assertEquals(actualOutput, expectedOutput)
  }

  test("escape should escape multiple characters in a string") {
    val input =
      """Multiple characters: \ ` * _ { } [ ] < > ( ) # + - . ! |
         /another line: \ ` * _ { } [ ] < > ( ) # + - . ! |"""
        .stripMargin('/')
    val expectedOutput =
      """Multiple characters: \\ \` \* \_ \{ \} \[ \] \< \> \( \) \# \+ \- \. \! \|
         /another line: \\ \` \* \_ \{ \} \[ \] \< \> \( \) \# \+ \- \. \! \|"""
        .stripMargin('/')
    val actualOutput = markdown.escape(input)
    assertEquals(actualOutput, expectedOutput)
  }
}
