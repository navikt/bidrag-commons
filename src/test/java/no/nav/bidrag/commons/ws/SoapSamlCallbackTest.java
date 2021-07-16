package no.nav.bidrag.commons.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;
import no.nav.bidrag.commons.ws.SoapSamlCallback.Decoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.SoapMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@DisplayName("SoapSamlCallback")
@ExtendWith(MockitoExtension.class)
class SoapSamlCallbackTest {

  private static final String SAML_TOKEN = "a token";
  private static final String SAML_TOKEN_GYLDIG = "PHNhbWwyOkFzc2VydGlvbiB4bWxuczpzYW1sMj0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmFzc2VydGlvbiIgSUQ9Il"
      + "NBTUwtZTRhODliZWEtNWZhNS00ODNiLWEzZmEtZGFmNzQzNmFiOGEwIiBJc3N1ZUluc3RhbnQ9IjIwMTktMDktMDVUMTA6NDg6NDEuOTY3WiIgVmVyc2lvbj0iMi4wIj48c2FtbDI6SX"
      + "NzdWVyPklTMDI8L3NhbWwyOklzc3Vlcj48U2lnbmF0dXJlIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjIj48U2lnbmVkSW5mbz48Q2Fub25pY2FsaXphdG"
      + "lvbk1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIvPjxTaWduYXR1cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm"
      + "9yZy8yMDAwLzA5L3htbGRzaWcjcnNhLXNoYTEiLz48UmVmZXJlbmNlIFVSST0iI1NBTUwtZTRhODliZWEtNWZhNS00ODNiLWEzZmEtZGFmNzQzNmFiOGEwIj48VHJhbnNmb3Jtcz48VH"
      + "JhbnNmb3JtIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI2VudmVsb3BlZC1zaWduYXR1cmUiLz48VHJhbnNmb3JtIEFsZ29yaXRobT0iaHR0cDovL3"
      + "d3dy53My5vcmcvMjAwMS8xMC94bWwtZXhjLWMxNG4jIi8-PC9UcmFuc2Zvcm1zPjxEaWdlc3RNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaW"
      + "cjc2hhMSIvPjxEaWdlc3RWYWx1ZT56bjJCNzJNTEtkSkVwVnFHRi9yT080N3RnVVk9PC9EaWdlc3RWYWx1ZT48L1JlZmVyZW5jZT48L1NpZ25lZEluZm8-PFNpZ25hdHVyZVZhbHVlPm"
      + "hHcWxJMkZCNUlLSlJNdndCNlVjVzZSMjZrTWFWR2FtTGFOTmxaVmtheERJOVlSWks0Vm9WaERhanFCMzJBZ2hwZHQzb0NtRWJzMFgKZytBeDBsdHF4R1A3MDl1UEt5ZWNWVVFiTlpMVn"
      + "B1UG84MFdwd2FWS2lCSVVjbHpJR2Q5aDFBME9wYjd1SjRlUC9yN0kyRnVSOGR1Mgpsc0dEZEkraW9heWFEQm40YkJTRnozS29Qb1IrM1czQ0hWVmo5YVJoN3BWSWNmalZKMnoyRVY0UU"
      + "9NTzR2eklKWE5Id25Sb1A5NXFTCkd5T085c1Mwd3UyRWV0YklXbnoyTk9HcU9RYUMxQ3ZYZDBaNVdacFNBeElSTjJrK3BhNVhpeVlwTnZvZEIvWEJDclpxVFEwdmwwc3AKS1Q5UU4xcU"
      + "45Wnh1bWIwOWZUOFY3MkdRd2JFWEdtRjhVajJVNmc9PTwvU2lnbmF0dXJlVmFsdWU-PEtleUluZm8-PFg1MDlEYXRhPjxYNTA5Q2VydGlmaWNhdGU-TUlJR3dqQ0NCYXFnQXdJQkFnSV"
      + "RhZ0FBSExhR1g1UFpIVEZ2YkFBQkFBQWN0akFOQmdrcWhraUc5dzBCQVFzRkFEQlFNUlV3RXdZSwpDWkltaVpQeUxHUUJHUllGYkc5allXd3hGekFWQmdvSmtpYUprL0lzWkFFWkZnZH"
      + "djbVZ3Y205a01SNHdIQVlEVlFRREV4VkNNamNnClNYTnpkV2x1WnlCRFFTQkpiblJsY200d0hoY05NVGd4TURBME1URTFPRFUyV2hjTk1qQXhNREEwTVRJd09EVTJXakIrTVJVd0V3WU"
      + "sKQ1pJbWlaUHlMR1FCR1JZRmJHOWpZV3d4RnpBVkJnb0praWFKay9Jc1pBRVpGZ2R3Y21Wd2NtOWtNUmd3RmdZRFZRUUxFdzlUWlhKMgphV05sUVdOamIzVnVkSE14RlRBVEJnTlZCQX"
      + "NUREVGd2NHeEJZMk52ZFc1MGN6RWJNQmtHQTFVRUF4TVNjM0oyYzJWamRYSnBkSGt0CmRHOXJaVzR0TUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFNSUlCQ2dLQ0FRRUFrZk"
      + "tVcUZVbjZDWnpsUkpRNVlLOFhGU1gKTHV6amVGWUN2eTdiOWVqeUE5eUVJdmNXalZKMnViYVpQNmhzbWxmdGRVUlBTOTBadFhLajIvL05UYjZGK2FZYjdWQ3JXb2N2MFNDZAo5WDMrYk"
      + "h5S09lTEQ1b3ovd2RhaWdyQmZ4eGVUajF5WHNBZ3UzdHF5VDhJNnkyaWlCQjV6ZzlmS2xjazRnNko1cjdCTytiZG9wWUZlCkI1eDJ5bnZFY2pwM0d3OERHMlA5SCtSTlpycDVFdzJqdH"
      + "Y4aW0zV01Dc28zL1FiL0U3THN2Y1dDbmpXTkxRSElrTUh6OXJhb013ZGYKcnBzbnRSN1NaZDVFUllsdi96enBXSjF5MjNmRFljTlF1empTOW13Z2ZWYU9lZlpFRnlSNE9rdUpZK1lmaF"
      + "oydWZxc0dmdmd1L3NMOApQVFBjL082RUdWTVNBUUlEQVFBQm80SURaVENDQTJFd0hRWURWUjBPQkJZRUZCUklTRE91cXZSRGY1ZS9GNXRma0s4c1BaWnpNQjhHCkExVWRJd1FZTUJhQU"
      + "ZPTm9ZMVc5MjJqYk56WGtZS2xTQjZzZ21xdU5NSUlCSVFZRFZSMGZCSUlCR0RDQ0FSUXdnZ0VRb0lJQkRLQ0MKQVFpR2djZHNaR0Z3T2k4dkwyTnVQVUl5TnlVeU1FbHpjM1ZwYm1jbE"
      + "1qQkRRU1V5TUVsdWRHVnliaXhEVGoxQ01qZEVVbFpYTURBNApMRU5PUFVORVVDeERUajFRZFdKc2FXTWxNakJyWlhrbE1qQlRaWEoyYVdObGN5eERUajFUWlhKMmFXTmxjeXhEVGoxRG"
      + "IyNW1hV2QxCmNtRjBhVzl1TEVSRFBYQnlaWEJ5YjJRc1JFTTliRzlqWVd3L1kyVnlkR2xtYVdOaGRHVlNaWFp2WTJGMGFXOXVUR2x6ZEQ5aVlYTmwKUDI5aWFtVmpkRU5zWVhOelBXTl"
      + "NURVJwYzNSeWFXSjFkR2x2YmxCdmFXNTBoanhvZEhSd09pOHZZM0pzTG5CeVpYQnliMlF1Ykc5agpZV3d2UTNKc0wwSXlOeVV5TUVsemMzVnBibWNsTWpCRFFTVXlNRWx1ZEdWeWJpNW"
      + "pjbXd3Z2dGakJnZ3JCZ0VGQlFjQkFRU0NBVlV3CmdnRlJNSUc4QmdnckJnRUZCUWN3QW9hQnIyeGtZWEE2THk4dlkyNDlRakkzSlRJd1NYTnpkV2x1WnlVeU1FTkJKVEl3U1c1MFpYSn"
      + "UKTEVOT1BVRkpRU3hEVGoxUWRXSnNhV01sTWpCclpYa2xNakJUWlhKMmFXTmxjeXhEVGoxVFpYSjJhV05sY3l4RFRqMURiMjVtYVdkMQpjbUYwYVc5dUxFUkRQWEJ5WlhCeWIyUXNSRU"
      + "05Ykc5allXdy9ZMEZEWlhKMGFXWnBZMkYwWlQ5aVlYTmxQMjlpYW1WamRFTnNZWE56ClBXTmxjblJwWm1sallYUnBiMjVCZFhSb2IzSnBkSGt3S2dZSUt3WUJCUVVITUFHR0htaDBkSE"
      + "E2THk5dlkzTndMbkJ5WlhCeWIyUXUKYkc5allXd3ZiMk56Y0RCa0JnZ3JCZ0VGQlFjd0FvWllhSFIwY0RvdkwyTnliQzV3Y21Wd2NtOWtMbXh2WTJGc0wwTnliQzlDTWpkRQpVbFpYTU"
      + "RBNExuQnlaWEJ5YjJRdWJHOWpZV3hmUWpJM0pUSXdTWE56ZFdsdVp5VXlNRU5CSlRJd1NXNTBaWEp1S0RFcExtTnlkREFPCkJnTlZIUThCQWY4RUJBTUNCYUF3T3dZSkt3WUJCQUdDTn"
      + "hVSEJDNHdMQVlrS3dZQkJBR0NOeFVJZ2RiVlhJT0FwMXlFOVowa202UlQKb0xKNWdTU0h2cDFGa1lNaUFnRmtBZ0VDTUIwR0ExVWRKUVFXTUJRR0NDc0dBUVVGQndNQkJnZ3JCZ0VGQl"
      + "FjREFqQW5CZ2tyQmdFRQpBWUkzRlFvRUdqQVlNQW9HQ0NzR0FRVUZCd01CTUFvR0NDc0dBUVVGQndNQ01BMEdDU3FHU0liM0RRRUJDd1VBQTRJQkFRQ2xNQjhVCnZiQ1lLRlBJWFRPSz"
      + "d4SmdKVDJPNUtGWkFMKytURGZaTVA4elF6VkQvcjBvZktrNmlGSzArdElrU0RlSUdHMy9OWk1Dd0dOMDRySE4KTWlpK2hwUThkRDl0Ym85eGtoVC9waElNVFU2YnVZc1BMcjltYWx4Wi"
      + "9PWjZkcHdzclU4US9YM2NTekwyMEphZFY2TVVDaDY4Tit0RApTQzVjZXhPM2MwVFVUdGdhN0pJNGp0b0hQTms4K1FxVTREbXpJbVRtSlpjUGJJOU41bUVJNHFqaVN4Uk5KRkp3dzhHRk"
      + "dlb1dRYklaClJNRkxvbmRsVTdNV1FwL2R2eFVzU0V3Nk9DQnFDSytOWFdUK1VnYUdUbGlRRVN0UGc5aElMT0VQOXlmKy9HVmpqalZLOEd2Mmg5aVIKYlNDZ0dxVWxTMVZZNnVQc3YwVU"
      + "ZXR0RpMGdzM3YrZUo8L1g1MDlDZXJ0aWZpY2F0ZT48WDUwOUlzc3VlclNlcmlhbD48WDUwOUlzc3Vlck5hbWU-Q049QjI3IElzc3VpbmcgQ0EgSW50ZXJuLCBEQz1wcmVwcm9kLCBEQz"
      + "1sb2NhbDwvWDUwOUlzc3Vlck5hbWU-PFg1MDlTZXJpYWxOdW1iZXI-MjM2Mzg3OTAyOTIxMDM1MzM3ODU1Mjg2ODU5MjY5MDEwOTM2MjY3MjI0NTk0MjwvWDUwOVNlcmlhbE51bWJlcj"
      + "48L1g1MDlJc3N1ZXJTZXJpYWw-PC9YNTA5RGF0YT48L0tleUluZm8-PC9TaWduYXR1cmU-PHNhbWwyOlN1YmplY3Q-PHNhbWwyOk5hbWVJRCBGb3JtYXQ9InVybjpvYXNpczpuYW1lcz"
      + "p0YzpTQU1MOjEuMTpuYW1laWQtZm9ybWF0OnVuc3BlY2lmaWVkIj5zcnZiaXN5czwvc2FtbDI6TmFtZUlEPjxzYW1sMjpTdWJqZWN0Q29uZmlybWF0aW9uIE1ldGhvZD0idXJuOm9hc2"
      + "lzOm5hbWVzOnRjOlNBTUw6Mi4wOmNtOmJlYXJlciI-PHNhbWwyOlN1YmplY3RDb25maXJtYXRpb25EYXRhIE5vdEJlZm9yZT0iMjAxOS0wOS0wNVQxMDo0ODo0MS45NjdaIiBOb3RPbk"
      + "9yQWZ0ZXI9IjIwMTktMDktMDVUMTE6NDg6MzYuOTY3WiIvPjwvc2FtbDI6U3ViamVjdENvbmZpcm1hdGlvbj48L3NhbWwyOlN1YmplY3Q-PHNhbWwyOkNvbmRpdGlvbnMgTm90QmVmb3"
      + "JlPSIyMDE5LTA5LTA1VDEwOjQ4OjQxLjk2N1oiIE5vdE9uT3JBZnRlcj0iMjAxOS0wOS0wNVQxMTo0ODozNi45NjdaIi8-PHNhbWwyOkF0dHJpYnV0ZVN0YXRlbWVudD48c2FtbDI6QX"
      + "R0cmlidXRlIE5hbWU9ImlkZW50VHlwZSIgTmFtZUZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmF0dHJuYW1lLWZvcm1hdDp1cmkiPjxzYW1sMjpBdHRyaWJ1dGVWYW"
      + "x1ZT5TeXN0ZW1yZXNzdXJzPC9zYW1sMjpBdHRyaWJ1dGVWYWx1ZT48L3NhbWwyOkF0dHJpYnV0ZT48c2FtbDI6QXR0cmlidXRlIE5hbWU9ImF1dGhlbnRpY2F0aW9uTGV2ZWwiIE5hbW"
      + "VGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphdHRybmFtZS1mb3JtYXQ6dXJpIj48c2FtbDI6QXR0cmlidXRlVmFsdWU-MDwvc2FtbDI6QXR0cmlidXRlVmFsdWU-PC"
      + "9zYW1sMjpBdHRyaWJ1dGU-PHNhbWwyOkF0dHJpYnV0ZSBOYW1lPSJjb25zdW1lcklkIiBOYW1lRm9ybWF0PSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXR0cm5hbWUtZm9ybW"
      + "F0OnVyaSI-PHNhbWwyOkF0dHJpYnV0ZVZhbHVlPnNydmJpc3lzPC9zYW1sMjpBdHRyaWJ1dGVWYWx1ZT48L3NhbWwyOkF0dHJpYnV0ZT48c2FtbDI6QXR0cmlidXRlIE5hbWU9ImF1ZG"
      + "l0VHJhY2tpbmdJZCIgTmFtZUZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmF0dHJuYW1lLWZvcm1hdDp1cmkiPjxzYW1sMjpBdHRyaWJ1dGVWYWx1ZT5mNmExN2EyOC"
      + "0wNWQyLTQ4OGMtYjc0My1hNGE3ZjBkZDYwNDY8L3NhbWwyOkF0dHJpYnV0ZVZhbHVlPjwvc2FtbDI6QXR0cmlidXRlPjwvc2FtbDI6QXR0cmlidXRlU3RhdGVtZW50Pjwvc2FtbDI6QX"
      + "NzZXJ0aW9uPg";

