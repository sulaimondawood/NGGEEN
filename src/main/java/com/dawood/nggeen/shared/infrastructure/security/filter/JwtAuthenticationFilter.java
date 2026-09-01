package com.dawood.nggeen.shared.infrastructure.security.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.dawood.nggeen.shared.dto.ApiError;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.AuthenticationException;
import com.dawood.nggeen.shared.infrastructure.security.jwt.JwtService;
import com.dawood.nggeen.shared.infrastructure.security.service.CustomUserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final CustomUserDetailsImpl customUserDetails;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            DecodedJWT claims = jwtService.verifyAndDecodeToken(token);
            String subject = claims.getSubject();

            UserDetails userDetails = customUserDetails.loadUserByUsername(subject);

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            filterChain.doFilter(request, response);
        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, e.getStatus().value(), e.getCode(), e.getMessage(), request.getRequestURI());
        } catch (Exception e) {
            log.error("Unexpected error during JWT authentication processing", e);
            sendErrorResponse(
                    response,
                    HttpStatus.UNAUTHORIZED.value(),
                    ErrorCode.UNAUTHORIZED,
                    "Authentication failed",
                    request.getRequestURI()
            );

        }

    }

    private void sendErrorResponse(HttpServletResponse response,
                                   int status,
                                   ErrorCode code,
                                   String message,
                                   String path) throws IOException {
        ApiError error = ApiError.of(status, code, message, path);

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
