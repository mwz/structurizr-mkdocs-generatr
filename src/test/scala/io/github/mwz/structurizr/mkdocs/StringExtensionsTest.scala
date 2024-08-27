package io.github.mwz.structurizr.mkdocs

import munit.FunSuite

class StringExtensionsTest extends FunSuite:
  test("toOption should return Some when the string is not empty"):
    val s      = "Hello, World!"
    val result = s.toOption
    assertEquals(result, Some(s))

  test("toOption should return None when the string is empty"):
    val s      = ""
    val result = s.toOption
    assertEquals(result, None)

  test("toOption should return None when the string is blank"):
    val s      = "   "
    val result = s.toOption
    assertEquals(result, None)
