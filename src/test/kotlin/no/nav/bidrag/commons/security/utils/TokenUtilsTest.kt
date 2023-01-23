package no.nav.bidrag.commons.security.utils

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.commons.security.SikkerhetsKontekst.Companion.medApplikasjonKontekst
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import java.util.Optional

internal class TokenUtilsTest {
  // Generated using http://jwtbuilder.jamiekurtz.com/
  private val issoUser =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2lzc28tcS5hZGVvLm5vOjQ0My9pc3NvL29hdXRoMiIsImlhdCI6MTY1NTg3NzQyNCwiZXhwIjoxNjg3NDEzNDI0LCJhdWQiOiJiaWRyYWctdWktZmVhdHVyZS1xMSIsInN1YiI6Ilo5OTQ5NzciLCJ0b2tlbk5hbWUiOiJpZF90b2tlbiIsImF6cCI6ImJpZHJhZy11aS1mZWF0dXJlLXExIn0.NYxxExStmzxqvjf-uKn7EnT9rOzluRxipclj0IH_0XQ"
  private val stsToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL3NlY3VyaXR5LXRva2VuLXNlcnZpY2UubmFpcy5wcmVwcm9kLmxvY2FsIiwiaWF0IjoxNjU1ODc3NDI0LCJleHAiOjE2ODc0MTM0MjQsImF1ZCI6InNydmJpc3lzIiwic3ViIjoic3J2YmlzeXMiLCJpZGVudFR5cGUiOiJTeXN0ZW1yZXNzdXJzIiwiYXpwIjoic3J2YmlzeXMifQ.ivpkYHclkl9z3fOfCSIMKKOsRSOGzr-y9AqerJEy9BA"
  private val azureSystemToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vdGVzdC92Mi4wIiwiaWF0IjoxNjU1ODc3MDQwLCJleHAiOjE2ODc0MTMwNDAsImF1ZCI6IjY3NjY2NDUtNTNkNS00OGY5LWJlOTctOTljN2ZjNzRmMDlhIiwic3ViIjoiNTU1NTU1LTUzZDUtNDhmOS1iZTk3LTk5YzdmYzc0ZjA5YSIsImF6cF9uYW1lIjoiZGV2LWZzczpiaWRyYWc6YmlkcmFnLWRva3VtZW50LWZlYXR1cmUiLCJyb2xlcyI6WyJhY2Nlc3NfYXNfYXBwbGljYXRpb24iLCJzb21ldGhpbmcgZWxzZSJdfQ.XvdyJCtIt-ME4t956z76xOf2hrkM7WOvTRWjI6QcYiA"
  private val azureUserToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vdGVzdC92Mi4wIiwiaWF0IjoxNjU1ODc3MDQwLCJleHAiOjE2ODc0MTMwNDAsImF1ZCI6IjY3NjY2NDUtNTNkNS00OGY5LWJlOTctOTljN2ZjNzRmMDlhIiwic3ViIjoiNTU1NTU1LTUzZDUtNDhmOS1iZTk3LTk5YzdmYzc0ZjA5YSIsImF6cF9uYW1lIjoiZGV2LWZzczpiaWRyYWc6YmlkcmFnLXVpLWZlYXR1cmUiLCJSb2xlIjoiYWNjZXNzX2FzX2FwcGxpY2F0aW9uIiwiTkFWaWRlbnQiOiJaOTk0OTc3In0.7XhNn27iaKY-z4voUp-ZfR__5u3Rv5rJCgTpSNVW1nY"
  private val tokenXUserToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuZGluZ3MuZGV2LWdjcC5uYWlzLmlvIiwiaWF0IjoxNjc0MTEyODk3LCJleHAiOjE3MDU2NDg4OTcsImF1ZCI6ImRldi1nY3A6YmlkcmFnOmJpZHJhZy1yZWlzZWtvc3RuYWQtYXBpIiwic3ViIjoiel9xZmw5RUhlLWoySGtZQ3RLeUZuT05hR1E5TWJLUTBRQm00MUgtVEVCVT0iLCJjbGllbnRfaWQiOiJkZXYtZ2NwOmJpZHJhZzpiaWRyYWctcmVpc2Vrb3N0bmFkLXVpIiwicGlkIjoiMTQ4MTcyOTgwMTAiLCJhY3IiOiJMZXZlbDQiLCJpZHAiOiJodHRwczovL29pZGMtdmVyMi5kaWZpLm5vL2lkcG9ydGVuLW9pZGMtcHJvdmlkZXIvIiwic2NvcGUiOiJvcGVuaWQiLCJjbGllbnRfb3Jnbm8iOiI4ODk2NDA3ODIifQ.KauxVua8ebC9Lc-Wx4XE2NlI_0_c1YvJi1Y5drEOoUM"
  @AfterEach
  fun clearTokenContext(){
    RequestContextHolder.resetRequestAttributes()
  }

