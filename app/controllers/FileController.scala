package controllers

import better.files._
import java.io.{File => JFile}

import better.files.Dsl._
import play.api.i18n.Langs
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Logger}
import utils.CodeUtility._

import scala.concurrent.ExecutionContext

class FileController(
                         langs: Langs,
                         config: Configuration,
                         cc: ControllerComponents,
                         ec: ExecutionContext) extends AbstractController(cc) with WebServiceController {

  private val logger = Logger(getClass)
  implicit val impEc = ec

  def download(path: String, extensionHint: String = "xml", baseDir: String = "file") = Action { implicit request =>
//  def download(path: String, inline: Boolean = true, extensionHint: String = "xml", baseDir: String = "file") = Action { implicit request =>
    // TODO
    //Ok("downloads.")
    //Ok.sendFile((pwd / "mapping" / "file" / "get" / "wiles" / "Ring-theoretic properties of certain Hecke algebras.pdff").toJava, inline = true)
    Ok.sendFile((pwd / "mapping" / "file" / "get" / "wiles" / "responses" / "001.json").toJava)

  }

  def upload(path: String, extensionHint: String = "xml", baseDir: String = "file") = Action { implicit request =>
    // TODO
    Ok("uploads.")
  }

}
