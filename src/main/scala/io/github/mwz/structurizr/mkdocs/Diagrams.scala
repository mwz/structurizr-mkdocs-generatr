package io.github.mwz.structurizr.mkdocs

import cats.effect.IO
import com.structurizr.`export`.Diagram
import com.structurizr.`export`.plantuml.C4PlantUMLExporter
import com.structurizr.Workspace as SWorkspace
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path as fs2Path

import java.nio.file.Path
import scala.collection.JavaConverters.*
import net.sourceforge.plantuml.SourceStringReader
import cats.effect.kernel.Resource
import java.io.FileOutputStream
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.FileFormat
import cats.Eval

trait Diagrams:
  def all(): Seq[Diagram]
  def exportAll(): IO[Unit]

object Diagrams:
  def apply(workspace: SWorkspace, basePath: Path): Diagrams = new Diagrams:
    val plantUMLExporter = C4PlantUMLExporter()
    val diagrams: Seq[Diagram] =
      plantUMLExporter.`export`(workspace).asScala.toSeq

    def all(): Seq[Diagram] =
      diagrams

    def save(path: fs2Path, diagram: Diagram): IO[Unit] =
      val file   = path / fs2Path(s"${diagram.getKey()}.svg")
      val reader = SourceStringReader(diagram.getDefinition())
      IO.println(s"Writing diagram: ${diagram.getKey()}") >>
        Resource
          .make(IO(new FileOutputStream(file.toString))) { os =>
            IO(os.close()).handleErrorWith(_ => IO.unit)
          }
          .use { os =>
            IO.blocking(reader.outputImage(os, FileFormatOption(FileFormat.SVG, false)))
          }

    def exportAll(): IO[Unit] =
      val dir = fs2Path.fromNioPath(basePath) / "img"
      val exportDiagrams: IO[Unit] =
        Stream
          .emits[IO, Diagram](diagrams)
          .mapAsync(4)(diagram => save(dir, diagram))
          .compile
          .drain
      Files[IO].exists(dir).ifM(IO.unit, Files[IO].createDirectories(dir)) >>
        exportDiagrams
