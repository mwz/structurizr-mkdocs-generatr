package io.github.mwz.structurizr.mkdocs

import cats.effect.IO
import com.structurizr.Workspace as SWorkspace
import com.structurizr.dsl.StructurizrDslParser
import com.structurizr.view.ThemeUtils
import fs2.io.file.Files

import java.io.File

trait Workspace:
  def get(): IO[SWorkspace]

object Workspace:
  def apply(pathConfig: PathConfig): Workspace = new Workspace {
    def get(): IO[SWorkspace] =
      val workspaceDslPath = pathConfig.basePath.fs2Path / "workspace.dsl"
      Files[IO]
        .exists(workspaceDslPath)
        .ifM(IO.unit, IO.raiseError(new Exception(s"${workspaceDslPath.toString} not found"))) >>
        IO.blocking {
          val parser = StructurizrDslParser()
          parser.parse(File(workspaceDslPath.absolute.toString))
          val workspace = parser.getWorkspace()
          ThemeUtils.loadThemes(workspace)
          workspace
        }
  }
