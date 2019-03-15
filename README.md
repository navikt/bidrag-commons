# bidrag-commons
Komponenter som brukes på tvers av applikasjoner under bidrag

Det finnes et annet felles-bibliotek (`bidrag-commons-test`) som utelukkende er laget for å
gjenbruke komponenter til enhetstesting. 

## release endringer

versjon | beskrivelse
--------|------------------------
0.0.19  | Correlation Id without public constructors and the CorrelationIdFilter will always have the last part of the request uri containing plain text
0.0.17  | Header name of correlation id is also present on the value bean
0.0.15  | Check correlation id header on request and name of header X_CORRELATION_ID -> X-Correlation-ID
0.0.13  | `CorrelationId` as value bean
0.0.11  | `ExceptionLogger` logger `Throwable` type
0.0.9   | `ExceptionLogger` logger `Exception` cause
0.0.8   | `ExceptionLogger` er opprettet
0.0.6   | `CorrelationIdFilter` legger generert id på `ThreadLocal`
0.0.4   | `HttpHeaderRestTemplate.Header` fra et interface til en klasse for enklere bruk
0.0.1   | opprettet med `CorrelationIdFilter` og `HttpHeaderRestTemplate`
