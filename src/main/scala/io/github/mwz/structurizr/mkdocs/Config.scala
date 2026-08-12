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

import fs2.io.file.Path as FS2Path

import java.nio.file.Path

import cli.CommandLineArgs

case class Config(path: PathConfig, assets: AssetsPathConfig)
case class PathConfig(basePath: Path, docsPath: Path)
case class AssetsPathConfig(source: Path, destination: Path)

extension (pathConfig: PathConfig)
  def relativeDocsPath: Path =
    pathConfig.basePath.relativize(pathConfig.docsPath)

object Config:
  def apply(options: CommandLineArgs): Config =
    val docsPath = options.path.resolve(options.docsDir)
    val assets = AssetsPathConfig(
      source = options.path.resolve(options.assetsDir),
      destination = docsPath.resolve("img")
    )
    Config(PathConfig(options.path, docsPath), assets)

extension (path: Path) def fs2Path: FS2Path = FS2Path.fromNioPath(path)
