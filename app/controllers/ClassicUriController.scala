package controllers

import java.nio.charset.Charset
import java.nio.file.NoSuchFileException

import better.files.Dsl._
import better.files._
import com.typesafe.config.ConfigFactory
import play.api.i18n.Langs
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import play.api.{Configuration, Logger}
import play.twirl.api.Xml
import utils.CodeUtility._

class ClassicUriController(
                         langs: Langs,
                         config: Configuration,
                         cc: ControllerComponents) extends AbstractController(cc) with WebServiceController {

  private val logger = Logger(getClass)

  def classicGet() = classicGetWithStructure("")

  def classicGetWithStructure(path: String, extensionHint: String = "xml", baseDir: String = "classic") = Action {implicit request =>

    logger.debug(inspect(path))
    val queryStrings = request.queryString
    logger.debug(inspect(queryStrings))
    // ?aaa=123&bbb=222&aaa=111
    // Map(aaa -> Vector(123, 111), bbb -> Vector(222))
    val contentTypeToUse = contentTypeOnAccept(request, extensionHint)
    val extensionToUse = contentTypeToUse.ext
    logger.debug(inspect(extensionToUse))
    logger.info(traceRequest("Received Request", request))


    val maybeRootPath = config.getOptional[String]("spass.mapping.rootpath")
    val mappingDir = maybeRootPath.map(File(_)).getOrElse(cwd / "mapping")
    val classicGetDir = mappingDir / baseDir / "get"

    // Assume a URI is not /type[/type/..]/ID but /type[/type/..]
    val splitted = path.split("/")
    val classicGetFileDir = splitted.foldLeft(classicGetDir)((z, n) => z / n)

    try {

      val allReqs = (classicGetFileDir / "requests").list(_.extension == Some(".conf")).toSeq
      import collection.JavaConverters._
      val matchedReqs = allReqs.filter(file => {
        val conf = ConfigFactory.parseFile(file.toJava)
        logger.debug(inspect(conf))
        val entries = conf.entrySet.asScala
        // partial matching
        val matchedEvals = entries.map(entry => {
          queryStrings.get(entry.getKey) match {
            case Some(queryVals) => {
              if (queryVals.size > 1) {
                logger.warn("A duplicated key of a query string found. key: " + entry.getKey)
              }
              val expectedVal = entry.getValue.unwrapped.toString
              val evaled = queryVals.map(queryVal => {
                logger.debug(inspect(queryVal))
                logger.debug(inspect(expectedVal))
                queryVal == expectedVal
              })
              logger.debug(inspect(evaled))
              val notMatched = evaled.filter(_ == false)
              notMatched.isEmpty
            }
            case None => false
          }
        })

        val notMatched = matchedEvals.filter(_ == false)
        // specified params are all matched.
        notMatched.isEmpty
      })
      logger.info(inspect(matchedReqs.size))

      val requestedFilename = matchedReqs.headOption match {
        case Some(file) =>
          logger.debug("A matched request conf file: " + file)
          file.name.dropRight(4) + extensionToUse
        case None => "default." + extensionToUse
      }
      logger.debug(inspect(requestedFilename))

      val allReses = (classicGetFileDir / "responses").list(_.extension == Some("." + extensionToUse)).toSeq
      val matchedReses = allReses.filter(_.name == requestedFilename)
      logger.info(inspect(matchedReses.size))


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

  def post(path: String) = Action {
    val sample = config.get[String]("spass.testing.sample")
    Created(Xml("<xml><method>post</method><desc>Your requested path is " + path +" and a config value is " + sample + ".</desc></xml>"))
  }

  def put(path: String) = Action {
    val sample = config.get[String]("spass.testing.sample")
    Ok(Xml("<xml><method>put</method><desc>Your requested path is " + path +" and a config value is " + sample + ".</desc></xml>"))
  }

  def delete(path: String) = Action {
    val sample = config.get[String]("spass.testing.sample")
    Ok(Xml("<xml><method>delete</method><desc>Your requested path is " + path +" and a config value is " + sample + ".</desc></xml>"))
  }

}
