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
import cats.syntax.foldable.*
import fs2.io.file.CopyFlag.ReplaceExisting
import fs2.io.file.CopyFlags
import fs2.io.file.Files

trait Assets:
  def copy(): IO[Unit]

object Assets:
  def apply(conf: AssetsPathConfig) = new Assets:
    def copy(): IO[Unit] =
      val sourceDir = conf.source.fs2Path
      val targetDir = conf.destination.fs2Path
      val copy = Files[IO]
        .walk(conf.source.fs2Path)
        .parEvalMapUnordered(4) { sourcePath =>
          val targetPath = targetDir.resolve(sourceDir.relativize(sourcePath))
          Files[IO]
            .isDirectory(sourcePath)
            .ifM(
              Files[IO].createDirectories(targetPath),
              // Ensure the target parent exists before the file is copied.
              targetPath.parent.traverse_(Files[IO].createDirectories) *>
                IO.println("Copying " + sourcePath + " to " + targetPath) *>
                Files[IO].copy(sourcePath, targetPath, CopyFlags(ReplaceExisting))
            )
        }
        .compile
        .drain
      Files[IO]
        .exists(sourceDir)
        .ifM(copy, IO.println(s"Assets directory ${sourceDir} does not exist - no files will be copied."))
