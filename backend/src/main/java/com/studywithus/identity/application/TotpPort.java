package com.studywithus.identity.application;

public interface TotpPort {
    String generateSecret();
    String buildOtpAuthUri(String email, String secret);
    boolean verifyCode(String base32Secret, String code);
}
