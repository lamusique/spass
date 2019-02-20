import controllers._
import filters.NegativeFilter
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
  lazy val homeController = wire[HomeController]

  def langs: Langs
  def configuration: Configuration
  def controllerComponents: ControllerComponents
  implicit def assetsFinder: AssetsFinder

}

