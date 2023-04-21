# bidrag-commons

![](https://github.com/navikt/bidrag-commons/workflows/maven%20deploy/badge.svg)

Komponenter som brukes på tvers av applikasjoner under bidrag

Det finnes et annet felles-bibliotek (`bidrag-commons-test`) som utelukkende er laget for å
gjenbruke komponenter til enhetstesting. 

## Innhold

### Sikkerhetskonfigurasjon
Sikkerhetskonfigurasjon er et enkel wrapper av NAV sin sikkerhetsbibliotek [token-support](https://github.com/navikt/token-support). <br/>
Det er lagt støtte for validering av `Authorization` token i kallet gjennom `token-validation-spring` og generering av token med caching gjennom `token-client-spring`.

Sikkerhetskonfigurasjon kan "skrus" på ved å legge til annotering `@EnableSecurityConfiguration` i konfigurasjonen. <br/>
I tillegg må `SecurityAutoConfiguration` og `ManagementWebSecurityAutoConfiguration` "excludes" da de blir dratt automatisk inn gjennom Spring web konfigurasjon. Disse bibliotekene konfigurer basic Spring sikkerhetskonfigurasjon (basic auth med brukernavn/passord) som ikke er nødvendig da vi gjør vår egen validering av token.

Eksempel på hvordan det kan konfigureres
```kotlin

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class, ManagementWebSecurityAutoConfiguration::class])
@EnableSecurityConfiguration 
class BidragApplication

fun main() {
    SpringApplication.run(BidragApplication::class.java)
}
```

I tillegg må det under legges til i `application.yaml`:
```yaml

no.nav.security.jwt:
  sts: #<--- Hvis STS token skal brukes ved videre kall mot andre apper. Ellers trenger ikke dette å settes
    properties:
      url: ${SECURITY_TOKEN_URL}
      username: ${SRVBIDRAG_USERNAME}
      password: ${SRVBIDRAG_PASSWORD}
  client:
    registration:
      app1:
        token-endpoint-url: https://login.microsoftonline.com/${AZURE_APP_TENANT_ID}/oauth2/v2.0/token
        grant-type: client_credentials #<--- Hvis det er service-service kall og kall videre skal gjøres med applikasjonstoken
        scope: api://${APP1_SCOPE}/.default
        authentication:
          client-id: ${AZURE_APP_CLIENT_ID}
          client-secret: ${AZURE_APP_CLIENT_SECRET}
          client-auth-method: client_secret_post
     app2:
        token-endpoint-url: https://login.microsoftonline.com/${AZURE_APP_TENANT_ID}/oauth2/v2.0/token
        grant-type: urn:ietf:params:oauth:grant-type:jwt-bearer #<--- Hvis kallet gjøres i kontekst av en bruker. Vil da generere on-behalf-of token basert på input token.
        scope: api://${APP2_SCOPE}/.default
        authentication:
          client-id: ${AZURE_APP_CLIENT_ID}
          client-secret: ${AZURE_APP_CLIENT_SECRET}
          client-auth-method: client_secret_post
  issuer:
    isso: #<--- Hvis du forventer ISSO token i input. Ellers kan dette ignoreres
      acceptedaudience: ${ISSO_ACCEPTED_AUDIENCE}
      discoveryurl: ${ISSO_ISSUER_URL}/.well-known/openid-configuration
    sts: #<--- Hvis du forventer STS token i input. Ellers kan dette ignoreres
      acceptedaudience: ${STS_ACCEPTED_AUDIENCE}
      discoveryurl: ${STS_ISSUER_URL}/.well-known/openid-configuration
    aad: #<--- Hvis du forventer Azure token i input. Dette må settes hvis du har RestKontroller
      discovery_url: https://login.microsoftonline.com/${AZURE_APP_TENANT_ID}/v2.0/.well-known/openid-configuration
      accepted_audience: ${AZURE_APP_CLIENT_ID}, api://${AZURE_APP_CLIENT_ID}

```

Restemplate kan da konfigureres ved å bruke `SecurityTokenService` bønne som blir eksponert gjennom sikkerhetskonfigurasjonen.
Interceptorene legger til `Authorization` header til kallet med sikkerhetstoken.
```Kotlin
/**
 * securityTokenService.serviceUserAuthTokenIntercepton("appname") vil generere clienbt_credentials token
 */
@Bean
fun app1Consumer(
    @Value("\${APP1_URL}") app1Url: String,
    restTemplate: HttpHeaderRestTemplate, 
    securityTokenService: SecurityTokenService
): OppgaveConsumer {
    restTemplate.uriTemplateHandler = RootUriTemplateHandler(app1Url)
    restTemplate.interceptors.add(securityTokenService.serviceUserAuthTokenInterceptor("app1"))
    return DefaultOppgaveConsumer(restTemplate)
}

/**
 * securityTokenService.authTokenInterceptor("appname") vil generere token basert på input.
 * Hvis input inneholder applikasjon token så vil det genereres client_credentials token. Ellers vil det genereres on-behalf-of token fra input.
 */
@Bean
fun app2Consumer(
    @Value("\${APP2_URL}") app2Url: String,
    restTemplate: HttpHeaderRestTemplate,
    securityTokenService: SecurityTokenService
): OppgaveConsumer {
    restTemplate.uriTemplateHandler = RootUriTemplateHandler(app1Url)
    restTemplate.interceptors.add(securityTokenService.authTokenInterceptor("app2Url"))
    return DefaultOppgaveConsumer(restTemplate)
}
```

### HttpHeaderRestTemplate
Implementasjon av RestTemplate med noe ekstra funksjonalitet.

`addHeaderGenerator` kan brukes til å legge til Header på kallet på enkel måte.

`withDefaultHeaders` legger til `X-Enhet` og `X-Correlation-ID` headere til kallet. <br/>
Dette hentes fra LocalThread som blir lagt til i `EnhetFilter` og `CorrelationIdFilter` og bør derfor brukes i kombinasjon av filterne

### SensitiveLogMasker
Vil maskere aktørid og fnr fra logg meldingene. Dette kan konfigureres ved å oppdatere logback filen:

```xml
  <appender name="stdout_json" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
      <providers>
        <mdc/>
        <timestamp/>
        <message/>
        <loggerName/>
        <threadName/>
        <logLevel/>
        <callerData/>
        <arguments/>
        <stackTrace/>
      </providers>
      ----> Legg til følgende i logback filen
      <jsonGeneratorDecorator class="net.logstash.logback.mask.MaskingJsonGeneratorDecorator">
        <valueMasker class="no.nav.bidrag.commons.logging.SensitiveLogMasker"/>
      </jsonGeneratorDecorator>
    </encoder>
  </appender>
```
### Web filter
Dette biblioteket inneholder to filtere.

Det ene er `EnhetFilter` som leser `X-Enhet` header fra kallet og lagrer det i MDC


### Bruker cache
Hvis en tjeneste tilbyr tilgangstyrt data som krever spesiell tilgang for å aksessere kan det være dumt å bruke generell cache. Da kan en person uten tilgang kunne aksessere data fordi cachen ikke gjør noe tilgangskontroll på bruker som kaller tjenesten.
Eksempel på dette er caching av respons fra bidrag-person. Hvis en bruker/applikasjon henter data om en kode 6/7 bruker som caches vil neste person uten tilgang kunne aksessere samme data fordi den hentes fra cachen istedenfor PDL.
For å unngå dette problemet bør `@BrukerCacheable` brukes. Denne annoteringen henter saksbehandlerident fra token i kontekst og bruker den når nøkkel for cachen opprettes. Cachen blir da begrenset til bruker istedenfor å være generell.
Hvis det er en applikasjon som kaller tjenesten så vil en felles nøkkel brukes.
For at `@BrukerCacheable` annotering skal fungere må `@Import(BrukerCacheKonfig::class)` annotering legges til på Cache konfigurasjonen

```kotlin
@Configuration
@EnableCaching
@Import(BrukerCacheKonfig::class) <----
class CacheConfig {
....
```

Deretter kan bruker cache brukes på følgende måte

```kotlin
  @BrukerCacheable("personinfo")
  fun hentPersonInfo(ident: String): PersonDto {
    return PersonDto()
  }
```

### Invalider cache før starten av arbeidsdag
Hvis du bruker CaffeineCacheManager kan følgende brukes for å konfigurere cache slik at den invalideres/slettes før starten av arbeisdagen (Kl 06.00 hver dag)
```kotlin
  @Bean
    fun cacheManager(): CacheManager {
        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.registerCustomCache(PERSON_CACHE, Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build())
        return caffeineCacheManager
    }
```


## continuous integration and deployment
Gjøres med 'workflows' og 'actions' fra GitHub. Ny pakker bygges og reslease ved merge til main i github.

