package controllers

import java.nio.charset.Charset

import better.files.Dsl._
import better.files._
import com.typesafe.config.ConfigFactory
import play.api.i18n.Langs
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import play.api.{Configuration, Logger}
import play.mvc.Http.MimeTypes
import play.twirl.api.{Html, Xml}
import services.GreetingService
import utils.CodeUtility._

class ClassicUriController(greetingService: GreetingService,
                         langs: Langs,
                         config: Configuration,
                         cc: ControllerComponents) extends AbstractController(cc) with WebServiceController {

  private val logger = Logger(getClass)

  def classicGet() = classicGetWithStructure("")

  def classicGetWithStructure(path: String, ext: String = "xml") = Action {implicit request =>

    logger.debug(inspect(path))
    val queryStrings = request.queryString
    logger.debug(inspect(queryStrings))
    // ?aaa=123&bbb=222&aaa=111
    // Map(aaa -> Vector(123, 111), bbb -> Vector(222))

    logger.debug(inspect(request))
    logger.info(wrapForLogging("Requested URI", request.uri))

    val maybeRootPath = config.getOptional[String]("spass.mapping.rootpath")
    val mappingDir = maybeRootPath.map(File(_)).getOrElse(cwd / "mapping")
    val classicGetDir = mappingDir / "classic" / "get"

    // Assume a URI is not /type[/type/..]/ID but /type[/type/..]
    val splitted = path.split("/")
    val classicGetFileDir = splitted.foldLeft(classicGetDir)((z, n) => z / n)

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
      case Some(file) => file.name.dropRight(4) + "xml"
      case None => "default." + ext
    }

    val allReses = (classicGetFileDir / "responses").list(_.extension == Some("." + ext)).toSeq
    val matchedReses = allReses.filter(_.name == requestedFilename)
    logger.info(inspect(matchedReses.size))
    // Should find one file
    val resXmlFile = matchedReses.head
    val content = resXmlFile.contentAsString
    logger.info(wrapForLogging("Response to put back", content))

    Ok(Xml(content))

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


  def contentType(request: Request[AnyContent]) = {
    request.contentType.map(_.toLowerCase) match {
      case Some("application/json") | Some("text/json") => "JSON"
      case Some("application/xml") | Some("text/xml") => "XML"
      case _ => None
    }
  }
  def acceptType(request: Request[AnyContent]) = {
    if (request.accepts(MimeTypes.XML)) {
      // code
    }else if (request.accepts(MimeTypes.JSON)) {
      //code
    } else{
      //code
    }
  }


}
