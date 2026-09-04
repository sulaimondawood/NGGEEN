package com.dawood.nggeen.identity.infrastructure.security.contract;

import com.dawood.nggeen.identity.infrastructure.security.dto.CaptchaResponse;

public interface CaptchaValidation {
     CaptchaResponse validateCaptcha (String token, String remoteip);
}
