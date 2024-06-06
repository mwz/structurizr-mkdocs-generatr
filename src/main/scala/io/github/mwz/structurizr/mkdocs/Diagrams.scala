package io.github.mwz.structurizr.mkdocs

import cats.effect.IO
import cats.effect.kernel.Resource
import com.structurizr.Workspace as SWorkspace
import com.structurizr.`export`.Diagram
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path as fs2Path
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader

import java.io.FileOutputStream
import java.nio.file.Path
import scala.jdk.CollectionConverters.*

enum DiagramFormat(val ext: String, val fileFormat: FileFormat):
  case SVG extends DiagramFormat(".svg", FileFormat.SVG)
  case PNG extends DiagramFormat(".png", FileFormat.PNG)

trait Diagrams:
  def exportAll(): IO[Unit]

object Diagrams:
  def apply(workspace: SWorkspace, basePath: Path): Diagrams = new Diagrams:
    val plantUMLExporter = CustomC4PlantUMLExporter(workspace)
    plantUMLExporter.addSkinParam("svgDimensionStyle", "false")
    plantUMLExporter.addSkinParam("preserveAspectRatio", "meet")

    val diagrams: Seq[Diagram] =
      plantUMLExporter.`export`(workspace).asScala.toSeq

    def save(path: fs2Path, diagram: Diagram): IO[Unit] =
      val file   = path / fs2Path(s"${diagram.getKey()}")
      val reader = SourceStringReader(diagram.getDefinition())
      def write(format: DiagramFormat): IO[Unit] =
        Resource
          .make(IO(new FileOutputStream(s"${file.toString}${format.ext}"))) { os =>
            IO.blocking(os.close())
          }
          .use { os =>
            IO.blocking(reader.outputImage(os, FileFormatOption(format.fileFormat, false))).void
          }

      IO.println(s"Generating diagram: ${diagram.getKey()}") >>
        write(DiagramFormat.SVG) &> write(DiagramFormat.PNG)

    // TODO: Skip generating diagram if up-to-date (use puml definition to compare)
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
