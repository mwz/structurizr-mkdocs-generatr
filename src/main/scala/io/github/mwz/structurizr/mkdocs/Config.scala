package io.github.mwz.structurizr.mkdocs

import fs2.io.file.Path as FS2Path

import java.nio.file.Path

import cli.CommandLineArgs

case class Config(path: PathConfig, assets: AssetsPathConfig)
case class PathConfig(basePath: Path, docsPath: Path)
case class AssetsPathConfig(source: Path, destination: Path)

extension (pathConfig: PathConfig)
  def relativeDocsPath: Path =
    pathConfig.basePath.relativize(pathConfig.docsPath)

object Config:
  def apply(options: CommandLineArgs): Config =
    val docsPath = options.path.resolve(options.docsDir)
    val assets = AssetsPathConfig(
      source = options.path.resolve(options.assetsDir),
      destination = docsPath.resolve("img")
    )
    Config(PathConfig(options.path, docsPath), assets)

extension (path: Path) def fs2Path: FS2Path = FS2Path.fromNioPath(path)
