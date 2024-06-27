package io.github.mwz.structurizr.mkdocs

import cats.effect.IO
import cats.effect.kernel.Clock
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Flags
import fs2.io.file.Path as fs2Path
import io.circe.*
import io.circe.syntax.*
import io.circe.yaml.v12.Printer
import io.circe.yaml.v12.parser

import java.nio.file.Path
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

trait SectionHierarchy
case class SectionGroup(title: Option[String], sections: Seq[SectionHierarchy]) extends SectionHierarchy
case class Section(title: Option[String], md: String, filename: String)         extends SectionHierarchy

trait MkDocs:
  def writeSections(sections: Stream[IO, Section]): IO[Unit]
  def writeSectionHierarchy(sectionHierarchy: Stream[IO, SectionHierarchy]): IO[Unit]
  def writeMkDocsConfig(sections: Stream[IO, SectionHierarchy]): IO[Unit]

object MkDocs:
  val printer   = Printer.builder.withPreserveOrder(true).build()
  val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy @ HH:mm")

  // Custom encoder for SectionHierarchy
  given encodeSectionHierarchy: Encoder[SectionHierarchy] = new Encoder[SectionHierarchy]:
    final def apply(h: SectionHierarchy): Json = h match
      case sg: SectionGroup =>
        Json.obj(sg.title.getOrElse("untitled") -> sg.sections.asJson)
      case s: Section =>
        s.title.fold(s.filename.asJson)(title => JsonObject(title -> s.filename.asJson).asJson)

  def apply(basePath: Path, clock: Clock[IO]) = new MkDocs:
    def flatten(sections: Stream[IO, SectionHierarchy]): Stream[IO, Section] =
      sections.flatMap {
        case SectionGroup(title, sections) =>
          Stream.emits(sections).covary[IO].through(flatten)
        case s: Section =>
          Stream.emit(s)
      }

    def writeSectionHierarchy(sectionHierarchy: Stream[IO, SectionHierarchy]): IO[Unit] =
      writeSections(flatten(sectionHierarchy))

    def writeSections(sections: Stream[IO, Section]): IO[Unit] =
      val generatedAt: IO[ZonedDateTime] = clock.realTimeInstant.map(_.atZone(ZoneId.of("UTC")))
      val path                           = fs2Path.fromNioPath(basePath)
      Files.forIO.exists(path).ifM(IO.unit, Files.forIO.createDirectory(path)) >>
        sections
          .evalTap(section => IO.println(s"Processing ${section.title.getOrElse("")} - ${section.filename}"))
          .evalTap { s =>
            val filename = path / fs2Path(s.filename)
            IO.fromOption(filename.parent)(new RuntimeException(s"Path doesn't have a parent directory: $filename"))
              .flatMap { parent =>
                Files[IO]
                  .exists(parent)
                  .ifM(IO.unit, Files[IO].createDirectories(parent))
              }
          }
          .flatMap { section =>
            val path = fs2Path.fromNioPath(basePath) / fs2Path(section.filename)
            val footer = generatedAt.map { time =>
              s"""\n
              |<div style="font-size: small; font-style: italic;">
              |${formatter.format(time)}
              |</div>""".stripMargin
            }
            val content = footer.map(s => section.md + s)

            Stream
              .eval(content)
              .through(Files[IO].writeUtf8(path, Flags.Write))
          }
          .compile
          .drain

    // Write the mkdocs config file.
    // TODO: use a default minimal config if no template is provided
    // TODO: add inline-select-svg plugin
    def writeMkDocsConfig(sections: Stream[IO, SectionHierarchy]): IO[Unit] =
      val parentPath = IO.fromOption(fs2Path.fromNioPath(basePath).parent)(
        new RuntimeException(s"Path doesn't have a parent directory: $basePath")
      )

      val sectionsJson: IO[Json] =
        sections.compile.toList
          .map(_.asJson)

      def template(path: fs2Path) =
        Files[IO]
          .readAll(path / "mkdocs.tmpl.yml")
          .through(fs2.text.utf8.decode)
          .compile
          .string

      // populate the nav section
      for
        parent       <- parentPath
        sectionsJson <- sectionsJson
        json <- template(parent)
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
          .through(Files[IO].writeUtf8(parent / "mkdocs.yml", Flags.Write))
          .compile
          .drain
      yield ()
