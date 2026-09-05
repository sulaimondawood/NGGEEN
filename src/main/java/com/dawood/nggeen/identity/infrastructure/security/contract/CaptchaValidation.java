package com.dawood.nggeen.identity.infrastructure.security.contract;

public interface CaptchaValidation {
     void validateCaptcha (String token, String remoteip);
}
