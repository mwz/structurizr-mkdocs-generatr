package io.github.mwz.structurizr.mkdocs

import cats.effect.IO
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Flags
import fs2.io.file.Path as fs2Path
import io.circe.*
import io.circe.syntax.*
import io.circe.yaml.v12.Printer
import io.circe.yaml.v12.Printer.Config
import io.circe.yaml.v12.parser
import io.circe.yaml.v12.syntax.*

import java.nio.file.Path

trait MkDocs:
  def writeSections(sections: Stream[IO, Section]): IO[Unit]
  def writeMkDocsConfig(sections: Stream[IO, Section]): IO[Unit]

object MkDocs:
  val printer = Printer.make(Config(preserveOrder = true))

  def apply(basePath: Path = Path.of("./mkdocs")) = new MkDocs:
    def writeSections(sections: Stream[IO, Section]): IO[Unit] =
      val path = fs2Path.fromNioPath(basePath)
      Files.forIO.exists(path).ifM(IO.unit, Files.forIO.createDirectory(path)) >>
        sections
          .evalTap(section => IO.println(s"Processing ${section.title} ${section.filename}"))
          .flatMap { section =>
            val path = fs2Path.fromNioPath(basePath) / fs2Path(section.filename)
            Stream(section.md)
              .through(Files[IO].writeUtf8(path, Flags.Write))
          }
          .compile
          .drain

    // Write the mkdocs config file.
    // TODO: use a default minimal config if no template is provided
    def writeMkDocsConfig(sections: Stream[IO, Section]): IO[Unit] =
      val sectionsJson: IO[Json] = sections.compile.toList.map(
        _.map(s => JsonObject(s.title.getOrElse("") -> s.filename.asJson)).asJson
      )
      val template = Files[IO]
        .readAll(fs2Path("mkdocs.tmpl.yml"))
        .through(fs2.text.utf8.decode)
        .compile
        .string

      // populate the nav section
      for
        sectionsJson <- sectionsJson
        json <- template
          .flatMap(s => IO.fromEither(parser.parse(s)))
          .flatTap(s => IO.println(s))
          .map(json =>
            json.hcursor
              .withFocus(_.mapObject(_.add("docs_dir", basePath.toString.asJson)))
              .withFocus(_.mapObject(_.add("nav", Json.fromValues(sectionsJson.asArray.getOrElse(Vector.empty)))))
              .top
              .getOrElse(json)
          )
          .flatTap(s => IO.println(s))
        _ <- Stream(printer.pretty(json))
          .through(Files[IO].writeUtf8(fs2Path("mkdocs.yml"), Flags.Write))
          .compile
          .drain
      yield ()
