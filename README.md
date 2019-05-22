# bidrag-commons
Komponenter som brukes på tvers av applikasjoner under bidrag

Det finnes et annet felles-bibliotek (`bidrag-commons-test`) som utelukkende er laget for å
gjenbruke komponenter til enhetstesting. 

## release endringer

versjon | endringstype | beskrivelse
--------|--------------|------------
0.0.?   | endring      | `HttpHeaderRestTemplate`: akspeterer at et "callback" ikke gjøres med en `HttpEntity`
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
