
import _root_.controllers.AssetsComponents
import com.softwaremill.macwire._
import filters.NegativeFilter
import play.api.ApplicationLoader.Context
import play.api._
import play.api.i18n._
import play.api.mvc._
import play.api.routing.Router
import router.Routes

import scala.concurrent.ExecutionContext

import better.files._
import java.io.{File => JFile}


/**
 * Application loader that wires up the application dependencies using Macwire
 */
class SpassApplicationLoader extends ApplicationLoader {

  private val logger = Logger(getClass)

  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    logger.info(
      Console.YELLOW
      + Resource.getAsString("logo.txt")
      + Console.RESET)

    new SpassComponents(context).application
  }
}

class SpassComponents(context: Context) extends BuiltInComponentsFromContext(context)
  with SpassModule
  with AssetsComponents
  with I18nComponents 
  with play.filters.HttpFiltersComponents {

  // set up logger
  LoggerConfigurator(context.environment.classLoader).foreach {
    _.configure(context.environment, context.initialConfiguration, Map.empty)
  }

  private val logger = Logger(getClass)


  // filters
  lazy val negativeFilter = wire[NegativeFilter]

  override def httpFilters: Seq[EssentialFilter] = {
    super.httpFilters :+ negativeFilter
  }

  lazy val router: Router = {
    // add the prefix string in local scope for the Routes constructor
    val prefix: String = "/"
    wire[Routes]
  }

//  implicit val configuration: Configuration
//  implicit val ec: ExecutionContext

//  logger.debug(configuration.toString)

}
