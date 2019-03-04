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

  def doHttpMethodWithBody(path: String, extensionHint: String) = Action {implicit request =>

    logger.debug(inspect(path))
    val contentTypeToUse = contentTypeOnAccept(request, extensionHint)
    val extensionToUse = contentTypeToUse.ext
    logger.debug(inspect(extensionToUse))
    logger.info(traceRequest("Received Request", request))

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
          logger.debug("A matched response file: " + file)
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
          logger.debug("A matched response file: " + file)
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


  // Read/SELECT
  // def getJSON(path: String) = doHttpMethods(path, ContentType.JSON.ext)

  // Create/INSERT, not idempotent
  def postJSON(path: String) = doHttpMethodWithBody(path, ContentType.JSON.ext)

  // INSERT UPDATE or REPLACE, idempotent
  // PUT /collection/id
  def putJSON(path: String) = doHttpMethodWithBody(path, ContentType.JSON.ext)

  // UPDATE
  // PATCH /collection/id
  def patchJSON(path: String) = doHttpMethodWithBody(path, ContentType.XML.ext)

  // DELETE
  // DELETE /collection
  // DELETE /collection/id
  def deleteJSON(path: String) = doHttpMethodWithBody(path, ContentType.JSON.ext)


  def postXML(path: String) = doHttpMethodWithBody(path, ContentType.XML.ext)
  def putXML(path: String) = doHttpMethodWithBody(path, ContentType.XML.ext)
  def patchXML(path: String) = doHttpMethodWithBody(path, ContentType.XML.ext)
  def deleteXML(path: String) = doHttpMethodWithBody(path, ContentType.XML.ext)

}
