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
import cats.effect.kernel.Resource
import cats.syntax.all.*
import com.structurizr.Workspace as SWorkspace
import com.structurizr.`export`.Diagram
import fs2.Chunk
import fs2.Stream
import fs2.hashing.HashAlgorithm.SHA256
import fs2.hashing.Hashing
import fs2.io.file.CopyFlag.AtomicMove
import fs2.io.file.CopyFlag.ReplaceExisting
import fs2.io.file.CopyFlags
import fs2.io.file.Files
import fs2.io.file.Path as FS2Path
import io.circe.Json
import io.circe.JsonObject
import io.circe.parser.parse
import io.circe.syntax.*
import net.atmp.CucaDiagram
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import net.sourceforge.plantuml.cli.ExitStatus
import net.sourceforge.plantuml.dot.GraphvizRuntimeEnvironment
import net.sourceforge.plantuml.dot.GraphvizUtils
import net.sourceforge.plantuml.version.Version

import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.NoSuchFileException
import scala.jdk.CollectionConverters.*

enum DiagramFormat(val ext: String, val fileFormat: FileFormat):
  case SVG extends DiagramFormat(".svg", FileFormat.SVG)
  case PNG extends DiagramFormat(".png", FileFormat.PNG)

trait Diagrams:
  def exportAll(): IO[Unit]

