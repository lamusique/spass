package controllers

import java.nio.charset.Charset

trait WebServiceController {
  implicit val encoding = Charset.forName("UTF-8")
}
