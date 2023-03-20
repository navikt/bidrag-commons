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

Gjøres med 'workflows' og 'actions' fra GitHub. Se `.github/workflows/*` for detaljer.

## release endringer

| versjon  | endringstype | beskrivelse                                                                                                                                         |
|----------|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| 0.7.49   | endret       | SensitivLogMasker oppdater regex til å ignorere correlationId.                                                                                      |
| 0.7.48   | endret       | Lagt til default override i ValueGenerator for bakoverkompatibilitet.                                                                               |
| 0.7.47   | endret       | Lagt tilbake et par metoder i HttpHeaderRestTemplate for bakoverkompatibilitet.                                                                     |
| 0.7.46   | endret       | `CorrelationId.fetchCorrelationIdForThread` endret fra å være nullable til å generere en ny basert på timestamp om thread ikke har en correlationId |
| 0.7.45   | opprettet    | `PersonidentGenerator` opprettet for å kunne generere gydlige personident til bruk i test                                                           |
| 0.7.44   | endret       | `HttpHeaderRestTemplate` endre til åpen klasse for å støtte mocking                                                                                 |
| 0.7.43   | endret       | `AbstractRestClient` legg til warning header i feilmelding                                                                                          |
| 0.7.43   | endret       | `AbstractRestClient` legg til warning header i feilmelding                                                                                          |
| 0.7.42   | endret       | Fjernet @Import av MetricsRestTemplateCustomizer.                                                                                                   |
| 0.7.41   | opprettet    | Lagt til BidragHttpHeader, MdcConstants og MDCFilter for ha en felles logikk for dette.                                                             |
| 0.7.40   | endret       | Lagt til options i `AbstractRestClient`                                                                                                             |
| 0.7.39   | endret       | `CorrelationId.fetchCorrelationIdForThread` endret til nullable                                                                                     |
| 0.7.38   | endret       | `TokenUtils` refaktorering og bugfiks                                                                                                               |
| 0.7.37   | endret       | Endret litt på logikken i audit-logging                                                                                                             |
| 0.7.36   | endret       | Oppdatert API i 'TokenUtils'                                                                                                                        |
| 0.7.35   | opprettet    | Lagt til rammeverk for audit-logging                                                                                                                |
| 0.7.34   | endret       | Lagt til jvmStatic og andre mindre endringer for kompatibilitet med Java og Spring                                                                  |
| 0.7.33   | endret       | Lagt til getForNonNullEntity og postForNonNullEntity i AbstractRestClient og oppdatert avhengigheter                                                |
| 0.7.32   | endret       | Endret Java til Kotlin i produksjonskode                                                                                                            |
| 0.7.31   | endret       | Endret AbstractRestClient til å akseptere tom body                                                                                                  |
| 0.7.30   | endret       | Lagt til `SikkerhetsKontekst` og mulighet til å gjøre kall i applikasjonkontekst                                                                    |
| 0.7.29   | endret       | Oppdater `KildesystemIdenfikator` legg til støtte for forsendelse og refaktorer til Kotlin                                                          |
| 0.7.28   | endret       | Fiks `BrukerCacheable` til å ta inn cache navn uten å nevne `value`                                                                                 |
| 0.7.27   | endret       | Lagt til `BrukerCacheable` og `InvaliderCacheFørStartenAvArbeidsdag`                                                                                |
| 0.7.26   | endret       | Lagt til flere headere i `MdcValuesPropagatingInterceptor`                                                                                          |
| 0.7.25   | opprettet    | Lagt til restTemplate som kun bruker service user til autentisering                                                                                 |
| 0.7.24   | endret       | Endret instantiering av restTemplateBuilder                                                                                                         |
| 0.7.23   | endret       | Fikset manglende import av proxy                                                                                                                    |
| 0.7.22   | endret       | Endret konfigurasjon for proxy cumstomizer i RestTemlateBuilderBean                                                                                 |
| 0.7.21   | endret       | Endre issuer idporten til tokenx og oppdater TokenUtils                                                                                             |
| 0.7.20   | endret       | Lagt til stsTokenService dummy bean som brukes hvis stsTokenService bønnen ikke er initialisert                                                     |
| 0.7.19   | endret       | La til støtte for `TokenX` i securityTokenService                                                                                                   |
| 0.7.18   | endret       | Byttet `tokenValidationContextHodler` med `SpringTokenValidationContextHodler()`                                                                    |
| 0.7.17   | endret       | `MdcValuesPropagatingClientInterceptor` Fikset sjekk av riktig MDC-verdi                                                                            |
| 0.7.16   | endret       | `MdcValuesPropagatingClientInterceptor` Satt riktig headernavn.                                                                                     |
| 0.7.15   | endret       | `SecurityConfig` fiks initialisering av StsTokenService bønne                                                                                       |
| 0.7.14   | endret       | `TokenUtils` fiks fetchAppName som kunne gi nullpointerexception                                                                                    |
| 0.7.13   | endret       | Fikset unødvendig lasting av StsConfigurationProperties.                                                                                            |
| 0.7.12   | opprettet    | Hjelpeklasser og konfigurasjon for restklienter.                                                                                                    |
| 0.7.11   | endret       | `TokenUtils` forbredet API                                                                                                                          |
| 0.7.11   | opprettet    | `UserMdcFilter` som legger til brukerid i MDC                                                                                                       |
| 0.7.10   | opprettet    | `TokenUtils` for å kunne hente saksbehandlerident fra token                                                                                         |
| 0.7.9    | endret       | Lagt til exposed headers på `DefaultCorsFilter`                                                                                                     |
| 0.7.8    | endret       | Endret `CorsFilter` til `DefaultCorsFilter` og oppdatert cors konfigurasjon                                                                         |
| 0.7.7    | opprettet    | `CorsFilter` for å kunne kalle bidrag-apper rett fra nettleser                                                                                      |
| 0.7.6    | endret       | `SecurityConfig` authTokenInterceptor bruk Azure client-credentials ved service token                                                               |
| 0.7.5    | endret       | `SecurityConfig` Fiks feil i navConsumerTokenInterceptor                                                                                            |
| 0.7.4    | endret       | `SecurityConfig` rull tilbake auto skru av default spring sikkerhetskonfigurasjon                                                                   |
| 0.7.3    | endret       | `SecurityConfig` feilretting av skru av default spring sikkerhetskonfigurasjon                                                                      |
| 0.7.1    | endret       | `SecurityConfig` skru av default spring sikkerhetskonfigurasjon                                                                                     |
| 0.7.1    | endret       | `SensitiveLogMasker` masker hele sensitiv data istedenfor å vise deler av den                                                                       |
| 0.7.0    | opprettet    | `SensitiveLogMasker` for bruk i logback for maskering av sensitiv data i logg                                                                       |
| 0.6.7    | endret       | Legg til applikasjon STS token hvis inkommende token er STS                                                                                         |
| 0.6.6    | endret       | Fjernet SpringSecurityConfig                                                                                                                        |
| 0.6.5    | endret       | Fikset caching i STSTokenService                                                                                                                    |
| 0.6.4    | endret       | Legg til støtte for å mocke bønner i sikkerhetskonfigurasjon                                                                                        |
| 0.6.3    | endret       | Feilfiks sikkerhetskonfigurasjon for StsService bønne                                                                                               |
| 0.6.2    | endret       | Bruk token-client-spring for sikkerhetskonfigurasjon                                                                                                |
| 0.6.1    | endret       | Feilfiks i sikkerhetskonfigurasjon og støtte for maskin-maskin kommunikasjon                                                                        |
| 0.6.0    | opprettet    | Lagt til felles sikkerhetskonfigurasjon                                                                                                             |
| 0.5.24   | endret       | `HttpHeaderRestTemplate`: Endre logger til nivå debug                                                                                               |
| 0.5.23   | endret       | `HttpHeaderRestTemplate`: Only fetch headergenerator once                                                                                           |
| 0.5.22   | endret       | `HttpHeaderRestTemplate`: Clear enhet from thread                                                                                                   |
| 0.5.21   | endret       | `HttpHeaderRestTemplate`: Hindre duplikat header for x_enhet                                                                                        |
| 0.5.20   | endret       | `HttpHeaderRestTemplate`: metode for å legge til default headere                                                                                    |
| 0.5.20   | endret       | `EnhetFilter`: legg til MDC                                                                                                                         |
| 0.5.19   | endret       | `HttpHeaderRestTemplate`: endret hvordan HEADER blir logget                                                                                         |
| 0.5.18   | endret       | `KildesystemIdentifikator`: ny metode: `erKjentKildesystemMedIdMedIdSomOverstigerInteger()`                                                         |
| 0.5.17.1 | endret       | Kompilering mot spring-boot 2.5.6                                                                                                                   |
| 0.5.16   | endret       | `HttpHeaderRestTemplate`: logger headere og generatorer bare når de finnes...                                                                       |
| 0.5.15   | endret       | `KildesystemIdentifikator`: Kan hente ut journalpostId som long (og ikke bare som int)                                                              |
| 0.5.14   | endret       | `ExceptionLogger`: previous frames melding kommer bare hvis det finnes previous frames                                                              |
| 0.5.13   | endret       | `ExceptionLogger`: delt melding av kode i nav med previous frames på 2 linjer                                                                       |
| 0.5.12   | endret       | `ExceptionLogger`: logger "simple name" til exception, endret hvordan detaljer blir logget                                                          |
| 0.5.11   | endret       | `HttpResponse`: ny metode `clearContentHeaders()`                                                                                                   |
| 0.5.10   | endret       | `ExceptionLogger`: Vil logge et exception en gang sammen med detaljene for dette exceptionet                                                        |
| 0.5.9    | endret       | `ExceptionLogger`: Vil også returnere logginnslag som en liste av strenger                                                                          |
| 0.5.8    | endret       | `CorrelationIdFilter`: Prosesserer ikke logging mot actuator, swagger eller api-docs                                                                |
| 0.5.7    | endret       | `CorrelationId`: Fjernet mulighet for bug når streng er blank, samt kompilering mot spring-boot 2.5.1                                               |
| 0.5.6    | endret       | `ExceptionLogger`: Logger metodenavn på de siste stack frames fra nav                                                                               |
| 0.5.5    | avhengighet  | Kompilering mot spring-boot 2.4.3 -> 2.4.4                                                                                                          |
| 0.5.4    | avhengighet  | Kompilering mot spring-boot 2.4.2 -> 2.4.3                                                                                                          |
| 0.5.3.3  | endret       | `ExceptionLogger`: Logger 3 siste stack elements sett fra nav                                                                                       |
| 0.5.3.2  | endret       | `ExceptionLogger`: Walking the stack trace, not the stack                                                                                           |
| 0.5.3.1  | endret       | `ExceptionLogger`: Ikke logging fra klasser som har filnavn lik <generated>                                                                         |
| 0.5.3    | endret       | `ExceptionLogger`: Vil ikke logge kode auto-generert klassenavn                                                                                     |
| 0.5.2    | endret       | `ExceptionLogger`: Kan instansieres med klasser som ikke skal være del av stack som logges                                                          |
| 0.5.1    | endret       | `ExceptionLogger`: Henter ikke stack fra exception, men bruker StackWalker.getInstance()                                                            |
| 0.5.0    | endret       | `HttpHeaderRestTemplate`: Kan fjerne header generator                                                                                               |
| 0.4.3    | endret       | `pom.xml`: oppgradert spring-boot for å fjerne sårbar avhengighet                                                                                   |
| 0.4.2    | endret       | `pom.xml`: fjernet sårbar avhengighet                                                                                                               |
| 0.4.1    | endret       | `HttpResponse`: Kan opprettes med "body", `HttpHeaders` og status kode, samt ny metode: `fetchHeaders`                                              |
| 0.4.0    | opprettet    | `WebUtil`: Util klasse med hjelpemetoder. Første versjon: Kan initialisere `HttpHeadera` fra spring med name/value på header                        |
| 0.4.0    | opprettet    | `HttpResponse`: Mulighet for å videresende `ResponseEntity` fra spring                                                                              |
| 0.4.0    | deprecated   | `HttpStatusResponse`: Behov for å videresende mer enn bare http status                                                                              |
| 0.3.7    | endret       | `ExceptionLogger`: Logger ikke response body fra `HttpStatusCodeException` når den mangler                                                          |
| 0.3.6    | endret       | `ExceptionLogger`: Logger også response body når det er et `HttpStatusCodeException`                                                                |
| 0.3.5    | endret       | `CorreleationIdFilter`: Fikse IndexOutOfBounds                                                                                                      |
| 0.3.4    | endret       | `CorreleationIdFilter`: Bruker class.getSimpleName() ved logging                                                                                    |
| 0.3.4    | endret       | `EnhetFilter`: Bruker class.getSimpleName() ved logging                                                                                             |
| 0.3.3    | endret       | `EnhetFilter`: Endre navn på headerfelt X-Enhetsnummer til X-Enhet                                                                                  |
| 0.3.2    | endret       | `HttpHeaderRestTemplate`: Logger header names (ikke values)                                                                                         |
| 0.3.2    | endret       | `SoapSamlCallback`: Logger ikke SOAP message                                                                                                        |
| 0.3.1    | endret       | `HttpHeaderRestTemplate`: Logger masked Authorization header                                                                                        |
| 0.3.0    | endret       | `KildesystemIdentifikator`: value bean uten statisk tilstand                                                                                        |
| 0.3.0    | endret       | `KildesystemIdentifikator`: Forholder seg ikke til heltallstype ved sjekking av gyldighet (long vs integer)                                         |
| 0.2.1    | endret       | Fjernet skadelige avhengigheter rapportert av snyk.io                                                                                               |
| 0.2.0    | -- ingen --  | Overgang til bruk av github som maven-repo                                                                                                          |
| 0.1.18   | endret       | `ExceptionLogger`: Exception med massage blir printet først i meldingene som logges                                                                 |
| 0.1.15   | endret       | `CorrelationId`: genererer `<hex>-correlationId` når verdien som blir gitt er null                                                                  |
| 0.1.15   | endret       | `CorrelationId`: genererer correlation id med `-` uten `()`                                                                                         |
| 0.1.14   | opprettet    | `SoapSamlCallback`: feil ved deploy av versjon 0.1.13                                                                                               |
| 0.1.13   | opprettet    | `SoapSamlCallback`: `SoapActionCallback` i en web service med et saml-token                                                                         |
| 0.1.12   | endring      | `EnhetFilter`: tar vare på enhetsinformasjon i ThreadLocal                                                                                          |
| 0.1.11   | endring      | `EnhetFilter`: error logger hvis ServletRequest ikke er en HttpServletRequest                                                                       |
| 0.1.10   | opprettet    | `HttpStatusResponse.isNotSuccessful()`:                                                                                                             |
| 0.1.9    | opprettet    | `KildesystemIdenfikator.hentJournalpostId()`:                                                                                                       |
| 0.1.8    | opprettet    | `KildesystemIdenfikator`: til å identifisere                                                                                                        |
| 0.1.7    | endring      | fix vulnerabilities reported on snyk.io                                                                                                             |
| 0.1.6    | endring      | `ExceptionLogger`: logger exception sett fra nav kode uten exception cause også                                                                     |
| 0.1.5    | endring      | `ExceptionLogger`: logger ikke egne innslag for exception uten cause                                                                                |
| 0.1.3    | endring      | `ExceptionLogger`: logger siste StackTraceElement fra no.nav før exception og skipper logging fra stack                                             |
| 0.1.2    | endring      | `ExceptionLogger`: utbedret logging, samt redusert logging til å bare logge full stack på root causeœ                                               |
| 0.1.1    | slettet      | `HttpStatusResponse`: metode `fetchBody()`                                                                                                          |
| 0.1.0    | endring      | `HttpStatusResponse`: navn på metoder (`isEmpty()` -> `isBodyEmpty`)                                                                                |
| 0.1.0    | endring      | `HttpStatusResponse`: ny metode (`fetchBody()`)                                                                                                     |
| 0.1.0    | endring      | `HttpStatusResponse`: ny metode (`isBodyPresent()`)                                                                                                 |
| 0.0.32   | endret       | `EnhetFilter`: logger ikke kall mot "actuator"-tjenester                                                                                            |
| 0.0.31   | endret       | `EnhetFilter`: fortsetter filtrering med `javax.servlet.FilterChain`                                                                                |
| 0.0.30   | opprettet    | `EnhetFilter`: et `javax.servlet.Filter` som videresender header med enhetsnummer                                                                   |
| 0.0.29   | endring      | `HttpStatusResponse`: konstruktør bare for `HttpStatus`                                                                                             |
| 0.0.28   | endring      | `HttpStatusResponse`: metode (`isEmpty()`)                                                                                                          |
| 0.0.28   | endring      | `HttpHeaderRestTemplate`: akspeterer at et "callback" ikke gjøres med en `HttpEntity`                                                               |
| 0.0.27   | endring      | `HttpStatusResponse`: metode (`toString()`)                                                                                                         |
| 0.0.25   | opprettet    | `HttpStatusResponse` for å videresende HttpStatus sammen med resultat fra consumer                                                                  |
| 0.0.23   | endring      | ny java baseline - java 12                                                                                                                          |
| 0.0.21   | slettet      | `HttpHeaderRestTemplate.Header` removed. Not a backword compatible change!                                                                          |
| 0.0.19   | endring      | Correlation Id without public constructors and the CorrelationIdFilter will always have the last part of the request uri containing plain text      |
| 0.0.17   | endring      | Header name of correlation id is also present on the value bean                                                                                     |
| 0.0.15   | endring      | Check correlation id header on request and name of header X_CORRELATION_ID -> X-Correlation-ID                                                      |
| 0.0.13   | endring      | `CorrelationId` as value bean                                                                                                                       |
| 0.0.11   | endring      | `ExceptionLogger` logger `Throwable` type                                                                                                           |
| 0.0.9    | endring      | `ExceptionLogger` logger `Exception` cause                                                                                                          |
| 0.0.8    | opprettet    | `ExceptionLogger`                                                                                                                                   |
| 0.0.6    | endring      | `CorrelationIdFilter` legger generert id på `ThreadLocal`                                                                                           |
| 0.0.4    | endring      | `HttpHeaderRestTemplate.Header` fra et interface til en klasse for enklere bruk                                                                     |
| 0.0.1    | opprettet    | `CorrelationIdFilter` og `HttpHeaderRestTemplate`                                                                                                   |
