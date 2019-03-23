package controllers

import models.Greeting
import play.api.Logger
import play.api.i18n.Langs
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import play.twirl.api.Html
import services.GreetingService
import utils.CodeUtility._

class GreeterController(greetingService: GreetingService,
                        langs: Langs,
                        cc: ControllerComponents) extends AbstractController(cc) {

  private val logger = Logger(getClass)

  val greetingsList = Seq(
    Greeting(1, greetingService.greetingMessage("en"), "sameer"),
    Greeting(2, greetingService.greetingMessage("it"), "sam")
  )

  def greetings = Action {
    Ok(Json.toJson(greetingsList))
  }

  def greetInMyLanguage = Action {
    Ok(greetingService.greetingMessage(langs.preferred(langs.availables).language))
  }

  def index = Action {
    Ok(Html("<h1>Welcome</h1><p>Your new application is ready.</p>"))
  }

  def parameter(param: String) = Action {
    logger.info(inspect(param))
    Ok(greetingService.greetingMessage(langs.preferred(langs.availables).language)
      + " with " + param)
  }

}
