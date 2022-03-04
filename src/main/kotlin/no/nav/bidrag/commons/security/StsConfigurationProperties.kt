package no.nav.bidrag.commons.security

import org.springframework.validation.annotation.Validated
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import javax.validation.constraints.NotEmpty
import javax.validation.Valid
import no.nav.security.token.support.client.core.ClientProperties

@Validated
@ConfigurationProperties("no.nav.security.jwt.sts")
@ConstructorBinding
data class StsConfigurationProperties(
    var properties: StsProperties?
)

data class StsProperties(
    var url: String,
    var username: String,
    var password: String,
)