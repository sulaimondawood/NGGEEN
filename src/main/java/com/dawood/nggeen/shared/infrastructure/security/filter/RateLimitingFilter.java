package com.dawood.nggeen.shared.infrastructure.security.filter;

import com.dawood.nggeen.shared.dto.ApiError;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.infrastructure.security.ratelimit.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String ip = clientIp(request);
        String path = request.getRequestURI();
        String key = ip + ":" + path;

        long capacity;
        Duration window;

        if (path.contains("login")) {
            capacity = 10;
            window = Duration.ofMinutes(15);
        } else if (path.contains("/register")) {
            capacity = 5;
            window = Duration.ofHours(1);
        } else if (path.contains("/refresh")) {
            capacity = 60;
            window = Duration.ofMinutes(1);
        } else if (path.contains("/verify-2fa") || path.contains("/2fa/confirm")) {
            capacity = 10;
            window = Duration.ofMinutes(15);
        } else {
            capacity = 10;
            window = Duration.ofMinutes(15);
        }

        if (!rateLimitService.tryConsume(key, capacity, window)) {
            log.warn("Rate limit exceeded ip={} path={}", ip, path);
            ApiError error = ApiError.of(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    ErrorCode.TOO_MANY_REQUESTS,
                    "Too many requests. Please try again later.",
                    path
            );
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
