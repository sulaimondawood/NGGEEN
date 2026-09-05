package com.dawood.nggeen.identity.api.rest;

import com.dawood.nggeen.identity.api.rest.dto.*;
import com.dawood.nggeen.identity.application.AuthApplicationService;
import com.dawood.nggeen.identity.service.TokenService;
import com.dawood.nggeen.shared.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        ResponseCookie cookie = tokenService.generateRefreshTokenCookie(result.refreshToken(), result.refreshDuration());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(result.loginResponse(),
                        "Your request was successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshAccessToken(@CookieValue(value = "refresh_token", required = false) String refreshToken, HttpServletRequest request) {

        String clientIp = extractClientIp(request);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);

        RefreshResult response = applicationService.refreshAccessToken(refreshToken, clientIp, userAgent);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, response.cookie().toString())
                .body(ApiResponse.success(response.accessToken(), "Your request was successful"));
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
