package filters

import javax.inject._
import play.api.http.HttpEntity.Strict
import play.api.mvc.Results.Status
import play.api.{Configuration, Logger}
import play.api.mvc._
import play.twirl.api.Html

import scala.concurrent.{ExecutionContext, Future}
import utils.CodeUtility._

/**
  * This is a simple filter that adds a header to all requests. It's
  * added to the application's list of filters by the
  * [[Filters]] class.
  *
  * @param ec This class is needed to execute code asynchronously.
  * It is used below by the `map` method.
  */
class NegativeFilter(config: Configuration)(implicit ec: ExecutionContext) extends EssentialFilter {
  private val logger = Logger(getClass)

  override def apply(next: EssentialAction) = EssentialAction { request =>
    logger.debug(inspect(request))
    implicit val impReq: RequestHeader = request
    implicit val impNext: EssentialAction = next

    val maybeNegativeMode = config.getOptional[String]("negative.mode")
    val isNegative = maybeNegativeMode match {
      case Some(mode) => "on" == mode
      case None => false
    }
    logger.debug(inspect(isNegative))

    if (isNegative) {

      val maybeWaitMillis = config.getOptional[String]("negative.waitmillis")

      // wait millis setting is prior to status code
        maybeWaitMillis match {
          case Some(waitMillis) => {
            logger.debug(inspect(waitMillis))
            // a blocking point
            Thread.sleep(waitMillis.toLong)
            success(List("X-Spass-Mock-Mode" -> "NegativeWait", "X-Spass-WaitMillis" -> waitMillis))
          }
          case None => {
            next(request).map { result => {
              // Should be specified in conf
              val code = config.get[String]("negative.code")
              val status = new Status(code.toInt)
              val errorStatusCode = status(code)
              logger.debug(inspect(errorStatusCode))
              errorStatusCode
                .withHeaders("X-Spass-Response-Data" -> "mock")
                .withHeaders("X-Spass-Mock-Mode" -> "NegativeStatusCode")
            }
            }
          }
        }

    } else {

      success(List("X-Spass-Mock-Mode" -> "Positive"))
    }


  }

  private def success(headers: List[(String, String)] = List.empty[(String, String)])(implicit next: EssentialAction, request: RequestHeader) = {
    next(request).map { result =>
      headers.foldLeft(result.withHeaders("X-Spass-Response-Data" -> "mock"))((z, n) => z.withHeaders(n))
    }
  }

}
