package com.studywithus.identity.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    @Test
    void shouldGenerateValidSecretAndVerifyCurrentCode() {
        String secret = totpService.generateSecret();
        assertThat(secret).isNotBlank();
        assertThat(secret).matches("^[A-Z2-7]+$");

        String code = totpService.generateCurrentCode(secret);
        assertThat(code).matches("^\\d{6}$");
        assertThat(totpService.verifyCode(secret, code)).isTrue();
    }

    @Test
    void shouldRejectInvalidOrMalformedCodes() {
        String secret = totpService.generateSecret();
        assertThat(totpService.verifyCode(secret, "000000")).isFalse();
        assertThat(totpService.verifyCode(secret, "abc123")).isFalse();
        assertThat(totpService.verifyCode(secret, "")).isFalse();
        assertThat(totpService.verifyCode(secret, null)).isFalse();
        assertThat(totpService.verifyCode("INVALID!", "123456")).isFalse();
    }

    @Test
    void shouldBuildOtpAuthUri() {
        String uri = totpService.buildOtpAuthUri("admin@studywithus.com", "JBSWY3DPEHPK3PXP");
        assertThat(uri).startsWith("otpauth://totp/StudyWithUs:");
        assertThat(uri).contains("secret=JBSWY3DPEHPK3PXP");
        assertThat(uri).contains("issuer=StudyWithUs");
    }
}
