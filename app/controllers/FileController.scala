package controllers

import play.api.i18n.Langs
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Logger}
import utils.CodeUtility._

class FileController(
                         langs: Langs,
                         config: Configuration,
                         cc: ControllerComponents) extends AbstractController(cc) with WebServiceController {

  private val logger = Logger(getClass)

  def download(path: String, extensionHint: String = "xml", baseDir: String = "file") = Action { implicit request =>
    // TODO
    Ok("downloads.")
  }

  def upload(path: String, extensionHint: String = "xml", baseDir: String = "file") = Action { implicit request =>
    // TODO
    Ok("uploads.")
  }

}
