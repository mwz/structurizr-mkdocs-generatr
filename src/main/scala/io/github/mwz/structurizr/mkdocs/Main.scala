package io.github.mwz.structurizr.mkdocs

import cats.effect.*
import com.structurizr.Workspace as SWorkspace
import fs2.Stream

import java.nio.file.Path
import scala.jdk.CollectionConverters.*

object Main extends IOApp.Simple:
  // TODO: Make the base path configurable via a command line argument
  val basePath: Path = Path.of("./mkdocs")

  val run: IO[Unit] = for
    workspace <- Workspace().get()
    docs = Docs(workspace.getDocumentation().getSections().asScala.toSeq)
    softwareSystems = SoftwareSystems(
      workspace.getModel().getSoftwareSystems().asScala.toSeq,
      workspace.getViews(),
      workspace.getModel().getRelationships().asScala.toSet,
      basePath,
      workspace.getModel().getProperties().asScala.getOrElse("structurizr.groupSeparator", "/")
    )
    diagrams = Diagrams(workspace, basePath)
    _ <- exec(docs, softwareSystems, diagrams, MkDocs(basePath, Clock[IO]))
  yield ()

  def exec(docs: Docs, softwareSystems: SoftwareSystems, diagrams: Diagrams, mkDocs: MkDocs): IO[Unit] =
    val sections: Stream[IO, SectionHierarchy] =
      Stream.emits(docs.sections()) ++
        Stream.emit(softwareSystems.summarySection) ++
        Stream.emit(softwareSystems.sectionHierarchy())
    (mkDocs.writeSectionHierarchy(sections) &> mkDocs.writeMkDocsConfig(sections)) >>
      // export diagrams at the end as this mutates the elements' url property
      diagrams.exportAll()

  // TODO:
  // Fill content:
  //  - ✅ homepage - 1st doc section - rename to index
  //  - ✅ docs
  //  - ✅ generate empty index if no docs are present
  //  - ✅ software systems summary
  //       - ✅ link to software systems
  //  - software system
  //    - ✅ About
  //      - docs
  //      - ADRs
  //    - ✅ Context views
  //    - ✅ Container views
  //      - docs
  //      - ADRs
  //    - ✅ Dynamic views
  //    - ✅ Dependencies
  //      - ✅ Inbound
  //      - ✅ Outbound
  //    - Component view
  //    - Deployment view
  // Save file:
  //  - remove flexmark extensions
  //  - ✅ resolve paths to embedded diagrams
  // ✅ Generate mkdocs config and add files to the nav:
  //   - ✅ support system hierarchy in nav/toc
  //   - add inline-select-svg plugin
  // Generate diagrams:
  //  - ✅ save as svg
  //  - ✅ save as png
  //  - ✅ add a link to the image
  //  - ✅ include in system views
  //  - export SVGs with element links (custom C4PlantUMLExporter)
  //    - ✅ System elements
  //    - Container elements
