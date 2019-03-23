package controllers

import java.nio.file.NoSuchFileException

import better.files.Dsl._
import better.files._
import com.typesafe.config.ConfigFactory
import play.api.i18n.Langs
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Logger}
import play.twirl.api.Xml
import utils.CodeUtility._

class ClassicFormController(
                         langs: Langs,
                         config: Configuration,
                         cc: ControllerComponents) extends AbstractController(cc) with WebServiceController {

  private val logger = Logger(getClass)


  def postForm(path: String, extensionHint: String = "xml", baseDir: String = "form") = Action {implicit request =>

    logger.debug(inspect(path))
    val queryStrings = request.queryString
    logger.debug(inspect(queryStrings))

    val contentTypeToUse = contentTypeOnAccept(request, extensionHint)
    val extensionToUse = contentTypeToUse.ext
    logger.debug(inspect(extensionToUse))
    logger.info(traceRequest("Received Request", request))

    // Content-Type: application/x-www-form-urlencoded
    // Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryEYbPWMhIwdu8veBy
    logger.debug(inspect(request.mediaType))


    // TODO how to determine a response content-type? extensionHint?

//    request.mediaType match {
//      case Some(mediaType) => mediaType.mediaSubType match {
//
//      }
//    }
//    request.contentType match {
//      case
//    }

    request.body.asFormUrlEncoded match {
      case Some(form) =>
        logger.info(wrapForLogging("Requested form", {
          form.map(entry => {
            val vs = entry._2.mkString(", ")
            entry._1 + ": " + vs
          }).mkString("\n")
        }))
        // TODO
        Ok("hello")
      case None =>
        logger.debug(inspect(request.body.asMultipartFormData))
        logger.error("No form found in POST. URI: " + request.uri)
        BadRequest("No form found in POST.")
    }
  }

}
