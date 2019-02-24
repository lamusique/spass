package controllers

import java.nio.charset.Charset

import better.files.Dsl._
import better.files._
import play.api.i18n.Langs
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


  def get(path: String, ext: String = "xml") = Action {implicit request =>

    logger.debug(inspect(path))
    logger.debug(inspect(ext))

    val maybeRootPath = config.getOptional[String]("spass.mapping.rootpath")
    val mappingDir = maybeRootPath.map(File(_)).getOrElse(cwd / "mapping")
    val restGetDir = mappingDir / "rest" / "get"

    // Assume a URI is /type[/type/..]/ID.
    val splitted = path.split("/")
    val resFileDir = splitted.dropRight(1).foldLeft(restGetDir)((z, n) => z / n)
    val file = resFileDir / (splitted.last + ext)


    if(file.exists) {
      Ok(Xml(file.contentAsString))
    } else {
      Ok(Xml("<xml><method>get</method><desc>Your requested doesn't match any files.</desc></xml>"))
    }

  }

  def post(path: String) = Action {
    val sample = config.get[String]("spass.testing.sample")
    Created(Xml("<xml><method>post</method><desc>Your requested path is " + path +" and a config value is " + sample + ".</desc></xml>"))
  }

  def put(path: String) = Action {
    val sample = config.get[String]("spass.testing.sample")
    Ok(Xml("<xml><method>put</method><desc>Your requested path is " + path +" and a config value is " + sample + ".</desc></xml>"))
  }

  def delete(path: String) = Action {
    val sample = config.get[String]("spass.testing.sample")
    Ok(Xml("<xml><method>delete</method><desc>Your requested path is " + path +" and a config value is " + sample + ".</desc></xml>"))
  }


  def contentType(request: Request[AnyContent]) = {
      request.contentType.map(_.toLowerCase) match {
        case Some("application/json") | Some("text/json") => "JSON"
        case Some("application/xml") | Some("text/xml") => "XML"
        case _ => None
      }
  }
  def acceptType(request: Request[AnyContent]) = {
    if (request.accepts(MimeTypes.XML)) {
      // code
    }else if (request.accepts(MimeTypes.JSON)) {
      //code
    } else{
      //code
    }
  }


  def getXML(path: String) = get(path)

  def postXML(path: String) = post(path)

  def putXML(path: String) = put(path)

  def deleteXML(path: String) = delete(path)


  def getJSON(path: String) = get(path)

  def postJSON(path: String) = post(path)

  def putJSON(path: String) = put(path)

  def deleteJSON(path: String) = delete(path)


}
