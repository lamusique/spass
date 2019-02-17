package controllers

import models.Greeting
import play.api.i18n.Langs
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import play.twirl.api.Html
import services.GreetingService

class GeneralController(greetingService: GreetingService,
                        langs: Langs,
                        cc: ControllerComponents) extends AbstractController(cc) {

  def index(path: String) = Action {
    Ok(Html("<h1>Welcome</h1><p>Your requested path is <code>" + path +"</code>.</p>"))
  }

}
