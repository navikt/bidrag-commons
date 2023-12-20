package no.nav.bidrag.commons.security.maskinporten

data class MaskinportenTokenResponse(
    val access_token: String,
    val token_type: String?,
    val expires_in: Int?,
    val scope: String?,
)
