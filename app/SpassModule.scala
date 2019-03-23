import controllers._
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
  lazy val restConditionalMockController = wire[RestConditionalMockController]
  lazy val homeController = wire[HomeController]
  lazy val classicUriController = wire[ClassicUriController]
  lazy val classicFormController = wire[ClassicFormController]
  lazy val fileController = wire[FileController]

  def langs: Langs
  def configuration: Configuration
  def controllerComponents: ControllerComponents
  implicit def assetsFinder: AssetsFinder

}

