package io.github.mwz.structurizr.mkdocs

import cats.effect.IO
import fs2.io.file.CopyFlag.ReplaceExisting
import fs2.io.file.CopyFlags
import fs2.io.file.Files

trait Assets:
  def copy(): IO[Unit]

object Assets:
  def apply(conf: AssetsPathConfig) = new Assets:
    def copy(): IO[Unit] =
      val sourceDir = conf.source.fs2Path
      val targetDir = conf.destination.fs2Path
      val copy = Files[IO]
        .walk(conf.source.fs2Path)
        .parEvalMapUnordered(4) { sourcePath =>
          val targetPath = targetDir.resolve(sourceDir.relativize(sourcePath))
          Files[IO]
            .isDirectory(sourcePath)
            .ifM(
              Files[IO].createDirectories(targetPath),
              IO.println("Copying " + sourcePath + " to " + targetPath) *>
                Files[IO].copy(sourcePath, targetPath, CopyFlags(ReplaceExisting))
            )
        }
        .compile
        .drain
      Files[IO]
        .exists(sourceDir)
        .ifM(copy, IO.raiseError(new RuntimeException(s"Source directory ${sourceDir} does not exist.")))
