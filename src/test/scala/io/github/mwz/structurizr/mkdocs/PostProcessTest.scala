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
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import munit.CatsEffectSuite

class PostProcessTest extends CatsEffectSuite:
  // fresh, isolated base + docs dirs per test
  def tmpDirs = Files[IO].tempDirectory.both(Files[IO].tempDirectory)

  def writeFile(path: Path, content: String): IO[Unit] =
    IO.fromOption(path.parent)(new RuntimeException(s"No parent: $path"))
      .flatMap(Files[IO].createDirectories) *>
      Stream.emit(content).through(fs2.text.utf8.encode).through(Files[IO].writeAll(path)).compile.drain

  test("copies the listed extra_css files into the docs dir, preserving structure"):
    val css = "body { color: red; }"
    tmpDirs
      .use { (base, docs) =>
        val conf = PathConfig(base.toNioPath, docs.toNioPath)
        writeFile(base / "mkdocs.yml", "site_name: Test\nextra_css:\n  - css/extra.css\n") *>
          writeFile(base / "css" / "extra.css", css) *>
          PostProcess(conf).run() *>
          assertIO(Files[IO].readUtf8(docs / "css" / "extra.css").compile.string, css)
      }

  test("completes without error and copies nothing when an extra_css file is missing"):
    tmpDirs
      .use { (base, docs) =>
        val conf = PathConfig(base.toNioPath, docs.toNioPath)
        writeFile(base / "mkdocs.yml", "site_name: Test\nextra_css:\n  - css/missing.css\n") *>
          PostProcess(conf).run() *>
          assertIO(Files[IO].walk(docs).compile.toList, List(docs))
      }

  test("is a no-op when no extra_css key is present"):
    tmpDirs
      .use { (base, docs) =>
        val conf = PathConfig(base.toNioPath, docs.toNioPath)
        writeFile(base / "mkdocs.yml", "site_name: Test\n") *>
          PostProcess(conf).run() *>
          assertIO(Files[IO].walk(docs).compile.toList, List(docs))
      }
