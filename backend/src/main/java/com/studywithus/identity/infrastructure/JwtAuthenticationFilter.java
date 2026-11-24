package com.studywithus.identity.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.identity.domain.UserCredential;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.infrastructure.web.TraceIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String jwt = null;

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else if (StringUtils.hasText(request.getParameter("token"))) {
            jwt = request.getParameter("token");
        }

        if (!StringUtils.hasText(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtService.isTokenValid(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                UUID userId = jwtService.extractUserId(jwt);
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                if (!userDetails.isEnabled()) {
                    log.debug("User {} is inactive/disabled; skipping authentication", userId);
                    filterChain.doFilter(request, response);
                    return;
                }

                if (request.getRequestURI().startsWith("/api/v1/admin")
                        && userDetails instanceof SecurityUserPrincipal principal
                        && principal.getGlobalRole().requiresTwoFactorEligibility()) {
                    boolean totpEnabled = userRepository.findCredentialByUserId(userId)
                            .map(UserCredential::isTotpEnabled)
                            .orElse(false);
                    if (totpEnabled && !jwtService.isTwoFactorVerified(jwt)) {
                        log.warn("Admin user {} attempted to access {} without 2FA verification", userId, request.getRequestURI());
                        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        objectMapper.writeValue(response.getOutputStream(), Map.of(
                                "timestamp", Instant.now().toString(),
                                "status", HttpServletResponse.SC_FORBIDDEN,
                                "code", "TOTP_VERIFICATION_REQUIRED",
                                "message", "Two-factor authentication verification is required for administrative access",
                                "path", request.getRequestURI(),
                                "traceId", traceId != null ? traceId : "unknown"
                        ));
                        return;
                    }
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            log.debug("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
