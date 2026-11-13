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

import munit.CatsEffectSuite

import java.nio.file.Path
import scala.jdk.CollectionConverters.*

class WorkspaceTest extends CatsEffectSuite:
  test("fail to read non-existing workspace file"):
    val config = PathConfig(Path.of("non-existing-path"), Path.of("non-existing-path/docs"))
    interceptIO[Exception](Workspace(config).get())

  test("read the example workspace"):
    val conf      = PathConfig(Path.of("example"), Path.of("example/workspace-docs"))
    val workspace = Workspace(conf).get()

    val expectedSystems = Set(
      "Acquirer",
      "ATM",
      "E-mail System",
      "Internet Banking System",
      "Mainframe Banking System"
    )

    val systems = workspace.map(_.getModel().getSoftwareSystems().asScala.toSet.map(_.getName()))
    assertIO(systems, expectedSystems)