object Diagrams:
  def apply(workspace: SWorkspace, conf: Config): Diagrams = new Diagrams:
    def exportAll(): IO[Unit] =
      val dir: FS2Path = conf.assets.destination.fs2Path
      val definitions: IO[List[Diagram]] = IO.blocking {
        val exporter = CustomC4PlantUMLExporter(workspace, conf.plantumlLayout)
        exporter.addSkinParam("svgDimensionStyle", "false")
        exporter.addSkinParam("preserveAspectRatio", "meet")
        exporter.`export`(workspace).asScala.toList
      }
      val locations: IO[Option[(FS2Path, String)]] = Files[IO]
        .realPath(conf.path.docsPath.fs2Path)
        .map(docs => docs.parent.map(parent => (parent, docs.fileName.toString)))
        .recover { case _: IOException => None }
      for
        _                                             <- Files[IO].createDirectories(dir)
        diagrams                                      <- definitions
        paths                                         <- locations
        state: Option[(FS2Path, Map[String, String])] <- paths.traverse { case (parent, name) =>
          val path = parent / s".$name.generatr-state.json"
          readState(path).tupleLeft(path)
        }
        cacheDirectory: Option[FS2Path] = paths.map { case (parent, name) => parent / s".$name.generatr-cache" }
        cache: Option[Cache] <- cacheDirectory.traverse(openCache)
        _                    <- Files[IO].tempDirectory(Some(dir), ".generatr-", None).use { staging =>
          for
            prepared: List[Prepared] <- Stream
              .emits(diagrams)
              .parEvalMapUnordered(4)(diagram => prepare(dir, staging, diagram, cache, conf.forceDiagrams))
              .compile
              .toList
            _ <- prepared.traverse_(_.checkDestination)
            publish: IO[Unit] = prepared.traverse_(_.publish)
            _ <- state.fold(publish) { case (path, previous) =>
              publishWithState(path, conf.assets, previous, prepared.flatMap(_.files).toMap)(publish)
            }
          yield ()
        }
        _ <- cacheDirectory.traverse_(dir => pruneCache(dir, diagrams.iterator.map(d => fingerprint(d.getKey())).toSet))
      yield ()

  private case class ImagePair(directory: FS2Path, name: String):
    val svg = directory / s"$name${DiagramFormat.SVG.ext}"
    val png = directory / s"$name${DiagramFormat.PNG.ext}"

    def checksums: IO[(String, String)] = (checksum(svg), checksum(png)).tupled

    def matches(hashes: (String, String)): IO[Boolean] =
      fileMatches(svg, hashes._1).ifM(fileMatches(png, hashes._2), IO.pure(false))

    def copyTo(target: ImagePair): IO[Unit] =
      val flags = CopyFlags(ReplaceExisting)
      Files[IO].copy(svg, target.svg, flags) *> Files[IO].copy(png, target.png, flags)

    def moveTo(target: ImagePair): IO[Unit] =
      replace(svg, target.svg) *> replace(png, target.png)

  private case class Prepared(output: ImagePair, hashes: (String, String), staged: Option[ImagePair]):
    def files: List[(String, String)] =
      List(output.svg.fileName.toString -> hashes._1, output.png.fileName.toString -> hashes._2)

    def checkDestination: IO[Unit] = staged.traverse_ { _ =>
      List(output.svg, output.png).traverse_ { path =>
        Files[IO].isDirectory(path, false).flatMap { directory =>
          IO.raiseWhen(directory)(new IOException(s"Cannot publish diagram to $path because it is a directory."))
        }
      }
    }

    def publish: IO[Unit] = staged.traverse_(_.moveTo(output))

  private def prepare(
      dir: FS2Path,
      staging: FS2Path,
      diagram: Diagram,
      cache: Option[Cache],
      force: Boolean
  ): IO[Prepared] =
    val key = diagram.getKey()
    val definition = diagram.getDefinition()
    val output = ImagePair(dir, key)
    val staged = ImagePair(staging, key)
    for
      entry: Option[CacheEntry] <- cache
        .filter(_ => hasBundledInputs(definition))
        .flatTraverse(_.entry(key, definition))
      reuse: Option[Prepared] <- if (force) IO.pure(None) else entry.flatTraverse(_.restore(output, staged))
      result: Prepared        <- reuse match
        case Some(prepared) => IO.println(s"Skipping up-to-date diagram: $key").as(prepared)
        case None           =>
          for
            _      <- IO.println(s"Generating diagram: $key")
            reader <- IO.blocking(SourceStringReader(definition))
            _      <- render(reader, key, DiagramFormat.SVG, staged.svg)
            // Check the parsed diagram because a custom include can also select ELK.
            _ <- reader.getBlocks().asScala.headOption.map(_.getDiagram()) match
              case Some(graph: CucaDiagram) if graph.isUseElk() => ElkSvg.groupLinks(staged.svg)
              case _                                            => IO.unit
            _      <- render(reader, key, DiagramFormat.PNG, staged.png)
            hashes <- staged.checksums
            _      <- entry.traverse_(_.write(staged, hashes))
          yield Prepared(output, hashes, Some(staged))
    yield result

  private val imageFilename = raw"[^/\\:\p{Cntrl}]+\.(?:svg|png)".r
  private val sha256 = "[0-9a-f]{64}".r

  private def readState(path: FS2Path): IO[Map[String, String]] =
    Files[IO]
      .readUtf8(path)
      .compile
      .string
      .flatMap { contents =>
        val decoded = for
          state <- parse(contents)
          cursor = state.hcursor
          version <- cursor.get[Int]("version")
          _       <- Either.cond(version == 1, (), new IOException(s"Unsupported diagram state version $version."))
          files   <- cursor.get[Map[String, String]]("files")
          _       <- Either.cond(
            files.valuesIterator.forall(sha256.matches),
            (),
            new IOException("Invalid SHA-256 checksum.")
          )
        yield files
        IO.fromEither(
          decoded.leftMap(error => new IOException(s"Invalid diagram state file $path (${error.getMessage})", error))
        )
      }
      .recover { case _: NoSuchFileException => Map.empty }

  private def publishWithState(
      path: FS2Path,
      assets: AssetsPathConfig,
      previous: Map[String, String],
      published: Map[String, String]
  )(publish: IO[Unit]): IO[Unit] =
    if previous == published then publish
    else
      val current = JsonObject.fromIterable(published.toList.sortBy(_._1).map { case (name, hash) =>
        name -> hash.asJson
      })
      val state = Json.obj("version" -> 1.asJson, "files" -> current.asJson)
      writeRecord(path, state.spaces2 + "\n")(publish *> removeObsolete(assets, previous, published.keySet))

  private def removeObsolete(
      assets: AssetsPathConfig,
      previous: Map[String, String],
      published: Set[String]
  ): IO[Unit] =
    val source = assets.source.fs2Path
    val destination = assets.destination.fs2Path
    val obsolete = previous.iterator.filter { case (name, _) =>
      imageFilename.matches(name) && !published.contains(name)
    }.toList
    obsolete.traverse_ { case (name, hash) =>
      val image = destination / name
      val aliasesCurrentImage = published.iterator.filter(_.equalsIgnoreCase(name)).toList.existsM { current =>
        Files[IO].isSameFile(image, destination / current).recover { case _: NoSuchFileException => false }
      }
      Files[IO]
        .exists(source / name)
        .ifM(
          IO.unit,
          fileMatches(image, hash).ifM(
            aliasesCurrentImage.ifM(IO.unit, Files[IO].deleteIfExists(image).void),
            IO.unit
          )
        )
    }

  private def render(reader: SourceStringReader, key: String, format: DiagramFormat, path: FS2Path): IO[Unit] =
    Resource.fromAutoCloseable(IO.blocking(new FileOutputStream(path.toString))).use { os =>
      IO.blocking {
        val result = reader.outputImage(os, FileFormatOption(format.fileFormat, false))
        val image = Option(result).flatMap(result => Option(result.getImageData()))
        if (image.forall(data => ExitStatus.isErrorStatus(data.getStatus()))) {
          val reason = image.flatMap(data => Option(data.getWarningOrError())).filter(_.nonEmpty)
          throw new IOException(
            s"PlantUML failed to render $key as $format${reason.fold("")(message => s": $message")}",
            image.flatMap(data => Option(data.getRootCause())).orNull
          )
        }
      }
    }

  // These includes are supplied by the pinned PlantUML library.
  private val bundledInclude = raw"!include\s+<C4/C4(?:_Context|_Container|_Component|_Deployment|_Sequence)?>".r
  private val imageOrFunction = raw"(?i)\bimg:|<img\s|%[a-z_]\w*\s*\(".r

  private def hasBundledInputs(definition: String): Boolean =
    definition.linesIterator.map(_.trim).forall { line =>
      (!line.startsWith("!") || bundledInclude.matches(line) || line == PlantUmlLayout.elkPragma) &&
      imageOrFunction.findFirstIn(line).isEmpty
    }

  private def openCache(directory: FS2Path): IO[Cache] =
    val plantuml = Version.versionString()
    val size = GraphvizUtils.getenvImageLimit().toString
    val dotRenderer: IO[Option[String]] = IO
      .blocking {
        val graphviz = GraphvizRuntimeEnvironment.getInstance()
        val dot: String = Option(graphviz.getDotExe()).map(_.getAbsolutePath()).getOrElse("")
        // Bump this version when the rendering options or cache format change.
        Some(fingerprint(List("1", plantuml, dot, graphviz.dotVersion(), size).mkString("\n")))
      }
      .recover { case _: IOException => None }
    // Bump the ELK revision when its runtime or SVG grouping changes independently of PlantUML.
    val elkRenderer: String = fingerprint(List("elk-1", plantuml, size).mkString("\n"))
    dotRenderer.memoize.map(Cache(directory, _, elkRenderer))

  private case class Cache(directory: FS2Path, dotRenderer: IO[Option[String]], elkRenderer: String):
    def entry(key: String, definition: String): IO[Option[CacheEntry]] =
      val renderer: IO[Option[String]] =
        if (definition.linesIterator.exists(_.trim == PlantUmlLayout.elkPragma)) IO.pure(Some(elkRenderer))
        else dotRenderer
      renderer.map(_.map(id => CacheEntry(directory, fingerprint(key), fingerprint(s"$id\n$definition"))))

  private val cacheFilename = raw"([0-9a-f]{64})\.(?:sha256|svg|png)".r

  private def pruneCache(directory: FS2Path, activeKeys: Set[String]): IO[Unit] =
    val prune = Files[IO]
      .list(directory)
      .evalMap { path =>
        path.fileName.toString match
          case cacheFilename(key) if !activeKeys.contains(key) =>
            Files[IO].isRegularFile(path, false).ifM(Files[IO].deleteIfExists(path).void, IO.unit)
          case _ => IO.unit
      }
      .compile
      .drain
    Files[IO].isDirectory(directory, false).ifM(prune, IO.unit).recoverWith { case error: IOException =>
      IO.println(s"Could not prune the diagram cache in $directory (${error.getMessage}).")
    }

  private case class CacheEntry(directory: FS2Path, key: String, input: String):
    private val record = directory / s"$key.sha256"
    private val images = ImagePair(directory, key)

    def restore(output: ImagePair, staged: ImagePair): IO[Option[Prepared]] =
      val read = Files[IO]
        .isRegularFile(record, false)
        .ifM(
          Files[IO].readUtf8(record).compile.string.map(_.linesIterator.toList),
          IO.pure(Nil)
        )
      read
        .flatMap {
          case List(`input`, svgHash, pngHash) =>
            val hashes = (svgHash, pngHash)
            for
              alreadyPublished <- output.matches(hashes)
              valid            <-
                if (alreadyPublished) images.matches(hashes)
                else
                  List(images.svg, images.png)
                    .forallM(Files[IO].isRegularFile(_, false))
                    .ifM(images.copyTo(staged) *> staged.matches(hashes), IO.pure(false))
            yield Option.when(valid)(Prepared(output, hashes, Option.unless(alreadyPublished)(staged)))
          case _ => IO.none
        }
        .recover { case _: IOException => None }

    def write(staged: ImagePair, hashes: (String, String)): IO[Unit] =
      val (svgHash, pngHash) = hashes
      val writeImages = Files[IO].tempDirectory(Some(directory), ".diagram-", None).use { temporary =>
        val copy = ImagePair(temporary, key)
        staged.copyTo(copy) *> writeRecord(record, s"$input\n$svgHash\n$pngHash\n")(copy.moveTo(images))
      }
      (Files[IO].createDirectories(directory) *> writeImages).recoverWith { case error: IOException =>
        IO.println(s"Could not save the diagram cache in $directory (${error.getMessage}).")
      }

  private def writeRecord(path: FS2Path, contents: String)(publish: IO[Unit]): IO[Unit] =
    Files[IO].tempFile(path.parent, ".generatr-", ".tmp", None).use { staged =>
      Stream.emit(contents).through(Files[IO].writeUtf8(staged)).compile.drain *> publish *> replace(staged, path)
    }

  private def fingerprint(value: String): String =
    Hashing.hashChunk(SHA256, Chunk.array(value.getBytes(UTF_8))).toString

  private def checksum(path: FS2Path): IO[String] =
    Files[IO].readAll(path).through(Hashing[IO].hash(SHA256)).compile.lastOrError.map(_.toString)

  private def fileMatches(path: FS2Path, expected: String): IO[Boolean] =
    Files[IO]
      .isRegularFile(path, false)
      .ifM(checksum(path).map(_ == expected), IO.pure(false))
      .recover { case _: NoSuchFileException => false }

  private def replace(from: FS2Path, to: FS2Path): IO[Unit] =
    Files[IO].move(from, to, CopyFlags(AtomicMove, ReplaceExisting)).recoverWith {
      case _: AtomicMoveNotSupportedException => Files[IO].move(from, to, CopyFlags(ReplaceExisting))
    }