  @Test
  fun skalHenteSubjectFraAzureSystemToken() {
    mockTokenContext(azureSystemToken)
    val subject = TokenUtils.hentApplikasjonsnavn()

    // then
    subject shouldBe "bidrag-dokument-feature"
  }

  @Test
  fun skalHenteSaksbehandlerFraAzureToken() {
    mockTokenContext(azureUserToken)
    val subject = TokenUtils.hentBruker()

    // then
    subject shouldBe "Z994977"
  }

  @Test
  fun skalHenteSaksbehandlerIdentFraAzureToken() {
    mockTokenContext(azureUserToken)
    val subject = TokenUtils.hentSaksbehandlerIdent()

    // then
    subject shouldBe "Z994977"
  }

  @Test
  fun skalHenteSubjectFraIssoToken() {
    mockTokenContext(issoUser)
    val subject = TokenUtils.hentBruker()

    // then
    subject shouldBe "Z994977"
  }

  @Test
  fun skalHenteAppNavnFraIssoToken() {
    mockTokenContext(issoUser)
    val subject = TokenUtils.hentApplikasjonsnavn()

    // then
    subject shouldBe "bidrag-ui-feature-q1"
  }

  @Test
  fun skalHenteAppNavnFraAzureToken() {
    mockTokenContext(azureUserToken)
    val subject = TokenUtils.hentApplikasjonsnavn()

    // then
    subject shouldBe "bidrag-ui-feature"
  }

  @Test
  fun skalIkkeHenteSaksbehandlerHvisApplikasjonToken() {
    mockTokenContext(azureSystemToken)

    // then
    TokenUtils.hentSaksbehandlerIdent() shouldBe null
  }


  @Test
  fun skalHenteApplikasjonsnavnFraTokenxToken(){
    mockTokenContext(tokenXUserToken)
    TokenUtils.hentApplikasjonsnavn() shouldBe "bidrag-reisekostnad-ui"
  }

  @Test
  fun skalHenteFødselsnummerFraTokenxToken(){
    mockTokenContext(tokenXUserToken)
    TokenUtils.hentBruker() shouldBe "14817298010"
  }

  @Test
  fun shouldValidateSystemToken() {

    mockTokenContext(azureSystemToken)
    val resultAzure = TokenUtils.erApplikasjonsbruker()

    mockTokenContext(stsToken)
    val resultSTS = TokenUtils.erApplikasjonsbruker()

    mockTokenContext(azureUserToken)
    val resultAzureUser = TokenUtils.erApplikasjonsbruker()

    mockTokenContext(issoUser)
    val resultIsso = TokenUtils.erApplikasjonsbruker()

    // then
    resultAzure shouldBe true
    resultSTS shouldBe true
    resultAzureUser shouldBe false
    resultIsso shouldBe false
  }

  @Test
  fun shouldValidateTokenIssuedBy() {

    mockTokenContext(azureSystemToken)
    TokenUtils.erTokenUtstedtAv(TokenIssuer.AZURE) shouldBe true
    TokenUtils.erTokenUtstedtAv(TokenIssuer.STS) shouldBe false
    TokenUtils.erTokenUtstedtAv(TokenIssuer.TOKENX) shouldBe false

    mockTokenContext(stsToken)
    TokenUtils.erTokenUtstedtAv(TokenIssuer.AZURE) shouldBe false
    TokenUtils.erTokenUtstedtAv(TokenIssuer.STS) shouldBe true
    TokenUtils.erTokenUtstedtAv(TokenIssuer.TOKENX) shouldBe false

    mockTokenContext(tokenXUserToken)
    TokenUtils.erTokenUtstedtAv(TokenIssuer.AZURE) shouldBe false
    TokenUtils.erTokenUtstedtAv(TokenIssuer.STS) shouldBe false
    TokenUtils.erTokenUtstedtAv(TokenIssuer.TOKENX) shouldBe true

  }

  @Test
  fun skalValidereApplikasjonBrukerIAppKontekst() {

    medApplikasjonKontekst {
      TokenUtils.erApplikasjonsbruker() shouldBe true
      TokenUtils.hentApplikasjonsnavn() shouldBe null
    }
  }

  fun mockTokenContext(token: String) {
    val tokenValidationContext = mockk<TokenValidationContext>()
    val requestAttributes = mockk<RequestAttributes>()
    RequestContextHolder.setRequestAttributes(requestAttributes)
    every {
      requestAttributes.getAttribute(
        SpringTokenValidationContextHolder::class.java.name,
        RequestAttributes.SCOPE_REQUEST
      )
    } returns tokenValidationContext
    every { tokenValidationContext.hasValidToken() } returns true
    every { tokenValidationContext.firstValidToken } returns Optional.ofNullable(JwtToken(token))
  }

}