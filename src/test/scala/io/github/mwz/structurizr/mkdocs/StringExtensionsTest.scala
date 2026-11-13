/*
 * Copyright 2024-2026 Michael Wizner and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
