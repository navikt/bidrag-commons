package no.nav.bidrag.commons.security.azure

import com.nimbusds.oauth2.sdk.GrantType
import org.springframework.security.oauth2.client.endpoint.AbstractOAuth2AuthorizationGrantRequest
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType

class OAuth2JwtBearerGrantRequest(clientRegistration: ClientRegistration?, val assertion: String)
    : AbstractOAuth2AuthorizationGrantRequest(JWT_BEARER_GRANT_TYPE, clientRegistration) {

    companion object {
        val JWT_BEARER_GRANT_TYPE = AuthorizationGrantType(GrantType.JWT_BEARER.value)
    }
}