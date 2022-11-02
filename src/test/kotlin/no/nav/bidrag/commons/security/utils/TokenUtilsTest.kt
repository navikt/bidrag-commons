package no.nav.bidrag.commons.security.utils

import no.nav.bidrag.commons.security.utils.TokenUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TokenUtilsTest {
    // Generated using http://jwtbuilder.jamiekurtz.com/
    private val issoUser =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2lzc28tcS5hZGVvLm5vOjQ0My9pc3NvL29hdXRoMiIsImlhdCI6MTY1NTg3NzQyNCwiZXhwIjoxNjg3NDEzNDI0LCJhdWQiOiJiaWRyYWctdWktZmVhdHVyZS1xMSIsInN1YiI6Ilo5OTQ5NzciLCJ0b2tlbk5hbWUiOiJpZF90b2tlbiIsImF6cCI6ImJpZHJhZy11aS1mZWF0dXJlLXExIn0.NYxxExStmzxqvjf-uKn7EnT9rOzluRxipclj0IH_0XQ"
    private val stsToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL3NlY3VyaXR5LXRva2VuLXNlcnZpY2UubmFpcy5wcmVwcm9kLmxvY2FsIiwiaWF0IjoxNjU1ODc3NDI0LCJleHAiOjE2ODc0MTM0MjQsImF1ZCI6InNydmJpc3lzIiwic3ViIjoic3J2YmlzeXMiLCJpZGVudFR5cGUiOiJTeXN0ZW1yZXNzdXJzIiwiYXpwIjoic3J2YmlzeXMifQ.ivpkYHclkl9z3fOfCSIMKKOsRSOGzr-y9AqerJEy9BA"
    private val azureSystemToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vdGVzdC92Mi4wIiwiaWF0IjoxNjU1ODc3MDQwLCJleHAiOjE2ODc0MTMwNDAsImF1ZCI6IjY3NjY2NDUtNTNkNS00OGY5LWJlOTctOTljN2ZjNzRmMDlhIiwic3ViIjoiNTU1NTU1LTUzZDUtNDhmOS1iZTk3LTk5YzdmYzc0ZjA5YSIsImF6cF9uYW1lIjoiZGV2LWZzczpiaWRyYWc6YmlkcmFnLWRva3VtZW50LWZlYXR1cmUiLCJyb2xlcyI6WyJhY2Nlc3NfYXNfYXBwbGljYXRpb24iLCJzb21ldGhpbmcgZWxzZSJdfQ.XvdyJCtIt-ME4t956z76xOf2hrkM7WOvTRWjI6QcYiA"
    private val azureUserToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vdGVzdC92Mi4wIiwiaWF0IjoxNjU1ODc3MDQwLCJleHAiOjE2ODc0MTMwNDAsImF1ZCI6IjY3NjY2NDUtNTNkNS00OGY5LWJlOTctOTljN2ZjNzRmMDlhIiwic3ViIjoiNTU1NTU1LTUzZDUtNDhmOS1iZTk3LTk5YzdmYzc0ZjA5YSIsImF6cF9uYW1lIjoiZGV2LWZzczpiaWRyYWc6YmlkcmFnLXVpLWZlYXR1cmUiLCJSb2xlIjoiYWNjZXNzX2FzX2FwcGxpY2F0aW9uIiwiTkFWaWRlbnQiOiJaOTk0OTc3In0.7XhNn27iaKY-z4voUp-ZfR__5u3Rv5rJCgTpSNVW1nY"

    @Test
    fun skalHenteSubjectFraAzureSystemToken() {
        val subject = TokenUtils.fetchSubject(azureSystemToken)

        // then
        assertThat(subject).isEqualTo("bidrag-dokument-feature")
    }

    @Test
    fun skalHenteSubjectFraAzureToken() {
        val subject = TokenUtils.fetchSubject(azureUserToken)

        // then
        assertThat(subject).isEqualTo("Z994977")
    }

    @Test
    fun skalHenteSubjectFraIssoToken() {
        val subject = TokenUtils.fetchSubject(issoUser)

        // then
        assertThat(subject).isEqualTo("Z994977")
    }

    @Test
    fun skalHenteAppNavnFraIssoToken() {
        val subject = TokenUtils.fetchAppName(issoUser)

        // then
        assertThat(subject).isEqualTo("bidrag-ui-feature-q1")
    }

    @Test
    fun skalHenteAppNavnFraAzureToken() {
        val subject = TokenUtils.fetchAppName(azureUserToken)

        // then
        assertThat(subject).isEqualTo("bidrag-ui-feature")
    }

    @Test
    fun shouldValidateSystemToken() {

        // when
        val resultAzure = TokenUtils.isSystemUser(azureSystemToken)
        val resultSTS = TokenUtils.isSystemUser(stsToken)
        val resultAzureUser = TokenUtils.isSystemUser(azureUserToken)
        val resultIsso = TokenUtils.isSystemUser(issoUser)

        // then
        assertThat(resultAzure).isTrue
        assertThat(resultSTS).isTrue
        assertThat(resultAzureUser).isFalse
        assertThat(resultIsso).isFalse
    }

}