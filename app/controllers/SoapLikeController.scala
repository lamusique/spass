package controllers

import play.api.{Configuration, Logger}
import play.api.i18n.Langs
import play.api.mvc.{AbstractController, ControllerComponents}
import play.twirl.api.Html
import play.twirl.api.Xml
import services.GreetingService
import better.files._
import java.io.{File => JFile}
import java.nio.charset.Charset

import better.files.Dsl._

import utils.CodeUtility._

class SoapLikeController(greetingService: GreetingService,
                         langs: Langs,
                         config: Configuration,
                         cc: ControllerComponents) extends AbstractController(cc) {

  private val logger = Logger(getClass)

  implicit val encoding = Charset.forName("UTF-8")

  def talkOnXml = Action { request =>
    request.body.asXml.map { xml =>

      logger.info("xml=" + xml.toString)
      logger.info("trimmed xml=" + xml.toString.replaceAll(">\\s*<", "><"))

      (xml \\ "name" headOption).map(_.text).map { name =>
        Ok(Xml("<xml><title>Welcome</title><content>You requested XML file and its name is " + name + "</content></xml>"))
      }.getOrElse {
        BadRequest("Missing parameter [name]")
      }
    }.getOrElse {
      BadRequest("Expecting Xml data")
    }
  }

  def mapXml = Action { request =>
    request.body.asXml.map { xml =>

      logger.info("xml=" + xml.toString)
      val trimmedReqXml = trimXml(xml.toString)
      logger.info("trimmed xml=" + trimmedReqXml)

      val maybeRootPath = config.getOptional[String]("spass.mapping.rootpath")
      val mappingDir = maybeRootPath.map(File(_)).getOrElse(cwd / "mapping")

      val allReqs = (mappingDir / "xml" / "requests").list(_.extension == Some(".xml")).toSeq
      val matchedReqs = allReqs.filter(file => {
        val expectedXmlContent = file.contentAsString
        val trimmedExpectedXml = trimXml(expectedXmlContent)

        logger.info(inspect(trimmedReqXml))
        logger.info(inspect(trimmedExpectedXml))
        logger.info(inspect(trimmedReqXml == trimmedExpectedXml))
        trimmedReqXml == trimmedExpectedXml
      })
      logger.info(inspect(matchedReqs.size))

      val requestedFilename = matchedReqs.headOption match {
        case Some(file) => file.name
        case None => "default.xml"
      }

      val allReses = (mappingDir / "xml" / "responses").list(_.extension == Some(".xml")).toSeq
      val matchedReses = allReses.filter(_.name == requestedFilename)
      logger.info(inspect(matchedReses.size))
      // Should find one file
      val resXmlFile = matchedReses.head

//      val resXmlFile = matchedReqs.headOption match {
//        case Some(file) => {
//          val requestedFilename = file.name
//          val allReses = (mappingDir / "xml" / "responses").list(_.extension == Some(".xml")).toSeq
//          val matchedReses = allReses.filter(_.name == requestedFilename)
//          logger.info(inspect(matchedReses.size))
//          matchedReses.headOption match {
//            case Some(file) => file
//            case None => mappingDir / "xml" / "responses" / "default.xml"
//          }
//        }
//        case None => mappingDir / "xml" / "responses" / "default.xml"
//      }

      Ok(Xml(resXmlFile.contentAsString))

    }.getOrElse {
      BadRequest("Expecting Xml data")
    }
  }

  def trimXml(xmlContent: String) = xmlContent
    .replaceAll(">\\s*<", "><")
    .replaceAll("\\A\\s*", "")
    .replaceAll("\\s*\\Z", "")

}
