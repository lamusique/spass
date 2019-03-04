package controllers

import java.nio.file.NoSuchFileException

import better.files.Dsl._
import better.files._
import play.api.i18n.Langs
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, ControllerComponents, Request}
import play.api.{Configuration, Logger}
import play.twirl.api.Xml
import utils.CodeUtility._

class RestConditionalMockController(
                         langs: Langs,
                         config: Configuration,
                         cc: ControllerComponents) extends ClassicUriController(langs, config, cc) with WebServiceController {

  private val logger = Logger(getClass)

  // for POST/PUT/PATCH or even DELETE
  def doHttpMethodWithBody(path: String, extensionHint: String) = Action {implicit request =>

    val contentTypeToUse = contentTypeOnAccept(request, extensionHint)
    val extensionToUse = contentTypeToUse.ext

    logger.debug(inspect(path))
    logger.debug(inspect(extensionToUse))
    logger.info(traceRequest("Received Request", request))

    val maybeTrimmedRequestBody = contentTypeToUse match {
      case ContentType.XML => request.body.asXml match {
        case Some(xml) => Option(trimXML(xml.toString))
        case None => None
      }
      // Expect JSON in Accept and Content-Type.
      case ContentType.JSON => request.body.asJson match {
        case Some(json) => Option(trimJSON(json.toString))
        case None => None
      }
      case _ => throw new RuntimeException("A content type is out of use. contentTypeToUse: " + contentTypeToUse)
    }

    matchAndReturn(path, contentTypeToUse, maybeTrimmedRequestBody, request)
  }

  def matchAndReturn(path: String, contentTypeToUse: ContentType, maybeTrimmedRequestBody: Option[String], request: Request[AnyContent]) = {

    val maybeRootPath = config.getOptional[String]("spass.mapping.rootpath")
    val mappingDir = maybeRootPath.map(File(_)).getOrElse(cwd / "mapping")
    val method = request.method.toLowerCase
    logger.info(inspect(method))
    val restGetDir = mappingDir / "rest-cond" / method

    // Assume a URI is /type[/type/..]/ID.
    val splitted = path.split("/")
    val restFileDir = splitted.foldLeft(restGetDir)((z, n) => z / n)

    // TODO remove duplication with Soap
    try {

      val allReqs = (restFileDir / "requests").list(_.extension == Some("." + contentTypeToUse.ext)).toSeq

      val exactlyMatchedReqs = allReqs.filter(file => {
        val expectedContent = file.contentAsString
        val trimmedExpected = contentTypeToUse match {
          case ContentType.XML => trimXML(expectedContent)
          case ContentType.JSON => trimJSON(expectedContent)
          case _ => throw new RuntimeException("A content type is out of use. contentTypeToUse: " + contentTypeToUse)
        }
        logger.debug(inspect(maybeTrimmedRequestBody))
        logger.debug(inspect(trimmedExpected))

        maybeTrimmedRequestBody match {
          case Some(trimmedRequestBody) => trimmedRequestBody == trimmedExpected
          case None => false
        }
      })
      logger.debug(inspect(exactlyMatchedReqs.size))

      val matchedReqs = if (exactlyMatchedReqs.isEmpty) {
        // try to find by RegEx
        logger.debug("Exact matching failed and then try to find an expectation by RegEx.")
        val allRegexReqs = (restFileDir / "requests").list(_.extension == Some(".regex")).toSeq
        val matchedReqs = allRegexReqs.filter(file => {
          val expectedRegexContent = file.contentAsString

          logger.debug(inspect(expectedRegexContent))
          logger.debug(inspect(maybeTrimmedRequestBody))

          maybeTrimmedRequestBody match {
            case Some(trimmedRequestBody) =>
              // Unnecessary to regex with DOTALL due to trimmed all whitespaces and line breaks.
              val matches = trimmedRequestBody.matches(expectedRegexContent)
              logger.debug(inspect(matches))
              matches
            case None => false
          }
        })
        matchedReqs

      } else exactlyMatchedReqs

      val requestedFilename = matchedReqs.headOption match {
        case Some(matchedReqFile) =>
          logger.debug(inspect(matchedReqFile))
          matchedReqFile.name.dropRight(matchedReqFile.extension.get.size - 1) + contentTypeToUse.ext
        case None => "default" + contentTypeToUse.ext
      }
      logger.debug(inspect(requestedFilename))

      val allReses = (restFileDir / "responses").list(_.extension == Some("." + contentTypeToUse.ext)).toSeq
      val matchedReses = allReses.filter(_.name == requestedFilename)
      logger.debug(inspect(matchedReses.size))
      // Basically a user should put at least one file but non-existence is possible.
      matchedReses.headOption match {
        case Some(resFile) => {
          logger.debug(inspect(resFile))
          val content = resFile.contentAsString
          logger.info(wrapForLogging("Response to put back", content))

          contentTypeToUse match {
            case ContentType.XML => Ok(Xml(content))
            case ContentType.JSON => Ok(Json.parse(content))
            case _ => throw new RuntimeException("A content type is out of use. contentTypeToUse: " + contentTypeToUse)
          }
        }
        case None => NotFound("No response file found.")
      }

    } catch {
      case nsfe: NoSuchFileException =>  {
        logger.error("Not Found: Your requested URI can't find a necessary file to respond.", nsfe)
        NotFound("Not Found: Your requested URI can't find a necessary file to respond. URI: " + request.uri)
      }
      case e: Exception => throw e
    }

  }


  // Read/SELECT
  //def getJSON(path: String) = classicGetWithStructure(path, ContentType.JSON.ext, "rest")

  // Create/INSERT, not idempotent
  def postJSON(path: String) = doHttpMethodWithBody(path, ContentType.JSON.ext)

  // INSERT UPDATE or REPLACE, idempotent
  // PUT /collection/id
  def putJSON(path: String) = doHttpMethodWithBody(path, ContentType.JSON.ext)

  // UPDATE
  // PATCH /collection/id
  def patchJSON(path: String) = doHttpMethodWithBody(path, ContentType.XML.ext)

  // DELETE
  // DELETE /collection
  // DELETE /collection/id
  def deleteJSON(path: String) = doHttpMethodWithBody(path, ContentType.JSON.ext)


  def postXML(path: String) = doHttpMethodWithBody(path, ContentType.XML.ext)
  def putXML(path: String) = doHttpMethodWithBody(path, ContentType.XML.ext)
  def patchXML(path: String) = doHttpMethodWithBody(path, ContentType.XML.ext)
  def deleteXML(path: String) = doHttpMethodWithBody(path, ContentType.XML.ext)

}
