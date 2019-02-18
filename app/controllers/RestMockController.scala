package controllers

import java.nio.charset.Charset

import better.files.Dsl._
import better.files._
import play.api.i18n.Langs
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Logger}
import play.twirl.api.{Html, Xml}
import services.GreetingService
import utils.CodeUtility._

class RestMockController(greetingService: GreetingService,
                         langs: Langs,
                         config: Configuration,
                         cc: ControllerComponents) extends AbstractController(cc) {

  private val logger = Logger(getClass)

  implicit val encoding = Charset.forName("UTF-8")

  def get(path: String) = Action {
    val sample = config.get[String]("spass.testing.sample")
    Ok(Html("<h1>get</h1><p>Your requested path is <code>" + path +"</code> and a config value is " + sample + ".</p>"))
  }

  def post(path: String) = Action {
    val sample = config.get[String]("spass.testing.sample")
    Created(Html("<h1>post</h1><p>Your requested path is <code>" + path +"</code> and a config value is " + sample + ".</p>"))
  }

  def put(path: String) = Action {
    val sample = config.get[String]("spass.testing.sample")
    Ok(Html("<h1>put</h1><p>Your requested path is <code>" + path +"</code> and a config value is " + sample + ".</p>"))
  }

  def delete(path: String) = Action {
    val sample = config.get[String]("spass.testing.sample")
    Ok(Html("<h1>delete</h1><p>Your requested path is <code>" + path +"</code> and a config value is " + sample + ".</p>"))
  }

}
