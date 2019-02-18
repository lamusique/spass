import controllers.GreeterController
import controllers.GeneralController
import controllers.SoapMockController
import controllers.RestMockController
import play.api.Configuration
import play.api.i18n.Langs
import play.api.mvc.ControllerComponents
import services.ServicesModule

trait SpassModule extends ServicesModule {

  import com.softwaremill.macwire._

  lazy val greeterController = wire[GreeterController]
  lazy val generalController = wire[GeneralController]
  lazy val soapMockController = wire[SoapMockController]
  lazy val restMockController = wire[RestMockController]

  def langs: Langs
  def configuration: Configuration
  def controllerComponents: ControllerComponents
}
