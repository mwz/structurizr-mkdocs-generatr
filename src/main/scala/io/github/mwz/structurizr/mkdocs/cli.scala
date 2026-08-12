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

import caseapp.*

import java.nio.file.Path

object cli:
  case class CommandLineArgs(
      @HelpMessage(
        "Path to the root of the project containing the Structurizr workspace.dsl and the mkdocs.tmpl.yml files. (defaults to './')"
      )
      path: Path = Path.of("./"),
      @HelpMessage(
        "The directory where the static files are located, relative to the 'path' argument. (defaults to 'img')"
      )
      assetsDir: String = "img",
      @HelpMessage(
        "The directory where the documentation markdown files will be generated, relative to the 'path' argument. Needs to match MkDocs' 'docs_dir' setting. (defaults to 'docs')"
      )
      docsDir: String = "docs"
  )
