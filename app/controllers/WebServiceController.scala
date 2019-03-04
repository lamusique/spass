package controllers

import java.nio.charset.Charset

import com.fasterxml.jackson.core.JsonParseException
import play.api.libs.json.JsObject
import play.api.mvc.{AnyContent, Request}
//import play.libs.Json
import play.api.libs.json._
import play.mvc.Http.MimeTypes

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
//  def trimJSON(jsonContent: String) = {
//    try {
//      val json = Json.parse(jsonContent).as[JsObject]
//      Option(json.toString)
//    } catch {
//      case jpe: JsonParseException => None
//      case e: Exception => throw e;
//    }
//  }


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

      val uri = "URI :" + request.method + " " + request.uri

      (
        uri + "\n"
          + "-" * uri.size + "\n"
          + request.headers.toSimpleMap.map(e => e._1 + ": " + e._2).mkString("\n")
        )

    })
  }

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
          case _ =>  ContentType.XML
        }
      }
    }
  }

  def contentTypeOnContentType(request: Request[AnyContent]) = {
    request.contentType.map(_.toLowerCase) match {
      case Some("application/json") | Some("text/json") => "JSON"
      case Some("application/xml") | Some("text/xml") => "XML"
      case _ => None
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
      } else{
        Some(ContentType.Unknown)
      }
    }
  }


}
