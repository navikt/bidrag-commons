# bidrag-commons
Komponenter som brukes på tvers av applikasjoner under bidrag

Det finnes et annet felles-bibliotek (`bidrag-commons-test`) som utelukkende er laget for å
gjenbruke komponenter til enhetstesting. 

## continuous integration and deployment

Gjøres med 'workflows' og 'actions' fra GitHub. Se `.github/workflows/*` og `.github/actions/*` for detaljer. 

## release endringer

versjon | endringstype | beskrivelse
--------|--------------|------------
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
