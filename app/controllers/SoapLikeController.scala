package controllers

import play.api.Configuration
import play.api.i18n.Langs
import play.api.mvc.{AbstractController, ControllerComponents}
import play.twirl.api.Html
import play.twirl.api.Xml
import services.GreetingService

class SoapLikeController(greetingService: GreetingService,
                         langs: Langs,
                         config: Configuration,
                         cc: ControllerComponents) extends AbstractController(cc) {

  def talkOnXml = Action { request =>
    request.body.asXml.map { xml =>
      (xml \\ "name" headOption).map(_.text).map { name =>
        Ok(Xml("<xml><title>Welcome</title><content>You requested XML file and its name is " + name + "</content></xml>"))
      }.getOrElse {
        BadRequest("Missing parameter [name]")
      }
    }.getOrElse {
      BadRequest("Expecting Xml data")
    }
  }

}
