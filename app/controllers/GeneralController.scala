package controllers

import models.Greeting
import play.api.{Configuration, Logger}
import play.api.i18n.Langs
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import play.twirl.api.Html
import services.GreetingService
import utils.CodeUtility._

class GeneralController(greetingService: GreetingService,
                        langs: Langs,
                        config: Configuration,
                        cc: ControllerComponents) extends AbstractController(cc) {

  private val logger = Logger(getClass)

  def index(path: String) = Action {
    logger.debug(inspect(path))
    val sample = config.get[String]("spass.testing.sample")
    Ok(Html("<h1>Welcome</h1><p>Your requested path is <code>" + path +"</code> and a config value is " + sample + ".</p>"))
  }

}
