package io.github.mwz.structurizr.mkdocs

import munit.FunSuite

import java.nio.file.Path

import cli.CommandLineArgs

class ConfigTest extends FunSuite:
  test("resolves relative docs path"):
    assertEquals(
      Config(CommandLineArgs(Path.of("./"), "docs")),
      Config(PathConfig(Path.of("./"), Path.of("./docs")))
    )
    assertEquals(
      Config(CommandLineArgs(Path.of("dir"), "docs")),
      Config(PathConfig(Path.of("dir"), Path.of("dir/docs")))
    )
    assertEquals(
      Config(CommandLineArgs(Path.of("/a/b/c"), "d")),
      Config(PathConfig(Path.of("/a/b/c"), Path.of("/a/b/c/d")))
    )

  test("relativises docs path"):
    assertEquals(
      Config(CommandLineArgs(Path.of("./"), "docs")).path.relativeDocsPath,
      Path.of("docs")
    )
    assertEquals(
      Config(CommandLineArgs(Path.of("/a/b"), "c/d")).path.relativeDocsPath,
      Path.of("c/d")
    )
