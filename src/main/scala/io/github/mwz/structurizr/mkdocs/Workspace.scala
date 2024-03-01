package io.github.mwz.structurizr.mkdocs

import com.structurizr.Workspace as SWorkspace
import cats.effect.IO
import com.structurizr.dsl.StructurizrDslParser
import java.io.File

trait Workspace {
  def get(): IO[SWorkspace]
}

object Workspace {
  def apply(): Workspace = new Workspace {
    def get(): IO[SWorkspace] = IO.blocking {
      val parser = StructurizrDslParser()
      parser.parse(File("workspace.dsl"))
      parser.getWorkspace()
    }
  }
}
