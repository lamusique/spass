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
import java.util.regex.Pattern

import better.files.Dsl._
import utils.CodeUtility._

class SoapMockController(greetingService: GreetingService,
                         langs: Langs,
                         config: Configuration,
                         cc: ControllerComponents) extends AbstractController(cc) with WebServiceController {

  private val logger = Logger(getClass)

  // SOAP XML has e.g.
  // <someprefix:Envelope xmlns:someprefix="http://www.w3.org/2003/05/soap-envelope" xmlns:anothertag="http://www.example.org">
  // cf. https://www.w3.org/TR/soap/
  val soapPattern = Pattern.compile(raw".*<[^>]*Envelope[^>]*xmlns[^=]*=[^>]*>.*", Pattern.DOTALL)


  def mapXML(path: String) = Action { request =>
    request.body.asXml.map { xml =>

      logger.info(traceRequest("Received Request", request))
      logger.info(wrapForLogging("Requested XML", xml.toString))

      val trimmedReqXml = trimXML(xml.toString)
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
        val trimmedExpectedXml = trimXML(expectedXmlContent)

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

        // Unnecessary to regex with DOTALL due to trimmed all whitespaces and line breaks.
        val matches = trimmedReqXml.matches(expectedRegexContent)
        logger.debug(inspect(matches))
        matches
      })
        matchedReqs

      } else matchedXMLReqs

      val requestedFilename = matchedReqs.headOption match {
        case Some(matchedReqFile) =>
          logger.debug(inspect(matchedReqFile))
          matchedReqFile.name.dropRight(matchedReqFile.extension.get.size - 1) + "xml"
        case None => "default.xml"
      }

      val allReses = (soapStructuredDir / "responses").list(_.extension == Some(".xml")).toSeq
      val matchedReses = allReses.filter(_.name == requestedFilename)
      logger.debug(inspect(matchedReses.size))
      // Should find one file
      val resXmlFile = matchedReses.head
      logger.debug(inspect(resXmlFile))
      val content = resXmlFile.contentAsString
      logger.info(wrapForLogging("Response to put back", content))

      if (soapPattern.matcher(content).matches) {
        // Content-Type of SOAP response of Spring WS only allows text/xml, not application/soap+xml.
        logger.debug("SOAP response recognised.")
        Ok(Xml(content)).as("text/xml")
      } else {
        // Not SOAP response is to be with application/xml.
        logger.debug("Not SOAP response recognised.")
        Ok(Xml(content))
      }

      } catch {
        case nsfe: NoSuchFileException =>  {
          logger.error("Not Found: Your requested URI can't find a necessary file to respond.", nsfe)
          NotFound("Not Found: Your requested URI can't find a necessary file to respond. URI: " + request.uri)
        }
        case e: Exception => throw e
      }


    }.getOrElse {
      BadRequest("Expecting XML data")
    }
  }

}
