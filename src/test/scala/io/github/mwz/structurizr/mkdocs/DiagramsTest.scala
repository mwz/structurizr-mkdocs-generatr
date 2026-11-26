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
import com.structurizr.Workspace as SWorkspace
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.PosixPermission.{OwnerExecute, OwnerRead}
import fs2.io.file.PosixPermissions
import munit.CatsEffectSuite

import java.io.IOException
import scala.concurrent.duration.*

import cli.CommandLineArgs

class DiagramsTest extends CatsEffectSuite:
  private def sequenceWorkspace(keys: String*): SWorkspace =
    val workspace = new SWorkspace("Diagrams", "")
    val model = workspace.getModel()
    val relationship = model.addPerson("Customer").uses(model.addSoftwareSystem("Bank"), "Checks balance")
    val views = workspace.getViews()
    views.getConfiguration().addProperty("plantuml.sequenceDiagram", "true")
    keys.foreach { key =>
      val _ = views.createDynamicView(key).add(relationship)
    }
    workspace

  test("reuse complete diagram pairs and rebuild only stale views"):
    Files[IO].tempDirectory.use { project =>
      val config = Config(CommandLineArgs(path = project.toNioPath, docsDir = "generated"))
      val output = config.assets.destination.fs2Path
      val workspace = new SWorkspace("Diagrams", "")
      val model = workspace.getModel()
      val customer = model.addPerson("Customer")
      val bank = model.addSoftwareSystem("Bank")
      val relationship = customer.uses(bank, "Checks balance")
      val views = workspace.getViews()
      views.getConfiguration().addProperty("plantuml.sequenceDiagram", "true")
      val first = views.createDynamicView("First")
      val second = views.createDynamicView("Second")
      val _ = first.add(relationship)
      val _ = second.add(relationship)

      val firstSvg = output / "First.svg"
      val firstPng = output / "First.png"
      val secondSvg = output / "Second.svg"
      val outputs = List(firstSvg, firstPng, secondSvg, output / "Second.png")
      val untouched = 120.seconds
      val markOutputs = outputs.traverse_(path => Files[IO].setFileTimes(path, Some(untouched), None, None, true))
      val runExport = Diagrams(workspace, config).exportAll()

      for
        _            <- runExport
        _            <- markOutputs
        _            <- runExport
        _            <- outputs.traverse_(path => assertIO(Files[IO].getLastModifiedTime(path), untouched))
        _            <- IO(first.setTitle("Updated view"))
        _            <- runExport
        _            <- Files[IO].getLastModifiedTime(firstSvg).map(time => assertNotEquals(time, untouched))
        _            <- assertIO(Files[IO].getLastModifiedTime(secondSvg), untouched)
        _            <- markOutputs
        cacheRecords <- Files[IO]
          .list(project / ".generated.generatr-cache")
          .filter(_.toString.endsWith(".sha256"))
          .compile
          .toList
        _ <- IO(assertEquals(cacheRecords.size, 2))
        _ <- cacheRecords.traverse_(path => Files[IO].setFileTimes(path, Some(untouched), None, None, true))
        _ <- Files[IO].delete(firstPng)
        _ <- runExport
        _ <- assertIO(Files[IO].isRegularFile(firstPng), true)
        _ <- Files[IO].getLastModifiedTime(firstSvg).map(time => assertNotEquals(time, untouched))
        _ <- assertIO(Files[IO].getLastModifiedTime(secondSvg), untouched)
        _ <- cacheRecords.traverse_(path => assertIO(Files[IO].getLastModifiedTime(path), untouched))
        _ <- Diagrams(workspace, config.copy(forceDiagrams = true)).exportAll()
        _ <- Files[IO].getLastModifiedTime(secondSvg).map(time => assertNotEquals(time, untouched))
      yield ()
    }

  test("remove obsolete generated images and preserve modified files and static assets"):
    Files[IO].tempDirectory.use { project =>
      val config = Config(CommandLineArgs(path = project.toNioPath))
      val output = config.assets.destination.fs2Path
      val assets = config.assets.source.fs2Path
      val workspace = sequenceWorkspace("Old", "Static")
      val views = workspace.getViews()
      val runExport = Diagrams(workspace, config).exportAll()

      for
        _ <- runExport
        _ <- Stream.emit("edited").through(Files[IO].writeUtf8(output / "Old.svg")).compile.drain
        _ <- Files[IO].createDirectories(assets)
        _ <- Files[IO].copy(output / "Static.svg", assets / "Static.svg")
        _ <- Assets(config.assets).copy()
        _ <- IO {
          views.removeViewWithKey("Old")
          views.removeViewWithKey("Static")
        }
        _ <- runExport
        _ <- assertIO(Files[IO].readUtf8(output / "Old.svg").compile.string, "edited")
        _ <- assertIO(Files[IO].exists(output / "Old.png"), false)
        _ <- assertIO(Files[IO].exists(output / "Static.svg"), true)
        _ <- assertIO(Files[IO].exists(output / "Static.png"), false)
      yield ()
    }

  test("render views with inline PlantUML functions on every export"):
    Files[IO].tempDirectory.use { project =>
      val config = Config(CommandLineArgs(path = project.toNioPath))
      val output = config.assets.destination.fs2Path
      val workspace = sequenceWorkspace("Dated", "Static")
      workspace.getViews().getViewWithKey("Dated").setTitle("Generated on %date()")
      val runExport = Diagrams(workspace, config).exportAll()
      val untouched = 120.seconds
      val dated = List(output / "Dated.svg", output / "Dated.png")
      val static = output / "Static.svg"

      for
        _ <- runExport
        _ <- (static :: dated).traverse_(Files[IO].setFileTimes(_, Some(untouched), None, None, true))
        _ <- runExport
        _ <- dated.traverse_(path => Files[IO].getLastModifiedTime(path).map(time => assertNotEquals(time, untouched)))
        _ <- assertIO(Files[IO].getLastModifiedTime(static), untouched)
      yield ()
    }

  test("prune retired views from a restored cache, including when the last view is removed"):
    Files[IO].tempDirectory.use { project =>
      val config = Config(CommandLineArgs(path = project.toNioPath))
      val cache = project / ".docs.generatr-cache"
      val other = cache / "notes.txt"
      val entries = Files[IO].list(cache).compile.toList.map(_.toSet)
      def exportViews(keys: String*) = Diagrams(sequenceWorkspace(keys*), config).exportAll()

      for
        _      <- exportViews("Keep")
        keep   <- entries
        _      <- exportViews("Keep", "Old")
        before <- entries
        _      <- Files[IO].deleteRecursively(config.path.docsPath.fs2Path)
        _      <- Files[IO].delete(project / ".docs.generatr-state.json")
        _      <- Files[IO].createFile(other)
        _      <- exportViews("Keep", "Renamed")
        after  <- entries
        _      <- IO {
          assertEquals(keep.size, 3)
          assertEquals((before -- keep).size, 3)
          assertEquals(after.intersect(before), keep)
          assertEquals((after -- keep - other).size, 3)
        }
        _ <- exportViews()
        _ <- assertIO(entries, Set(other))
      yield ()
    }

  test("a state staging failure leaves published diagrams intact for the next export"):
    Files[IO].tempDirectory.use { project =>
      assume(project.toNioPath.getFileSystem.supportedFileAttributeViews().contains("posix"))
      val config = Config(CommandLineArgs(path = project.toNioPath))
      val output = config.assets.destination.fs2Path
      val images = Files[IO].list(output).map(_.fileName.toString).compile.toList.map(_.toSet)
      def exportView(key: String) = Diagrams(sequenceWorkspace(key), config).exportAll()

      for
        _           <- exportView("Original")
        permissions <- Files[IO].getPosixPermissions(project)
        _           <- Files[IO]
          .setPosixPermissions(project, PosixPermissions(OwnerRead, OwnerExecute))
          .bracket { _ =>
            for
              writable <- Files[IO].isWritable(project)
              _        <- IO(assume(!writable, "The test needs a user subject to directory permissions"))
              _        <- interceptIO[IOException](exportView("Unpublished"))
              _        <- assertIO(images, Set("Original.svg", "Original.png"))
            yield ()
          }(_ => Files[IO].setPosixPermissions(project, permissions))
        _ <- exportView("Renamed")
        _ <- assertIO(images, Set("Renamed.svg", "Renamed.png"))
      yield ()
    }

  test("check every image destination before publishing any diagram"):
    Files[IO].tempDirectory.use { project =>
      val config = Config(CommandLineArgs(path = project.toNioPath))
      val output = config.assets.destination.fs2Path
      val blocked = output / "Blocked.png"
      val runExport = Diagrams(sequenceWorkspace("First", "Blocked"), config).exportAll()

      for
        _     <- Files[IO].createDirectories(blocked)
        error <- interceptIO[IOException](runExport)
        _     <- IO(assert(error.getMessage.contains(s"$blocked because it is a directory")))
        _     <- assertIO(Files[IO].list(output).compile.toList, List(blocked))
        _     <- assertIO(Files[IO].exists(project / ".docs.generatr-state.json"), false)
        _     <- Files[IO].delete(blocked)
        _     <- runExport
        _     <- assertIO(Files[IO].isRegularFile(blocked), true)
      yield ()
    }

  test("reject invalid diagram state before publishing or removing images"):
    Files[IO].tempDirectory.use { project =>
      val config = Config(CommandLineArgs(path = project.toNioPath))
      val output = config.assets.destination.fs2Path
      val state = project / ".docs.generatr-state.json"
      val runExport = Diagrams(sequenceWorkspace("Renamed"), config).exportAll()
      val invalidStates = List("{", """{"version":2,"files":{}}""", """{"version":1,"files":{"First.svg":"bad"}}""")

      Diagrams(sequenceWorkspace("First"), config).exportAll() *> invalidStates.traverse_ { contents =>
        for
          _     <- Stream.emit(contents).through(Files[IO].writeUtf8(state)).compile.drain
          error <- interceptIO[IOException](runExport)
          _     <- IO(assert(error.getMessage.contains(s"Invalid diagram state file $state")))
          _     <- assertIO(Files[IO].readUtf8(state).compile.string, contents)
          _     <- assertIO(Files[IO].isRegularFile(output / "First.svg"), true)
          _     <- assertIO(Files[IO].isRegularFile(output / "First.png"), true)
          _     <- assertIO(Files[IO].exists(output / "Renamed.svg"), false)
          _     <- assertIO(Files[IO].exists(output / "Renamed.png"), false)
        yield ()
      }
    }
