package soap

import java.util.regex.Pattern

import play.twirl.api.Xml
import utils.CodeUtility._

object ParsingTest extends App {

  val requestSoapXML = """<?xml version="1.0"?>
                         |
                         |<soap:Envelope
                         |xmlns:soap="http://www.w3.org/2003/05/soap-envelope/"
                         |soap:encodingStyle="http://www.w3.org/2003/05/soap-encoding">
                         |
                         |<soap:Body xmlns:m="http://www.example.org/stock">
                         |  <m:GetStockPrice>
                         |    <m:StockName>IBM</m:StockName>
                         |  </m:GetStockPrice>
                         |</soap:Body>
                         |
                         |</soap:Envelope>""".stripMargin

  val responseSoapXML = """<?xml version="1.0"?>
                        |
                        |<soap:Envelope
                        |    xmlns:soap="http://www.w3.org/2003/05/soap-envelope/"
                        |  soap:encodingStyle="http://www.w3.org/2003/05/soap-encoding">
                        |
                        |<soap:Body xmlns:m="http://www.example.org/stock"  >
                        |  <m:GetStockPriceResponse>
                        |    <m:Price>34.5</m:Price>
                        |  </m:GetStockPriceResponse>
                        |</soap:Body>
                        |
                        |</soap:Envelope>""".stripMargin

  val notXML = """<xml><id>not XML</id></xml>"""

  detectSoap(responseSoapXML)
  detectSoap(notXML)

  def detectSoap(content: String) = {

    println(inspect("aaasoapaaa".matches(".*soap.*")))
    println(inspect(content.matches(".*soap.*")))
    println(inspect(content.matches(".*envelope.*")))

    println(inspect(Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE))
    val soapPattern = Pattern.compile(".*<soap:Envelope.*", Pattern.DOTALL)

    println(inspect(soapPattern.matcher(content).matches()))

    val xml = scala.xml.XML.loadString(content)
    val trimmed = scala.xml.Utility.trim(xml)
    println(inspect(trimmed))
    val env = xml \\ "soap:Envelope"
    println(inspect(env.length))
    val envb = xml \\ "Envelope"
    val soapEnv = envb.filter(_.prefix == "soap")
    println(inspect(soapEnv))
  }

}
