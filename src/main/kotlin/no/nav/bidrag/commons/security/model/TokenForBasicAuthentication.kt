package no.nav.bidrag.commons.security.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class TokenForBasicAuthentication(
    @JsonProperty("access_token") var access_token: String = "",
    @JsonProperty("token_type") var tokenType: String = ""
) {
    fun fetchToken() = access_token.trim()
}
