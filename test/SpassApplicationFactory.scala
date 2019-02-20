import org.scalatestplus.play.FakeApplicationFactory
import play.api.inject.DefaultApplicationLifecycle
import play.api.{Application, ApplicationLoader, Configuration, Environment}
import play.core.DefaultWebCommands

trait SpassApplicationFactory extends FakeApplicationFactory {

  private class SpassApplicationBuilder {
    def build(): Application = {
      val env = Environment.simple()
      val context = ApplicationLoader.Context.create(env)
      val loader = new SpassApplicationLoader()
      loader.load(context)
    }
  }

  def fakeApplication(): Application = new SpassApplicationBuilder().build()

}
