package io.github.mwz.structurizr.mkdocs

import cats.effect.*
import cats.syntax.parallel.*
import com.structurizr.dsl.StructurizrDslParser
import com.structurizr.model.SoftwareSystem
import com.structurizr.Workspace as SWorkspace
import fs2.Pipe
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Flags
import fs2.io.stdoutLines

import java.io.File
import java.nio.file.Path
import scala.collection.immutable.TreeMap
import scala.jdk.CollectionConverters.*

object mainApp extends IOApp.Simple:
  // TODO: Make the base path configurable via a command line argument
  val basePath: Path = Path.of("./mkdocs")

  val run: IO[Unit] = for
    workspace <- Workspace().get()
    docs = Docs(workspace.getDocumentation().getSections().asScala.toSeq)
    softwareSystems = SoftwareSystems(
      workspace.getModel().getSoftwareSystems().asScala.toSeq,
      workspace.getViews(),
      basePath,
      workspace.getModel().getProperties().asScala.getOrElse("structurizr.groupSeparator", "/")
    )
    diagrams = Diagrams(workspace, basePath)
    _ <- exec(docs, softwareSystems, diagrams, MkDocs(basePath))
  yield ()

  def exec(docs: Docs, softwareSystems: SoftwareSystems, diagrams: Diagrams, mkDocs: MkDocs): IO[Unit] =
    val sections: Stream[IO, SectionHierarchy] =
      Stream.emits(docs.sections()) ++
        Stream.emit(softwareSystems.summarySection) ++
        Stream.emit(softwareSystems.sectionHierarchy())
    val actions = Seq(
      mkDocs.writeSectionHierarchy(sections),
      mkDocs.writeMkDocsConfig(sections),
      diagrams.exportAll()
    )
    actions.parSequence.void

  // TODO:
  // Fill content:
  //  - ✅ homepage - 1st doc section - rename to index
  //  - ✅ docs
  //  - generate empty index if no docs are present
  //  - ✅ software systems summary
  //       - ✅ link to software systems
  //  - software system
  //    - ✅ Info
  //      - docs
  //      - ADRs
  //    - ✅ Context views
  //    - ✅ Container views
  //      - docs
  //      - ADRs
  //    - ✅ Dynamic views
  //    - Dependencies
  //      - Inbound
  //      - Outbound
  //    - Component view
  //    - Deployment view
  // Save file:
  //  - remove flexmark extensions
  //  - resolve paths to embedded diagrams
  // ✅ Generate mkdocs config and add files to the nav:
  //   - ✅ support system hierarchy in nav/toc
  // Generate diagrams:
  //  - ✅ save as svg
  //  - save as png
  //  - ✅ add a link to the image
  //  - generate custom css for improved diagram styling + add to mkdocs config
  //  - ⬜︎ include in system views
  //  - export with element links (custom C4PlantUMLExporter)
