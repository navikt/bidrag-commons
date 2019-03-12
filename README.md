# bidrag-commons
Komponenter som brukes på tvers av applikasjoner under bidrag

Det finnes et annet felles-bibliotek (`bidrag-commons-test`) som utelukkende er laget for å
gjenbruke komponenter til enhetstesting. 

## release endringer

versjon | beskrivelse
--------|------------------------
0.0.13  | `CorrelationId` as value bean
0.0.11  | `ExceptionLogger` logger `Throwable` type
0.0.9   | `ExceptionLogger` logger `Exception` cause
0.0.8   | `ExceptionLogger` er opprettet
0.0.6   | `CorrelationIdFilter` legger generert id på `ThreadLocal`
0.0.4   | `HttpHeaderRestTemplate.Header` fra et interface til en klasse for enklere bruk
0.0.1   | opprettet med `CorrelationIdFilter` og `HttpHeaderRestTemplate`
