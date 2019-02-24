package controllers

import java.nio.charset.Charset

trait WebServiceController {
  implicit val encoding = Charset.forName("UTF-8")

  sealed trait ContentType {
    val code: Int
    val ext: String
    val contentTypeValue: String
  }
  object ContentType {
    val extensionMapping = Map(
      XML.ext -> XML,
      JSON.ext -> JSON
    )

    def get(extension: String) = extensionMapping.get(extension)

    case object Unknown extends ContentType {
      override val code: Int = 0
      override val ext: String = "unknown"
      override val contentTypeValue: String = "application/octet-stream"
    }

    case object XML extends ContentType {
      override val code: Int = 1
      override val ext: String = "xml"
      override val contentTypeValue: String = "application/xml"
    }

    case object JSON extends ContentType {
      override val code: Int = 2
      override val ext: String = "json"
      override val contentTypeValue: String = "application/xml"
    }

  }

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
