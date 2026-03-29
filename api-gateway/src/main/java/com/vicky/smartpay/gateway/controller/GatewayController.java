package com.vicky.smartpay.gateway.controller;

import com.vicky.smartpay.gateway.filter.JwtUtil;
import com.vicky.smartpay.gateway.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Enumeration;
import java.util.Map;

@RestController
public class GatewayController {

    private final JwtUtil jwtUtil;
    private final RateLimitService rateLimitService;
    private final RestTemplate restTemplate;

    @Value("${services.user}")
    private String userServiceUrl;

    @Value("${services.payment}")
    private String paymentServiceUrl;

    @Value("${services.wallet}")
    private String walletServiceUrl;

    @Value("${services.notification}")
    private String notificationServiceUrl;

    // Public endpoints — no JWT needed
    private static final java.util.List<String> PUBLIC_PATHS = java.util.List.of(
            "/api/users/register",
            "/api/users/login"
    );

    public GatewayController(JwtUtil jwtUtil,
                             RateLimitService rateLimitService,
                             RestTemplate restTemplate) {
        this.jwtUtil = jwtUtil;
        this.rateLimitService = rateLimitService;
        this.restTemplate = restTemplate;
    }

    @RequestMapping("/**")
    public ResponseEntity<?> route(HttpServletRequest request,
                                   @RequestBody(required = false) String body) {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Step 1 — Check if public path
        boolean isPublic = PUBLIC_PATHS.stream()
                .anyMatch(path::startsWith);

        Long userId = null;

        if (!isPublic) {
            // Step 2 — Validate JWT
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired token"));
            }

            userId = jwtUtil.extractUserId(token);

            // Step 3 — Rate limiting
            if (!rateLimitService.isAllowed(userId)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error",
                                "Rate limit exceeded. Max 10 requests per minute."));
            }
        }

        // Step 4 — Route to correct service
        String targetUrl = resolveTargetUrl(path);
        if (targetUrl == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No service found for path: " + path));
        }

        // Step 5 — Forward request
        return forwardRequest(request, targetUrl + path, body);
    }

    private String resolveTargetUrl(String path) {
        if (path.startsWith("/api/users")) return userServiceUrl;
        if (path.startsWith("/api/payments")) return paymentServiceUrl;
        if (path.startsWith("/api/wallets")) return walletServiceUrl;
        if (path.startsWith("/api/notifications")) return notificationServiceUrl;
        return null;
    }

    private ResponseEntity<?> forwardRequest(HttpServletRequest request,
                                             String url, String body) {
        try {
            // Copy headers but EXCLUDE Host header
            HttpHeaders headers = new HttpHeaders();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                // Skip Host header — it causes "Bad authority" error
                if (!headerName.equalsIgnoreCase("host")) {
                    headers.set(headerName, request.getHeader(headerName));
                }
            }
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());

            return restTemplate.exchange(
                    java.net.URI.create(url),
                    httpMethod,
                    entity,
                    String.class);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Service unavailable: " + e.getMessage()));
        }
    }
}