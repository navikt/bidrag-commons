package no.nav.bidrag.commons.ws;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * SoapSamlCallback
 */
public class SoapSamlCallback extends SoapActionCallback {

  private static final Logger LOGGER = LoggerFactory.getLogger(SoapSamlCallback.class);
  private static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

  private String samlAssertion;
  private Exception error;

  public SoapSamlCallback(String soapAction, String samlToken, Decoder decoder) {
    super(soapAction);

    try {
      this.samlAssertion = decoder.decode(samlToken);
    } catch (RuntimeException re) {
      error = re;
    }
  }

  @Override
  public void doWithMessage(WebServiceMessage message) throws IOException {
    super.doWithMessage(message);

    if (error != null) {
      throw new IOException("Invalid SAML token", error);
    }

    SoapMessage soapMessage = (SoapMessage) message;
    Document document = soapMessage.getDocument();
    SoapHeaderElement wsse = soapMessage.getSoapHeader().addHeaderElement(new QName(WSSE_NS, "Security"));
    DOMResult result = (DOMResult) wsse.getResult();
    Element samlNode = buildSamlAssertionDocument().getDocumentElement();
    Node node = document.importNode(samlNode.cloneNode(true), true);
    result.getNode().appendChild(node);

    try {
      LOGGER.debug("SOAP Message\n" + transformToString(soapMessage.getDocument()));
    } catch (TransformerException e) {
      LOGGER.error("Unable to append saml token", e);
    }
  }

  private Document buildSamlAssertionDocument() {
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      Document document = documentBuilder.parse(new InputSource(new StringReader(samlAssertion)));
      LOGGER.debug("SAML\n" + transformToString(document));

      return document;
    } catch (Exception e) {
      throw new IllegalStateException("Could not build saml assertion document", e);
    }
  }

  private String transformToString(Document document) throws TransformerException {
    DOMSource domSource = new DOMSource(document);
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.INDENT, "no");
    StringWriter sw = new StringWriter();
    StreamResult sr = new StreamResult(sw);
    transformer.transform(domSource, sr);

    return sw.toString();
  }

  @FunctionalInterface
  public interface Decoder {

    String decode(String token);
  }
}
