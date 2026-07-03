package io.github.mwz.structurizr.mkdocs

import munit.CatsEffectSuite

import java.nio.file.Path
import scala.jdk.CollectionConverters.*

class WorkspaceTest extends CatsEffectSuite:
  test("fail to read non-existing workspace file"):
    val config = PathConfig(Path.of("non-existing-path"), Path.of("non-existing-path/docs"))
    interceptIO[Exception](Workspace(config).get())

  test("read the example workspace"):
    val conf = PathConfig(Path.of("example"), Path.of("example/workspace-docs"))
    val workspace = Workspace(conf).get()

    val expectedSystems = Set(
      "Acquirer",
      "ATM",
      "E-mail System",
      "Internet Banking System",
      "Mainframe Banking System"
    )

    val systems = workspace.map(_.getModel().getSoftwareSystems().asScala.toSet.map(_.getName()))
    assertIO(systems, expectedSystems)
