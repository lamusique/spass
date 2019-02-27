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
import java.nio.file.NoSuchFileException

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

  def mapXML(path: String) = Action { request =>
    request.body.asXml.map { xml =>

      logger.info(wrapForLogging("Requested XML", xml.toString))

      val trimmedReqXml = trimXml(xml.toString)
      logger.info("trimmed requested XML=" + trimmedReqXml)

      val maybeRootPath = config.getOptional[String]("spass.mapping.rootpath")
      val mappingDir = maybeRootPath.map(File(_)).getOrElse(cwd / "mapping")
      val soapDir = mappingDir / "soap"


      // Assume a URI is not /type[/type/..]/ID but /type[/type/..]
      val splitted = path.split("/")
      val soapStructuredDir = splitted.foldLeft(soapDir)((z, n) => z / n)

      try {

      val allXMLReqs = (soapStructuredDir / "requests").list(_.extension == Some(".xml")).toSeq
      val matchedXMLReqs = allXMLReqs.filter(file => {
        val expectedXmlContent = file.contentAsString
        val trimmedExpectedXml = trimXml(expectedXmlContent)

        logger.debug(inspect(trimmedReqXml))
        logger.debug(inspect(trimmedExpectedXml))
        logger.debug(inspect(trimmedReqXml == trimmedExpectedXml))
        trimmedReqXml == trimmedExpectedXml
      })
      logger.debug(inspect(matchedXMLReqs.size))

      val matchedReqs = if (matchedXMLReqs.isEmpty) {
        // try to find by RegEx
        logger.debug("Exact matching failed and then try to find an expectation by RegEx.")
        val allRegexReqs = (soapStructuredDir / "requests").list(_.extension == Some(".regex")).toSeq
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

      val allReses = (soapStructuredDir / "responses").list(_.extension == Some(".xml")).toSeq
      val matchedReses = allReses.filter(_.name == requestedFilename)
      logger.debug(inspect(matchedReses.size))
      // Should find one file
      val resXmlFile = matchedReses.head
      val content = resXmlFile.contentAsString
      logger.info(wrapForLogging("Response to put back", content))
      Ok(Xml(content))

      } catch {
        case nsfe: NoSuchFileException =>  {
          logger.error("Not Found: Your requested URI can't find a necessary file to respond.", nsfe)
          NotFound("Not Found: Your requested URI can't find a necessary file to respond. URI: " + request.uri)
        }
        case e: Exception => throw e
      }


    }.getOrElse {
      BadRequest("Expecting Xml data")
    }
  }

}
