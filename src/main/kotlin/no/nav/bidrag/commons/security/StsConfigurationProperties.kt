package no.nav.bidrag.commons.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("no.nav.security.jwt.sts")
@ConstructorBinding
data class StsConfigurationProperties(val properties: StsProperties)

data class StsProperties(
  val url: String,
  val username: String,
  val password: String,
)
