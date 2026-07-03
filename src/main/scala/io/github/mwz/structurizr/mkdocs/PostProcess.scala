package io.github.mwz.structurizr.mkdocs

import cats.effect.IO
import cats.syntax.foldable.*
import fs2.io.file.CopyFlag.ReplaceExisting
import fs2.io.file.CopyFlags
import fs2.io.file.Files
import fs2.io.file.Path as FS2Path
import io.circe.Json
import io.circe.yaml.v12.parser

/**
 * Post-processing actions performed after the mkdocs.yml config has been written. These are driven by the content of
 * the generated config, so further config-based actions can be chained onto `run` in the future.
 */
trait PostProcess:
  def run(): IO[Unit]

object PostProcess:
  def apply(conf: PathConfig) = new PostProcess:
    def run(): IO[Unit] = readConfig.flatMap(copyExtraCss)

    // Read and parse the generated mkdocs.yml (lives next to mkdocs.tmpl.yml in basePath).
    private def readConfig: IO[Json] =
      Files[IO]
        .readAll(conf.basePath.fs2Path / "mkdocs.yml")
        .through(fs2.text.utf8.decode)
        .compile
        .string
        .flatMap(s => IO.fromEither(parser.parse(s)))

    // Copy each `extra_css` entry from basePath into docsPath, preserving the relative
    // path so the setting (resolved by MkDocs relative to docs_dir) keeps working.
    private def copyExtraCss(json: Json): IO[Unit] =
      val cssFiles = json.hcursor.downField("extra_css").as[List[String]].getOrElse(Nil)
      cssFiles.traverse_ { entry =>
        val src  = conf.basePath.fs2Path / FS2Path(entry)
        val dest = conf.docsPath.fs2Path / FS2Path(entry)
        Files[IO]
          .exists(src)
          .ifM(
            IO.fromOption(dest.parent)(new RuntimeException(s"Path doesn't have a parent directory: $dest"))
              .flatMap(Files[IO].createDirectories) *>
              IO.println(s"Copying extra CSS $src to $dest") *>
              Files[IO].copy(src, dest, CopyFlags(ReplaceExisting)),
            IO.println(s"Extra CSS file $src does not exist - it will not be copied.")
          )
      }
