package io.github.mwz.structurizr.mkdocs

import cats.effect.IO
import cats.syntax.apply.*
import cats.effect.kernel.Resource
import fs2.io.file.Files
import fs2.io.file.Path
import munit.CatsEffectSuite

import java.nio.file.Files as NioFiles

class AssetsTest extends CatsEffectSuite:
  val tmpSourceDir = ResourceTestLocalFixture("tmpSourceDir", Files[IO].tempDirectory)
  val tmpTargetDir = ResourceTestLocalFixture("tmpTargetDir", Files[IO].tempDirectory)

  def tmpFile(dir: Path, content: String): Resource[IO, Path] =
    Files[IO].createDirectories(dir).toResource *>
      Files[IO]
        .tempFile(Some(dir), "", ".tmp", None)
        .evalMap(path => IO(NioFiles.writeString(path.toNioPath, content)).as(path))

  override def munitFixtures = List(tmpSourceDir, tmpTargetDir)

  test("succeed if nothing to copy"):
    val source = tmpSourceDir()
    val target = tmpTargetDir()

    val copy = Assets(AssetsPathConfig(source.toNioPath, target.toNioPath)).copy()

    assertIO(
      copy *> IO.both(Files[IO].walk(source).compile.toList, Files[IO].walk(target).compile.toList),
      (List(source), List(target))
    )

  test("succeed if assets source directory does not exist"):
    val source = tmpSourceDir()
    val target = tmpTargetDir()
    val copy   = Assets(AssetsPathConfig((source / "does-not-exist").toNioPath, target.toNioPath)).copy()

    assertIO(
      copy *> Files[IO].walk(target).compile.toList,
      List(target)
    )

  test("copy files recursively and overwrite files"):
    val source = tmpSourceDir()
    val target = tmpTargetDir()

    tmpFile(source, "hello")
      .both(tmpFile(source / "subdir", "world"))
      .use { case (file1, file2) =>
        // copy files, overwrite file2 and copy again
        Assets(AssetsPathConfig(source.toNioPath, target.toNioPath)).copy() *>
          IO(NioFiles.writeString(file2.toNioPath, "asdf")) *>
          Assets(AssetsPathConfig(source.toNioPath, target.toNioPath)).copy() *>
          IO.both(Files[IO].walk(source).compile.toList, Files[IO].walk(target).compile.toList)
            .map { (sourceFiles, destFiles) =>
              // check if the same files present in source and destination dirs
              assertEquals(sourceFiles.map(source.relativize), destFiles.map(target.relativize))
            }
            .flatMap { _ =>
              val content =
                Files[IO].readUtf8(target.resolve(source.relativize(file1))) ++
                  Files[IO].readUtf8(target.resolve(source.relativize(file2)))
              // check if the content of the copied files are as expected
              assertIO(content.compile.toList, List("hello", "asdf"))
            }
      }
      .unsafeRunSync()
