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
    images   = Assets(config.assets)
    diagrams = Diagrams(workspace, config.assets)
    _ <- exec(docs, softwareSystems, images, diagrams, MkDocs(config.path, Clock[IO]))
  yield ()

  def exec(docs: Docs, softwareSystems: SoftwareSystems, images: Assets, diagrams: Diagrams, mkDocs: MkDocs): IO[Unit] =
    val sections: Stream[IO, SectionHierarchy] =
      Stream.emits(docs.sections()) ++
        Stream.emit(softwareSystems.summarySection()) ++
        Stream.emit(softwareSystems.sectionHierarchy())
    (mkDocs.writeSectionHierarchy(sections) &> mkDocs.writeMkDocsConfig(sections)) >>
      images.copy() >>
      // export diagrams at the end as this mutates the elements' url property
      diagrams.exportAll()
