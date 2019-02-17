import controllers.GreeterController
import controllers.GeneralController
import controllers.SoapLikeController
import play.api.Configuration
import play.api.i18n.Langs
import play.api.mvc.ControllerComponents
import services.ServicesModule

trait GreetingModule extends ServicesModule {

  import com.softwaremill.macwire._

  lazy val greeterController = wire[GreeterController]
  lazy val generalController = wire[GeneralController]
  lazy val soapLikeController = wire[SoapLikeController]

  def langs: Langs
  def configuration: Configuration
  def controllerComponents: ControllerComponents
}
