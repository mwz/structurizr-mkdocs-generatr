package io.github.mwz.structurizr.mkdocs

import munit.FunSuite

import java.nio.file.Path

import cli.CommandLineArgs

class ConfigTest extends FunSuite:
  test("resolves default paths"):
    assertEquals(
      Config(CommandLineArgs()),
      Config(
        PathConfig(Path.of("./"), Path.of("./docs")),
        AssetsPathConfig(Path.of("./img"), Path.of("./docs/img"))
      )
    )

  test("resolves relative docs path"):
    assertEquals(
      Config(CommandLineArgs(Path.of("./"), assetsDir = "img", docsDir = "docs")),
      Config(
        PathConfig(Path.of("./"), Path.of("./docs")),
        AssetsPathConfig(Path.of("./img"), Path.of("./docs/img"))
      )
    )
    assertEquals(
      Config(CommandLineArgs(Path.of("dir"), assetsDir = "nested/folder", docsDir = "docs")),
      Config(
        PathConfig(Path.of("dir"), Path.of("dir/docs")),
        AssetsPathConfig(Path.of("dir/nested/folder"), Path.of("dir/docs/img"))
      )
    )
    assertEquals(
      Config(CommandLineArgs(Path.of("/a/b/c"), assetsDir = "i", docsDir = "d/e")),
      Config(
        PathConfig(Path.of("/a/b/c"), Path.of("/a/b/c/d/e")),
        AssetsPathConfig(Path.of("/a/b/c/i"), Path.of("/a/b/c/d/e/img"))
      )
    )

  test("relativises docs path"):
    assertEquals(
      Config(CommandLineArgs(Path.of("./"), docsDir = "docs")).path.relativeDocsPath,
      Path.of("docs")
    )
    assertEquals(
      Config(CommandLineArgs(Path.of("/a/b"), docsDir = "c/d")).path.relativeDocsPath,
      Path.of("c/d")
    )
