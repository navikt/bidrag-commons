package no.nav.bidrag.commons.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("no.nav.security.jwt.sts")
data class StsConfigurationProperties
    @ConstructorBinding
    constructor(val properties: StsProperties)

data class StsProperties(
    val url: String,
    val username: String,
    val password: String,
)
