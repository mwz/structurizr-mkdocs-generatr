package io.github.mwz.structurizr.mkdocs

import cats.effect.IO
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Flags
import fs2.io.file.Path as fs2Path
import java.nio.file.Path

trait MkDocs {
  def writeSections(sections: Stream[IO, Section]): IO[Unit]
}

object MkDocs {
  def apply(basePath: Path) = new MkDocs:
    def writeSections(sections: Stream[IO, Section]): IO[Unit] =
      val path = fs2Path.fromNioPath(basePath)
      Files.forIO.exists(path).ifM(IO.unit, Files.forIO.createDirectory(path)) >>
        sections
          .evalTap(section => IO.println(s"Processing ${section.filename}"))
          .flatMap { section =>
            val path = fs2Path.fromNioPath(basePath) / fs2Path(section.filename)
            Stream(section.md)
              .through(Files[IO].writeUtf8(path, Flags.Write))
          }
          .compile
          .drain
}
