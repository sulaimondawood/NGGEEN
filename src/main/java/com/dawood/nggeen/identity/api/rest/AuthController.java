package com.dawood.nggeen.identity.api.rest;

import com.dawood.nggeen.identity.api.rest.dto.*;
import com.dawood.nggeen.identity.application.AuthApplicationService;
import com.dawood.nggeen.identity.service.TokenService;
import com.dawood.nggeen.shared.dto.ApiResponse;
import dev.samstevens.totp.exceptions.QrGenerationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthApplicationService applicationService;
    private final TokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CreateUserResponse>> register(
            @Valid @RequestBody CreateUserRequest payload,
            HttpServletRequest request
    ) {
        CreateUserResponse response = applicationService.createUser(payload, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response,
                "Account created successfully. Please check your email to verify your address (be sure to check your spam/junk folder)."));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam("token") String token) {
        applicationService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.successMessage("Email verified successfully. You can now log in."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest payload,
            HttpServletRequest request
    ) {

        String clientIp = extractClientIp(request);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);

        LoginResult result = applicationService.login(payload, clientIp, userAgent);

        if(result.loginResponse().requires2fa()){
            return ResponseEntity.ok()
                    .body(ApiResponse.success(result.loginResponse(),"Two-factor authentication required"));
        }

        ResponseCookie cookie = tokenService.generateRefreshTokenCookie(result.refreshToken(), result.refreshDuration());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(result.loginResponse(),
                        "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshAccessToken(@CookieValue(value = "refresh_token", required = false) String refreshToken, HttpServletRequest request) {

        String clientIp = extractClientIp(request);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);

        RefreshResult response = applicationService.refreshAccessToken(refreshToken, clientIp, userAgent);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, response.cookie().toString())
                .body(ApiResponse.success(response.accessToken(), "Token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@CookieValue(value = "refresh_token", required = false) String refreshToken) {

        ResponseCookie response = applicationService.logout(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, response.toString())
                .body(ApiResponse.successMessage("Logged out successfully"));
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<ApiResponse<TotpSetupResponse>> setupTOTP() throws QrGenerationException {

        TotpSetupResponse response = applicationService.setup2FA();
        return ResponseEntity.ok()
                .body(ApiResponse.success(response, "2FA setup initialized"));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<ApiResponse<Void>> setupTOTP(@Valid @RequestBody Disable2faRequest request)  {

         applicationService.disable2FA(request);
        return ResponseEntity.ok()
                .body(ApiResponse.successMessage( "2FA disabled successfully"));
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmTOTPSetup(@RequestBody Map<String, String> payload) {
        applicationService.confirm2FASetup(payload.get("code"));
        return ResponseEntity.ok()
                .body(ApiResponse.successMessage("2FA enabled successfully"));
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<ApiResponse<LoginResponse>> verify2fa(@Valid @RequestBody TOTPVerifyRequest payload, HttpServletRequest request) {
        String ip = extractClientIp(request);
        String ua = request.getHeader(HttpHeaders.USER_AGENT);

        LoginResult result = applicationService.verify2fa(payload, ip, ua);

        ResponseCookie cookie = tokenService.generateRefreshTokenCookie(result.refreshToken(), result.refreshDuration());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(result.loginResponse(), "Login successful"));
    }


    private String extractClientIp(HttpServletRequest request) {
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp;
        }
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

}