  private SoapSamlCallback soapSamlCallback;

  @Mock
  private Decoder decoderMock;

  @Test
  @DisplayName("skal initiere SoapSamlCallback og dekode saml-token")
  void skalInitiereSoapSamlCallbackMedOgDekodeSamlToken() {
    new SoapSamlCallback("an action", SAML_TOKEN, decoderMock);

    verify(decoderMock).decode("a token");
  }

  @Test
  @DisplayName("skal feile doWithMessage når man ikke har en feilfri dekoding av token")
  void skalFeileDoWithMessageUtenFeilfriDekodingAvToken() {
    Throwable samlTokenDecodeError = new RuntimeException("it blew up");
    when(decoderMock.decode(SAML_TOKEN)).thenThrow(samlTokenDecodeError);

    soapSamlCallback = new SoapSamlCallback("an action", SAML_TOKEN, decoderMock);

    assertThatIOException().isThrownBy(() -> soapSamlCallback.doWithMessage(mock(SoapMessage.class)))
        .withMessageContaining("Invalid SAML token").withCause(samlTokenDecodeError);
  }

  @Test
  @DisplayName("skal opprette saml assertion document")
  void skalOppretteSamlAssertionDocument() {
    var appenderMock = mockLogAppender();
    var soapHeaderMock = mock(SoapHeader.class);
    var soapMessageMock = mock(SoapMessage.class);

    when(soapMessageMock.getSoapHeader()).thenReturn(soapHeaderMock);
    when(soapHeaderMock.addHeaderElement(any(QName.class))).thenReturn(mock(SoapHeaderElement.class));

    soapSamlCallback = new SoapSamlCallback("an action", SAML_TOKEN_GYLDIG, (token) -> new String(Base64.getUrlDecoder().decode(token)));

    // expects NullPointerException after creation of SAML-token
    assertThatNullPointerException().isThrownBy(() -> soapSamlCallback.doWithMessage(soapMessageMock));

    Set<String> logMsgs = fetchMessages(appenderMock);
    String allMsgs = String.join("\n", logMsgs);

    assertAll(
        () -> assertThat(allMsgs).contains("SAML").contains("assertion document"),
        () -> assertThat(allMsgs).doesNotContain("SOAP Message").doesNotContain(SoapSamlCallback.WSSE_NS)
    );
  }

