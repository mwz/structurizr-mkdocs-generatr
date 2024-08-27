package io.github.mwz.structurizr.mkdocs

import com.structurizr.Workspace as SWorkspace
import munit.FunSuite

class MarkdownTest extends FunSuite:
  val IMAGE =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAERlWElmTU0AKgAAAAgAAYdpAAQAAAABAAAAGgAAAAAAA6ABAAMAAAABAAEAAKACAAQAAAABAAAACKADAAQAAAABAAAACAAAAACVhHtSAAAAvUlEQVQYGWNgAIL/DQxMIBoZ/P/PwAjmwyT//zfm+v/cKvH/C5uo//9DmUGSIEVgVf9f6HH//8F2iJGXzQgozMDw7e9OBpmTnoyMjP8hRv/liGBkZDH68vzPj49Pfv9i+MvszvDMwg9kClTBv/9v3/1jYGD8wwiy+c1HkPAvoFEwBbKsy3lYfxz58/M/O9Pfv2wCzJ82MUqf3wRSwAh2CCPQPf8dWBjuPA9g+Pf/D6P6rQ0gSQyfgARhACYJAFJ6Ton8Xte2AAAAAElFTkSuQmCC"

  test("escape should not change the string if no escapable characters are present"):
    val input          = "This is a test"
    val expectedOutput = "This is a test"
    val actualOutput   = markdown.escape(input)
    assertEquals(actualOutput, expectedOutput)

  test("escape should escape multiple characters in a string"):
    val input =
      """Multiple characters: \ ` * _ { } [ ] < > ( ) # + - . ! |
         /another line: \ ` * _ { } [ ] < > ( ) # + - . ! |"""
        .stripMargin('/')
    val expectedOutput =
      """Multiple characters: \\ \` \* \_ \{ \} \[ \] \< \> \( \) \# \+ \- \. \! \|
         /another line: \\ \` \* \_ \{ \} \[ \] \< \> \( \) \# \+ \- \. \! \|"""
        .stripMargin('/')
    val actualOutput = markdown.escape(input)
    assertEquals(actualOutput, expectedOutput)

  test("generate image tabs"):
    val md = markdown.imgTabs("img1", Some("Image 1"), Some("Description"))
    val expected = """Image 1
      |=== "SVG"
      |    <center>![img1SVG](../img/img1.svg)</center>
      |=== "PNG"
      |    <center>![img1PNG](../img/img1.png)</center>
      |<center>Description</center>
      |""".stripMargin
    assertEquals(md, expected)

  test("generate an embedded image"):
    val workspace = new SWorkspace("Name", "Test Workspace")
    val imgView   = workspace.getViews().createImageView("img")
    imgView.setTitle("Image 1")
    imgView.setDescription("Description")
    imgView.setContentType("image/png")
    imgView.setContent(IMAGE)
    val md = markdown.imageView(imgView)
    val expected = s"""<center>Image 1</center>
      |<center>![imgIMG](${IMAGE})</center>
      |<center>Description</center>
      |
      |""".stripMargin
    assertEquals(md, expected)
