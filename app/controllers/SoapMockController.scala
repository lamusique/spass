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

class SoapMockController(greetingService: GreetingService,
                         langs: Langs,
                         config: Configuration,
                         cc: ControllerComponents) extends AbstractController(cc) with WebServiceController {

  private val logger = Logger(getClass)


  def talkOnXml = Action { request =>
    request.body.asXml.map { xml =>

      logger.info("requested XML:\n"
        + "=" * 16
        + xml.toString
        + "=" * 16)
      logger.info("trimmed requested XML=" + xml.toString.replaceAll(">\\s*<", "><"))

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

      logger.info("requested XML:\n"
        + "=" * 16
        + xml.toString
        + "=" * 16)

      val trimmedReqXml = trimXml(xml.toString)
      logger.info("trimmed requested XML=" + trimmedReqXml)

      val maybeRootPath = config.getOptional[String]("spass.mapping.rootpath")
      val mappingDir = maybeRootPath.map(File(_)).getOrElse(cwd / "mapping")
      val soapDir = mappingDir / "soap"

      val allReqs = (soapDir / "requests").list(_.extension == Some(".xml")).toSeq
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

      val allReses = (soapDir / "responses").list(_.extension == Some(".xml")).toSeq
      val matchedReses = allReses.filter(_.name == requestedFilename)
      logger.info(inspect(matchedReses.size))
      // Should find one file
      val resXmlFile = matchedReses.head

      logger.info("matched response XML:\n"
        + "=" * 16
        + resXmlFile.contentAsString
        + "=" * 16)

      Ok(Xml(resXmlFile.contentAsString))

    }.getOrElse {
      BadRequest("Expecting Xml data")
    }
  }

  def trimXml(xmlContent: String) = xmlContent
    .replaceAll(">\\s*<", "><").trim

}
