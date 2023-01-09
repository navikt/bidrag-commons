package no.nav.bidrag.commons.ws

import org.slf4j.LoggerFactory
import org.springframework.ws.WebServiceMessage
import org.springframework.ws.soap.SoapMessage
import org.springframework.ws.soap.client.core.SoapActionCallback
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * SoapSamlCallback
 */
class SoapSamlCallback(soapAction: String?, samlToken: String?, decoder: Decoder) : SoapActionCallback(soapAction) {

  private var samlAssertion: String? = null
  private var error: Exception? = null

  init {
    try {
      samlAssertion = decoder.decode(samlToken)
    } catch (re: RuntimeException) {
      error = re
    }
  }

  @Throws(IOException::class)
  override fun doWithMessage(message: WebServiceMessage) {
    super.doWithMessage(message)
    if (error != null) {
      throw IOException("Invalid SAML token", error)
    }
    val soapMessage = message as SoapMessage
    val document = soapMessage.document
    val wsse = soapMessage.soapHeader.addHeaderElement(QName(WSSE_NS, "Security"))
    val result = wsse.result as DOMResult?
    val samlNode = buildSamlAssertionDocument().documentElement
    val node = document.importNode(samlNode.cloneNode(true), true)
    result?.node?.appendChild(node)
    try {
      val transformed = transformToString(soapMessage.document)
      val index = transformed.indexOf('>')
      LOGGER.debug("SOAP Message med SAML token fra {}: {}{}", WSSE_NS, transformed.substring(0, index), "...")
    } catch (e: TransformerException) {
      LOGGER.error("Unable to append saml token", e)
    }
  }

  private fun buildSamlAssertionDocument(): Document {
    return try {
      val documentBuilderFactory = DocumentBuilderFactory.newInstance()
      documentBuilderFactory.isNamespaceAware = true
      val documentBuilder = documentBuilderFactory.newDocumentBuilder()
      val document = documentBuilder.parse(InputSource(StringReader(samlAssertion)))
      val transformed = transformToString(document)
      val index = transformed.indexOf('>')
      LOGGER.debug("SAML assertion document: {}{}", transformed.substring(0, index), "...")
      document
    } catch (e: Exception) {
      throw IllegalStateException("Could not build saml assertion document", e)
    }
  }

  @Throws(TransformerException::class)
  private fun transformToString(document: Document): String {
    val domSource = DOMSource(document)
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    transformer.setOutputProperty(OutputKeys.INDENT, "no")
    val sw = StringWriter()
    val sr = StreamResult(sw)
    transformer.transform(domSource, sr)
    return sw.toString()
  }

  fun interface Decoder {
    fun decode(token: String?): String?
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(SoapSamlCallback::class.java)
    const val WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
  }
}