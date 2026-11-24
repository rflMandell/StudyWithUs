package com.studywithus.shared.infrastructure.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    @Test
    void shouldMaskBearerTokensJwtsPasswordsAndSecrets() {
        String raw = "Authorization: Bearer abc123.def456.ghi789 " +
                "jwt=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NSJ9.signature123 " +
                "{\"password\":\"mySecret123\",\"refreshToken\":\"ref-999\"} " +
                "totpSecret=JBSWY3DPEHPK3PXP";

        String masked = SensitiveDataMasker.mask(raw);

        assertThat(masked).doesNotContain("abc123.def456.ghi789");
        assertThat(masked).doesNotContain("eyJhbGciOiJIUzI1NiJ9");
        assertThat(masked).doesNotContain("mySecret123");
        assertThat(masked).doesNotContain("ref-999");
        assertThat(masked).doesNotContain("JBSWY3DPEHPK3PXP");
        assertThat(masked).contains("***MASKED***");
        assertThat(masked).contains("***JWT_MASKED***");
    }
}
