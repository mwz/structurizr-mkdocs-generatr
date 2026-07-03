package io.github.mwz.structurizr.mkdocs

import caseapp.*
import caseapp.catseffect.*
import cats.effect.*
import fs2.Stream

import java.nio.file.Path
import scala.jdk.CollectionConverters.*

import cli.CommandLineArgs

object Main extends IOCaseApp[CommandLineArgs]:
  def run(options: CommandLineArgs, remainingArgs: RemainingArgs): IO[ExitCode] =
    run(Config(options)).as(ExitCode.Success)

  def run(config: Config): IO[Unit] = for
    workspace <- Workspace(config.path).get()
    docs = Docs(workspace.getDocumentation().getSections().asScala.toSeq)
    softwareSystems = SoftwareSystems(
      workspace.getModel().getSoftwareSystems().asScala.toSeq,
      workspace.getViews(),
      workspace.getModel().getRelationships().asScala.toSet,
      workspace.getModel().getProperties().asScala.getOrElse("structurizr.groupSeparator", "/")
    )
    images      = Assets(config.assets)
    diagrams    = Diagrams(workspace, config.assets)
    postProcess = PostProcess(config.path)
    _ <- exec(docs, softwareSystems, images, diagrams, MkDocs(config.path, Clock[IO]), postProcess)
  yield ()

  def exec(
      docs: Docs,
      softwareSystems: SoftwareSystems,
      images: Assets,
      diagrams: Diagrams,
      mkDocs: MkDocs,
      postProcess: PostProcess
  ): IO[Unit] =
    val sections: Stream[IO, SectionHierarchy] =
      Stream.emits(docs.sections()) ++
        Stream.emit(softwareSystems.summarySection()) ++
        Stream.emit(softwareSystems.sectionHierarchy())
    (mkDocs.writeSectionHierarchy(sections) &> mkDocs.writeMkDocsConfig(sections)) >>
      // post-process the written config (e.g. copy extra_css files)
      postProcess.run() >>
      images.copy() >>
      // export diagrams at the end as this mutates the elements' url property
      diagrams.exportAll()
