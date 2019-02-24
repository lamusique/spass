package controllers

import java.nio.charset.Charset

trait WebServiceController {
  implicit val encoding = Charset.forName("UTF-8")

  def trimXml(xmlContent: String) = xmlContent
    .replaceAll(">\\s*<", "><").trim

  def wrapForLogging(title: String, content: String): String = {

    val header = "=" * 8 + " " + title + " " + "=" * 8

    ("\n" + Console.CYAN
    + header + "\n"
    + content + "\n"
    + "=" * header.size
    + Console.RESET)
  }
}
