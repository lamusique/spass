package controllers

import java.nio.charset.Charset

import better.files.Dsl._
import better.files._
import play.api.i18n.Langs
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import play.api.{Configuration, Logger}
import play.mvc.Http.MimeTypes
import play.twirl.api.{Html, Xml}
import services.GreetingService
import utils.CodeUtility._

class RestMockController(greetingService: GreetingService,
                         langs: Langs,
                         config: Configuration,
                         cc: ControllerComponents) extends AbstractController(cc) with WebServiceController {

  private val logger = Logger(getClass)

  def doHttpMethods(path: String, extensionHint: String) = Action {implicit request =>

    val contentTypeToUse = contentType(request, extensionHint)
    val extensionToUse = contentTypeToUse.ext

    logger.debug(inspect(path))
    logger.debug(inspect(extensionToUse))
    logger.info(wrapForLogging("Request URI", request.method + " " + request.uri))

    matchAndReturn(path, contentTypeToUse, request)
  }

  def matchAndReturn(path: String, contentTypeToUse: ContentType, request: Request[AnyContent]) = {

    val maybeRootPath = config.getOptional[String]("spass.mapping.rootpath")
    val mappingDir = maybeRootPath.map(File(_)).getOrElse(cwd / "mapping")
    val method = request.method.toLowerCase
    logger.info(inspect(method))
    val restGetDir = mappingDir / "rest" / method

    // Assume a URI is /type[/type/..]/ID.
    val splitted = path.split("/")
    val resFileDir = splitted.dropRight(1).foldLeft(restGetDir)((z, n) => z / n)
    val file = resFileDir / (splitted.last + "." + contentTypeToUse.ext)

    contentTypeToUse match {
      case ContentType.XML => {
        if (file.exists) {
          val content = file.contentAsString
          logger.info(wrapForLogging("Response to put back", content))
          // TODO: POST to create an entity should have not Ok but Created.
          Ok(Xml(content))
        } else {
          val uri = request.uri
          logger.error("Your request doesn't match XML or JSON. URI: " + uri)
          Ok(Xml(s"""<xml><message><title>Doesn't mach</title><content>Your request doesn't match any files. URI=$uri</content></message></xml>"""))
        }
      }
      case ContentType.JSON => {
        if (file.exists) {
          val content = file.contentAsString
          logger.info(wrapForLogging("Response to put back", content))
          Ok(Json.parse(content))
        } else {
          val uri = request.uri
          logger.error("Your request doesn't match XML or JSON. URI: " + uri)
          Ok(Json.parse(s"""{"json": {"message": {"title": "Doesn't mach", "content": "Your GET request doesn't match any files. URI=$uri"}}}"""))
        }
      }
      case _ => {
        logger.error("Your request doesn't match XML or JSON. contentTypeToUse: " + contentTypeToUse)
        Ok("Your request doesn't match XML or JSON. contentTypeToUse: " + contentTypeToUse)
      }
    }
  }


  def contentType(request: Request[AnyContent], extensionHint: String) = {
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

  def contentType(request: Request[AnyContent]) = {
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
      }else if (request.accepts(MimeTypes.JSON)) {
        Some(ContentType.JSON)
      } else{
        Some(ContentType.Unknown)
      }
    }
  }


  def getXML(path: String) = doHttpMethods(path, ContentType.XML.ext)

  def postXML(path: String) = doHttpMethods(path, ContentType.XML.ext)

  def putXML(path: String) = doHttpMethods(path, ContentType.XML.ext)

  def deleteXML(path: String) = doHttpMethods(path, ContentType.XML.ext)


  def getJSON(path: String) = doHttpMethods(path, ContentType.JSON.ext)

  def postJSON(path: String) = doHttpMethods(path, ContentType.JSON.ext)

  def putJSON(path: String) = doHttpMethods(path, ContentType.JSON.ext)

  def deleteJSON(path: String) = doHttpMethods(path, ContentType.JSON.ext)


}