  @Test
  @DisplayName("skal lage en soap melding med saml assertion")
  void skalLageEnSoapMeldingMedSamlAssertion() throws IOException {
    var appenderMock = mockLogAppender();
    var documentMock = mock(Document.class);
    var nodeMock = mock(Node.class);
    var soapHeaderElementmock = mock(SoapHeaderElement.class);
    var soapHeaderMock = mock(SoapHeader.class);
    var soapMessageMock = mock(SoapMessage.class);

    when(soapMessageMock.getSoapHeader()).thenReturn(soapHeaderMock);
    when(soapHeaderMock.addHeaderElement(any(QName.class))).thenReturn(soapHeaderElementmock);
    when(soapHeaderElementmock.getResult()).thenReturn(new DOMResult(nodeMock));
    when(soapMessageMock.getDocument()).thenReturn(documentMock);
    when(documentMock.importNode(any(Element.class), eq(true))).thenReturn(nodeMock);

    soapSamlCallback = new SoapSamlCallback("an action", SAML_TOKEN_GYLDIG, (token) -> new String(Base64.getUrlDecoder().decode(token)));
    soapSamlCallback.doWithMessage(soapMessageMock);

    Set<String> logMsgs = fetchMessages(appenderMock);
    assertThat(String.join("\n", logMsgs)).contains("SOAP Message").contains(SoapSamlCallback.WSSE_NS);
  }

  private Appender<ILoggingEvent> mockLogAppender() {
    @SuppressWarnings("unchecked") Appender<ILoggingEvent> appenderMock = mock(Appender.class);
    var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    lenient().when(appenderMock.getName()).thenReturn("MOCK");
    lenient().when(appenderMock.isStarted()).thenReturn(true);
    logger.addAppender(appenderMock);

    return appenderMock;
  }

  @SuppressWarnings("rawtypes")
  private Set<String> fetchMessages(Appender appenderMock) {
    var msgs = new ArrayList<String>();

    //noinspection unchecked
    verify(appenderMock, atLeastOnce())
        .doAppend(argThat((ArgumentMatcher) argument -> msgs.add(((ILoggingEvent) argument).getFormattedMessage())));

    return new HashSet<>(msgs);
  }
}