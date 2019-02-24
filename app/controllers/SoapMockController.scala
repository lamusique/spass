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
        + "=" * 16 + Console.CYAN
        + xml.toString
        + "=" * 16 + Console.RESET)
      val trimmedReqXml = trimXml(xml.toString)
      logger.info("trimmed requested XML=" + trimmedReqXml)

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
      + "=" * 8 + " Requested " + "=" * 8 + Console.CYAN
      + xml.toString
      + "=" * 27 + Console.RESET)

      val trimmedReqXml = trimXml(xml.toString)
      logger.info("trimmed requested XML=" + trimmedReqXml)

      val maybeRootPath = config.getOptional[String]("spass.mapping.rootpath")
      val mappingDir = maybeRootPath.map(File(_)).getOrElse(cwd / "mapping")
      val soapDir = mappingDir / "soap"

      val allXMLReqs = (soapDir / "requests").list(_.extension == Some(".xml")).toSeq
      val matchedXMLReqs = allXMLReqs.filter(file => {
        val expectedXmlContent = file.contentAsString
        val trimmedExpectedXml = trimXml(expectedXmlContent)

        logger.info(inspect(trimmedReqXml))
        logger.info(inspect(trimmedExpectedXml))
        logger.info(inspect(trimmedReqXml == trimmedExpectedXml))
        trimmedReqXml == trimmedExpectedXml
      })
      logger.info(inspect(matchedXMLReqs.size))

      val matchedReqs = if (matchedXMLReqs.isEmpty) {
        // try to find by RegEx
        val allRegexReqs = (soapDir / "requests").list(_.extension == Some(".regex")).toSeq
        val matchedReqs = allRegexReqs.filter(file => {
        val expectedRegexContent = file.contentAsString

        logger.debug(inspect(expectedRegexContent))
        logger.debug(inspect(trimmedReqXml))
        val matches = trimmedReqXml.matches(expectedRegexContent)
        logger.debug(inspect(matches))
        matches
      })
        matchedReqs

      } else matchedXMLReqs

      val requestedFilename = matchedReqs.headOption match {
        case Some(file) => file.name.dropRight(file.extension.get.size - 1) + "xml"
        case None => "default.xml"
      }

      val allReses = (soapDir / "responses").list(_.extension == Some(".xml")).toSeq
      val matchedReses = allReses.filter(_.name == requestedFilename)
      logger.info(inspect(matchedReses.size))
      // Should find one file
      val resXmlFile = matchedReses.head

      logger.info("matched response XML:\n"
      + "=" * 8 + " To respond with " + "=" * 8 + Console.CYAN
      + resXmlFile.contentAsString
      + "=" * 27 + Console.RESET)

      Ok(Xml(resXmlFile.contentAsString))

    }.getOrElse {
      BadRequest("Expecting Xml data")
    }
  }

}
