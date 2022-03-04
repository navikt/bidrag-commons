package no.nav.bidrag.commons.security.azure

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.web.client.RestTemplate
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.http.RequestEntity
import org.springframework.web.client.HttpStatusCodeException
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistration
import java.lang.RuntimeException
import org.springframework.util.LinkedMultiValueMap
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import org.springframework.util.MultiValueMap
import java.util.Arrays
import java.util.Optional

class OnBehalfOfTokenResponseClient(restTemplateBuilder: RestTemplateBuilder, environment: Environment?) : OAuth2AccessTokenResponseClient<OAuth2JwtBearerGrantRequest> {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(OnBehalfOfTokenResponseClient::class.java)
        private const val INVALID_TOKEN_RESPONSE_ERROR_CODE = "invalid_token_response"
        private const val OAUTH2_PARAMETER_NAME_ASSERTION = "assertion"
        private const val OAUTH2_PARAMETER_NAME_REQUESTED_TOKEN_USE = "requested_token_use"
    }

    private val restTemplate: RestTemplate

    init {
        restTemplate = if (environment?.activeProfiles?.contains("local")==true) restTemplateBuilder.build() else restTemplateBuilder.customizers(ProxyCustomizer()).build()
        restTemplate.messageConverters = Arrays.asList(
            FormHttpMessageConverter(),
            OAuth2AccessTokenResponseHttpMessageConverter()
        )
    }

    override fun getTokenResponse(oAuth2JwtBearerGrantRequest: OAuth2JwtBearerGrantRequest): OAuth2AccessTokenResponse {
        Assert.notNull(oAuth2JwtBearerGrantRequest, "oAuth2JwtBearerGrantRequest cannot be null")
        val request = convert(oAuth2JwtBearerGrantRequest)
        return try {
            restTemplate.exchange(request, OAuth2AccessTokenResponse::class.java).body!!
        } catch (ex: HttpStatusCodeException) {
            LOGGER.error("received status code={}, and body={}", ex.statusCode, ex.responseBodyAsString)
            val oauth2Error = OAuth2Error(
                INVALID_TOKEN_RESPONSE_ERROR_CODE,
                ex.responseBodyAsString, request.url.toString()
            )
            throw OAuth2AuthorizationException(oauth2Error, ex)
        }
    }

    private fun convert(oAuth2JwtBearerGrantRequest: OAuth2JwtBearerGrantRequest): RequestEntity<*> {
        val headers = getTokenRequestHeaders(oAuth2JwtBearerGrantRequest.clientRegistration)
        val formParameters = buildFormParameters(oAuth2JwtBearerGrantRequest)
        val uri = UriComponentsBuilder.fromUriString(oAuth2JwtBearerGrantRequest.clientRegistration.providerDetails.tokenUri).build().toUri()
        return RequestEntity(formParameters, headers, HttpMethod.POST, uri)
    }

    private fun buildFormParameters(oAuth2JwtBearerGrantRequest: OAuth2JwtBearerGrantRequest): MultiValueMap<String, String> {
        val clientRegistration = oAuth2JwtBearerGrantRequest.clientRegistration
        val scope = java.lang.String.join(" ",
            Optional.ofNullable(clientRegistration.scopes)
                .orElseThrow {
                    RuntimeException(
                        "scope must be set for client with registrationId="
                                + clientRegistration.registrationId
                    )
                })
        val formParameters: MultiValueMap<String, String> = LinkedMultiValueMap()
        if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC == clientRegistration.clientAuthenticationMethod) {
            formParameters.add(OAuth2ParameterNames.CLIENT_ID, clientRegistration.clientId)
            formParameters.add(OAuth2ParameterNames.CLIENT_SECRET, clientRegistration.clientSecret)
        }
        formParameters.add(OAuth2ParameterNames.GRANT_TYPE, oAuth2JwtBearerGrantRequest.grantType.value)
        formParameters.add(OAuth2ParameterNames.SCOPE, scope)
        formParameters.add(OAUTH2_PARAMETER_NAME_ASSERTION, oAuth2JwtBearerGrantRequest.assertion)
        formParameters.add(OAUTH2_PARAMETER_NAME_REQUESTED_TOKEN_USE, "on_behalf_of")
        return formParameters
    }

    private fun getTokenRequestHeaders(clientRegistration: ClientRegistration): HttpHeaders {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        val contentType = MediaType.valueOf(MediaType.APPLICATION_FORM_URLENCODED_VALUE + ";charset=UTF-8")
        headers.contentType = contentType
        if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC == clientRegistration.clientAuthenticationMethod) {
            headers.setBasicAuth(clientRegistration.clientId, clientRegistration.clientSecret)
        }
        return headers
    }
}