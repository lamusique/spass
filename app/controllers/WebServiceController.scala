package controllers

import java.nio.charset.Charset

import play.api.Logger
import play.api.libs.json.JsObject
import play.api.mvc.{AnyContent, Request}
import play.api.libs.json._
import play.mvc.Http.MimeTypes

trait WebServiceController {
  private val logger = Logger(getClass)
  implicit val encoding = Charset.forName("UTF-8")

  sealed trait ContentType {
    val code: Int
    val ext: Option[String]
    val contentTypeValue: String
  }
  object ContentType {
    val extensionMapping: Map[String, ContentType] = Map(
      XML.ext.get -> XML,
      JSON.ext.get -> JSON
    )

    def get(extension: String) = extensionMapping.get(extension)

    case object OctetStream extends ContentType {
      override val code = 0
      override val ext = None
      override val contentTypeValue = "application/octet-stream"
    }

    case object XML extends ContentType {
      override val code = 1
      override val ext = Some("xml")
      override val contentTypeValue = "application/xml"
    }

    case object JSON extends ContentType {
      override val code = 2
      override val ext = Some("json")
      override val contentTypeValue: String = "application/json"
    }

    case object FormUrlEncoded extends ContentType {
      override val code = 3
      override val ext = None
      override val contentTypeValue: String = "application/x-www-form-urlencoded"
    }

  }

//  def trimXml(xmlContent: String) = xmlContent
//    .replaceAll(">\\s*<", "><").trim

  def trimXML(xmlContent: String) = {
    val xml = scala.xml.XML.loadString(xmlContent)
    val trimmed = scala.xml.Utility.trim(xml)
    trimmed.toString
  }
  def trimJSON(jsonContent: String) = {
      val json = Json.parse(jsonContent).as[JsObject]
      json.toString
  }


  def wrapForLogging(title: String, content: String): String = {

    val header = "=" * 8 + " " + title + " " + "=" * 8

    ("\n" + Console.CYAN
      + header + "\n"
      + content + "\n"
      + "=" * header.size
      + Console.RESET)
  }

  def traceRequest(title: String, request: Request[AnyContent]) = {
    wrapForLogging("Received Request", {

      val uri = "URI: " + request.method + " " + request.uri

      (
        uri + "\n"
          + "-" * uri.size + "\n"
          + request.headers.toSimpleMap.map(e => e._1 + ": " + e._2).mkString("\n")
        )

    })
  }

  def contentTypeOnContentType(request: Request[AnyContent]) = {
    request.contentType.map(_.toLowerCase) match {
      case Some("application/json") | Some("text/json") => Some(ContentType.JSON)
      case Some("application/xml") | Some("text/xml") => Some(ContentType.XML)
      case _ =>
        logger.warn("No recognisable content type specified in a request header. contentType: "
          + request.contentType)
        None
    }
  }

  // This is for RESTful.
  def contentTypeOnAccept(request: Request[AnyContent], extensionHint: String) = {
    // Hint is prior to Accept.

    Option(extensionHint) match {
      case Some(contentTypeHint) => {
        ContentType.get(extensionHint).getOrElse(ContentType.XML)
      }
      case None => {
        acceptType(request) match {
          case Some(ContentType.XML) => ContentType.XML
          case Some(ContentType.JSON) => ContentType.JSON
          case _ =>
            logger.warn("No acceptable content types specified in Accept in a request header so let XML for this request's content. acceptedTypes: "
              + request.acceptedTypes)
            ContentType.XML
        }
      }
    }
  }

  def acceptType(request: Request[AnyContent]) = {
    if (request.acceptedTypes.isEmpty) {
      None
    } else {
      if (request.accepts(MimeTypes.XML)) {
        Some(ContentType.XML)
      } else if (request.accepts(MimeTypes.JSON)) {
        Some(ContentType.JSON)
      } else {
        logger.debug("No recognisable accept types. acceptedTypes: "
          + request.acceptedTypes)
        None
      }
    }
  }

}
