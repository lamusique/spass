package controllers

import play.api.{Configuration, Logger}
import play.api.i18n.Langs
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
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


  def mapXML(path: String, soapVersion: Option[Double]) = Action { request =>
    request.body.asXml.map { xml =>

      logger.info(traceRequest("Received Request", request))
      logger.info(wrapForLogging("Requested XML", xml.toString))

      val maybeSoapVersion = detectSoapVersion(request, soapVersion)
      logger.info(inspect(maybeSoapVersion))

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
      // Should find one file.
      val resXmlFile = matchedReses.headOption match {
        case Some(head) => head
        case None => throw new NoSuchFileException("No matched response files.")
      }

      logger.debug(inspect(resXmlFile))
      val content = resXmlFile.contentAsString
      logger.info(wrapForLogging("Response to put back", content))

      if (soapPattern.matcher(content).matches) {
        logger.debug("A response file recognised as a SOAP format.")
        logger.info("A SOAP response recognised.")
        maybeSoapVersion match {
          case Some(soapVersion) =>  Ok(Xml(content)).as(soapVersion.contentTypeValue)
          case None =>
            logger.warn("A SOAP response file found but a request is not SOAP.")
            logger.warn("Treat a response as SOAP 1.2 for this time.")
            Ok(Xml(content)).as(SoapVersion.OnePointTwo.contentTypeValue)
        }
      } else {
        // A not SOAP response is to be with application/xml.
        logger.debug("A response file not recognised as a SOAP format.")
        logger.info("A not SOAP response recognised so as a mere XML.")
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

  def detectSoapVersion(request: Request[AnyContent], soapVersionNumber: Option[Double]) = {

    val maybeSoapVersionSpecified = soapVersionNumber match {
      case Some(versionNumber) =>
        SoapVersion.get(versionNumber) match {
          case Some(version) => Some(version)
          case None =>
            logger.warn("soapVersionNumber specified but out of recognisation. soapVersionNumber: "
              + soapVersionNumber)
            None
        }
      case None => None
    }

//    SOAP 1.1
//    POST /services/SoapService HTTP/1.1
//    SOAPAction: "prefix:sampleAction"
//    Content-Type: text/xml

//    SOAP 1.2
//    POST /services/SoapService HTTP/1.1
//    Content-Type: application/soap+xml; charset=utf-8; action="prefix:sampleAction"

    maybeSoapVersionSpecified match {
      case Some(version) => Some(version)
      case None => {
        if (request.headers.hasHeader("SOAPAction")) {
          // assume 1.1
          request.contentType match {
            case Some(contentType) =>
              if (contentType.contains("text/xml")) {
                Some(SoapVersion.OnePointOne)
              } else {
                logger.warn("SOAPAction as SOAP 1.1 specified but no Content-Type "
                  + SoapVersion.OnePointOne.contentTypeValue + " as 1.1. contentType: "
                  + contentType)
                None
              }
            case None =>
              logger.warn("SOAPAction as SOAP 1.1 specified but no Content-Type.")
              None
          }
        } else {
          // assume 1.2
          request.contentType match {
            case Some(contentType) =>
              if (contentType.contains(SoapVersion.OnePointTwo.contentTypeValue)) {
                Some(SoapVersion.OnePointTwo)
              } else {
                logger.warn("No SOAPAction specified as SOAP 1.2 but no Content-Type "
                  + SoapVersion.OnePointTwo.contentTypeValue + " as 1.2. contentType: "
                  + contentType)
                None
              }
            case None =>
              logger.warn("No SOAPAction specified as SOAP 1.2 but no Content-Type specified.")
              None
          }
        }
      }
    }

  }

  sealed trait SoapVersion {
    val version: Double
    val contentTypeValue: String

    override def toString: String =
      "SOAP version " + version +
        ", Content-Type: " + contentTypeValue
  }
  object SoapVersion {
    val versionMapping = Map(
      OnePointOne.version -> OnePointOne,
      OnePointTwo.version -> OnePointTwo
    )
    case object OnePointOne extends SoapVersion {
      override val version = 1.1
      override val contentTypeValue = "text/xml"
    }
    case object OnePointTwo extends SoapVersion {
      override val version = 1.2
      override val contentTypeValue = "application/soap+xml"
    }
    def get(versionNumber: Double): Option[SoapVersion] = versionMapping.get(versionNumber)
  }

}
