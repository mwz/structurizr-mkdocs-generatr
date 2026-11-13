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

import cats.effect.IO
import com.structurizr.Workspace as SWorkspace
import com.structurizr.dsl.StructurizrDslParser
import com.structurizr.view.ThemeUtils
import fs2.io.file.Files

import java.io.File

trait Workspace:
  def get(): IO[SWorkspace]

object Workspace:
  def apply(pathConfig: PathConfig): Workspace = new Workspace {
    def get(): IO[SWorkspace] =
      val workspaceDslPath = pathConfig.basePath.fs2Path / "workspace.dsl"
      Files[IO]
        .exists(workspaceDslPath)
        .ifM(IO.unit, IO.raiseError(new Exception(s"${workspaceDslPath.toString} not found"))) >>
        IO.blocking {
          val parser = StructurizrDslParser()
          parser.parse(File(workspaceDslPath.absolute.toString))
          val workspace = parser.getWorkspace()
          ThemeUtils.loadThemes(workspace)
          workspace
        }
  }
