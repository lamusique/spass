
import _root_.controllers.AssetsComponents
import com.softwaremill.macwire._
import play.api.ApplicationLoader.Context
import play.api._
import play.api.i18n._
import play.api.mvc._
import play.api.routing.Router
import router.Routes

/**
 * Application loader that wires up the application dependencies using Macwire
 */
class SpassApplicationLoader extends ApplicationLoader {
  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }
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

  lazy val router: Router = {
    // add the prefix string in local scope for the Routes constructor
    val prefix: String = "/"
    wire[Routes]
  }
}
