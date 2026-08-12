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
import com.structurizr.Workspace as SWorkspace
import com.structurizr.`export`.Diagram
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path as FS2Path
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader

import java.io.FileOutputStream
import scala.jdk.CollectionConverters.*

enum DiagramFormat(val ext: String, val fileFormat: FileFormat):
  case SVG extends DiagramFormat(".svg", FileFormat.SVG)
  case PNG extends DiagramFormat(".png", FileFormat.PNG)

trait Diagrams:
  def exportAll(): IO[Unit]

object Diagrams:
  def apply(workspace: SWorkspace, conf: AssetsPathConfig): Diagrams = new Diagrams:
    val plantUMLExporter = CustomC4PlantUMLExporter(workspace)
    plantUMLExporter.addSkinParam("svgDimensionStyle", "false")
    plantUMLExporter.addSkinParam("preserveAspectRatio", "meet")

    val diagrams: Seq[Diagram] =
      plantUMLExporter.`export`(workspace).asScala.toSeq

    def save(path: FS2Path, diagram: Diagram): IO[Unit] =
      val file = path / FS2Path(s"${diagram.getKey()}")
      val reader = SourceStringReader(diagram.getDefinition())
      def write(format: DiagramFormat): IO[Unit] =
        Resource
          .fromAutoCloseable(IO(new FileOutputStream(s"${file.toString}${format.ext}")))
          .use { os =>
            IO.blocking(reader.outputImage(os, FileFormatOption(format.fileFormat, false))).void
          }

      IO.println(s"Generating diagram: ${diagram.getKey()}") >>
        write(DiagramFormat.SVG) &> write(DiagramFormat.PNG)

    // TODO: Skip generating diagram if up-to-date (use puml definition to compare)
    def exportAll(): IO[Unit] =
      val dir = conf.destination.fs2Path
      val exportDiagrams: IO[Unit] =
        Stream
          .emits[IO, Diagram](diagrams)
          .parEvalMapUnordered(4)(diagram => save(dir, diagram))
          .compile
          .drain
      Files[IO].exists(dir).ifM(IO.unit, Files[IO].createDirectories(dir)) >>
        exportDiagrams
