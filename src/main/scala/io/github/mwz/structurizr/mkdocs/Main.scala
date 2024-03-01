package io.github.mwz.structurizr.mkdocs

import cats.effect.*
import com.structurizr.dsl.StructurizrDslParser
import java.io.File
import scala.jdk.CollectionConverters.*
import com.structurizr.model.SoftwareSystem
import fs2.Stream
import fs2.Pipe
import fs2.io.file.Files
import fs2.io.file.Flags
import fs2.io.stdoutLines
import com.structurizr.Workspace as SWorkspace
import scala.collection.immutable.TreeMap
import java.nio.file.Path

object mainApp extends IOApp.Simple:

  val run: IO[Unit] = for {
    workspace <- Workspace().get()
    docs = Docs(workspace.getDocumentation().getSections().asScala.toSeq)
    softwareSystems = SoftwareSystems(
      workspace.getModel().getSoftwareSystems().asScala.toSeq,
      workspace.getModel().getProperties().asScala.getOrElse("structurizr.groupSeparator", "/")
    )
    // TODO: make mkDocs path configurable
    mkDocs = MkDocs(Path.of("./mkdocs"))
    _ <- exec(docs, softwareSystems, mkDocs)
  } yield ()

  def exec(docs: Docs, softwareSystems: SoftwareSystems, mkDocs: MkDocs): IO[Unit] =
    val sections: Stream[IO, Section] =
      Stream.emits(docs.sections()) ++
        Stream.emit(softwareSystems.section)
    mkDocs.writeSections(sections)

  // TODO:
  // Fill content:
  //  - ✅ homepage - 1st doc section
  //  - ✅ docs
  //  - ✅ software systems summary
  //  - software system
  //    - info
  //    - context views
  //    - container views
  //    - dynamic views
  //    - dependencies
  //      - inbound
  //      - outbound
  // Save file
  //  - remove flexmark extensions
  //  - resolve paths to embedded diagrams
  // ⬜︎ Generate mkdocs config and add files to the nav
  // Generate diagrams
  //  - export with element links (custom C4PlantUMLExporter)
  //  - save as svg (can svg be used in mkdocs?)
