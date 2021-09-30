# bidrag-commons

![](https://github.com/navikt/bidrag-commons/workflows/maven%20deploy/badge.svg)

Komponenter som brukes på tvers av applikasjoner under bidrag

Det finnes et annet felles-bibliotek (`bidrag-commons-test`) som utelukkende er laget for å
gjenbruke komponenter til enhetstesting. 

## continuous integration and deployment

Gjøres med 'workflows' og 'actions' fra GitHub. Se `.github/workflows/*` for detaljer. 

## release endringer

versjon | endringstype | beskrivelse
--------|--------------|------------
0.5.12  | endret       | `ExceptionLogger`: logger "simple name" til exception, endret hvordan detaljer blir logget
0.5.11  | endret       | `HttpResponse`: ny metode `clearContentHeaders()`
0.5.10  | endret       | `ExceptionLogger`: Vil logge et exception en gang sammen med detaljene for dette exceptionet
0.5.9   | endret       | `ExceptionLogger`: Vil også returnere logginnslag som en liste av strenger
0.5.8   | endret       | `CorrelationIdFilter`: Prosesserer ikke logging mot actuator, swagger eller api-docs
0.5.7   | endret       | `CorrelationId`: Fjernet mulighet for bug når streng er blank, samt kompilering mot spring-boot 2.5.1
0.5.6   | endret       | `ExceptionLogger`: Logger metodenavn på de siste stack frames fra nav
0.5.5   | avhengighet  | Kompilering mot spring-boot 2.4.3 -> 2.4.4
0.5.4   | avhengighet  | Kompilering mot spring-boot 2.4.2 -> 2.4.3
0.5.3.3 | endret       | `ExceptionLogger`: Logger 3 siste stack elements sett fra nav
0.5.3.2 | endret       | `ExceptionLogger`: Walking the stack trace, not the stack
0.5.3.1 | endret       | `ExceptionLogger`: Ikke logging fra klasser som har filnavn lik <generated>
0.5.3   | endret       | `ExceptionLogger`: Vil ikke logge kode auto-generert klassenavn
0.5.2   | endret       | `ExceptionLogger`: Kan instansieres med klasser som ikke skal være del av stack som logges
0.5.1   | endret       | `ExceptionLogger`: Henter ikke stack fra exception, men bruker StackWalker.getInstance()
0.5.0   | endret       | `HttpHeaderRestTemplate`: Kan fjerne header generator
0.4.3   | endret       | `pom.xml`: oppgradert spring-boot for å fjerne sårbar avhengighet
0.4.2   | endret       | `pom.xml`: fjernet sårbar avhengighet
0.4.1   | endret       | `HttpResponse`: Kan opprettes med "body", `HttpHeaders` og status kode, samt ny metode: `fetchHeaders`
0.4.0   | opprettet    | `WebUtil`: Util klasse med hjelpemetoder. Første versjon: Kan initialisere `HttpHeadera` fra spring med name/value på header
0.4.0   | opprettet    | `HttpResponse`: Mulighet for å videresende `ResponseEntity` fra spring
0.4.0   | deprecated   | `HttpStatusResponse`: Behov for å videresende mer enn bare http status
0.3.7   | endret       | `ExceptionLogger`: Logger ikke response body fra `HttpStatusCodeException` når den mangler 
0.3.6   | endret       | `ExceptionLogger`: Logger også response body når det er et `HttpStatusCodeException` 
0.3.5   | endret       | `CorreleationIdFilter`: Fikse IndexOutOfBounds
0.3.4   | endret       | `CorreleationIdFilter`: Bruker class.getSimpleName() ved logging
0.3.4   | endret       | `EnhetFilter`: Bruker class.getSimpleName() ved logging
0.3.3   | endret       | `EnhetFilter`: Endre navn på headerfelt X-Enhetsnummer til X-Enhet
0.3.2   | endret       | `HttpHeaderRestTemplate`: Logger header names (ikke values)
0.3.2   | endret       | `SoapSamlCallback`: Logger ikke SOAP message
0.3.1   | endret       | `HttpHeaderRestTemplate`: Logger masked Authorization header
0.3.0   | endret       | `KildesystemIdentifikator`: value bean uten statisk tilstand
0.3.0   | endret       | `KildesystemIdentifikator`: Forholder seg ikke til heltallstype ved sjekking av gyldighet (long vs integer)
0.2.1   | endret       | Fjernet skadelige avhengigheter rapportert av snyk.io
0.2.0   | -- ingen --  | Overgang til bruk av github som maven-repo
0.1.18  | endret       | `ExceptionLogger`: Exception med massage blir printet først i meldingene som logges
0.1.15  | endret       | `CorrelationId`: genererer `<hex>-correlationId` når verdien som blir gitt er null
0.1.15  | endret       | `CorrelationId`: genererer correlation id med `-` uten `()`
0.1.14  | opprettet    | `SoapSamlCallback`: feil ved deploy av versjon 0.1.13
0.1.13  | opprettet    | `SoapSamlCallback`: `SoapActionCallback` i en web service med et saml-token
0.1.12  | endring      | `EnhetFilter`: tar vare på enhetsinformasjon i ThreadLocal     
0.1.11  | endring      | `EnhetFilter`: error logger hvis ServletRequest ikke er en HttpServletRequest     
0.1.10  | opprettet    | `HttpStatusResponse.isNotSuccessful()`:   
0.1.9   | opprettet    | `KildesystemIdenfikator.hentJournalpostId()`:   
0.1.8   | opprettet    | `KildesystemIdenfikator`: til å identifisere  
0.1.7   | endring      | fix vulnerabilities reported on snyk.io 
0.1.6   | endring      | `ExceptionLogger`: logger exception sett fra nav kode uten exception cause også 
0.1.5   | endring      | `ExceptionLogger`: logger ikke egne innslag for exception uten cause 
0.1.3   | endring      | `ExceptionLogger`: logger siste StackTraceElement fra no.nav før exception og skipper logging fra stack 
0.1.2   | endring      | `ExceptionLogger`: utbedret logging, samt redusert logging til å bare logge full stack på root causeœ 
0.1.1   | slettet      | `HttpStatusResponse`: metode `fetchBody()`
0.1.0   | endring      | `HttpStatusResponse`: navn på metoder (`isEmpty()` -> `isBodyEmpty`)
0.1.0   | endring      | `HttpStatusResponse`: ny metode (`fetchBody()`)
0.1.0   | endring      | `HttpStatusResponse`: ny metode (`isBodyPresent()`)
0.0.32  | endret       | `EnhetFilter`: logger ikke kall mot "actuator"-tjenester
0.0.31  | endret       | `EnhetFilter`: fortsetter filtrering med `javax.servlet.FilterChain`
0.0.30  | opprettet    | `EnhetFilter`: et `javax.servlet.Filter` som videresender header med enhetsnummer
0.0.29  | endring      | `HttpStatusResponse`: konstruktør bare for `HttpStatus`
0.0.28  | endring      | `HttpStatusResponse`: metode (`isEmpty()`)
0.0.28  | endring      | `HttpHeaderRestTemplate`: akspeterer at et "callback" ikke gjøres med en `HttpEntity`
0.0.27  | endring      | `HttpStatusResponse`: metode (`toString()`)
0.0.25  | opprettet    | `HttpStatusResponse` for å videresende HttpStatus sammen med resultat fra consumer
0.0.23  | endring      | ny java baseline - java 12
0.0.21  | slettet      | `HttpHeaderRestTemplate.Header` removed. Not a backword compatible change!
0.0.19  | endring      | Correlation Id without public constructors and the CorrelationIdFilter will always have the last part of the request uri containing plain text
0.0.17  | endring      | Header name of correlation id is also present on the value bean
0.0.15  | endring      | Check correlation id header on request and name of header X_CORRELATION_ID -> X-Correlation-ID
0.0.13  | endring      | `CorrelationId` as value bean
0.0.11  | endring      | `ExceptionLogger` logger `Throwable` type
0.0.9   | endring      | `ExceptionLogger` logger `Exception` cause
0.0.8   | opprettet    | `ExceptionLogger`
0.0.6   | endring      | `CorrelationIdFilter` legger generert id på `ThreadLocal`
0.0.4   | endring      | `HttpHeaderRestTemplate.Header` fra et interface til en klasse for enklere bruk
0.0.1   | opprettet    | `CorrelationIdFilter` og `HttpHeaderRestTemplate`
