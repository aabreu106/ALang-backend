package com.alang.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration.
 *
 * RESPONSIBILITIES:
 * - Configure JWT authentication
 * - Configure CORS for React frontend
 * - Define public vs protected endpoints
 * - Password encoding
 *
 * TODO: Implement JWT filter
 * TODO: Implement JWT token validation
 * TODO: Add rate limiting
 * TODO: Add CSRF protection for non-JWT endpoints (if any)
 *
 * ARCHITECTURAL NOTE:
 * This is a STUB. Full implementation required:
 * 1. Create JwtAuthenticationFilter to extract and validate JWT from Authorization header
 * 2. Create JwtTokenProvider to generate/validate tokens
 * 3. Configure filter chain to use JWT filter
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Password encoder (BCrypt).
     * Use this in AuthService for password hashing.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security filter chain.
     *
     * TODO: Implement JWT authentication filter
     * TODO: Configure endpoint-level security
     *
     * Current configuration: PERMISSIVE (all endpoints public)
     * This needs to be locked down in production!
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (using JWT, which is immune to CSRF)
            .csrf(csrf -> csrf.disable())

            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless session (JWT-based)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Endpoint authorization
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no authentication required)
                .requestMatchers("/auth/login", "/auth/signup").permitAll()
                .requestMatchers("/meta/**").permitAll() // Language list, starter prompts

                // Protected endpoints (JWT required)
                // TODO: Uncomment when JWT filter is implemented
                // .requestMatchers("/chat/**").authenticated()
                // .requestMatchers("/notes/**").authenticated()
                // .requestMatchers("/review/**").authenticated()
                // .requestMatchers("/auth/me").authenticated()

                // TEMPORARY: Allow all requests (for development)
                // ⚠️ REMOVE THIS IN PRODUCTION
                .anyRequest().permitAll()
            );

        // TODO: Add JWT authentication filter
        // http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration.
     * Allows React frontend to call this API.
     *
     * TODO: Load allowed origins from application.yml
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // TODO: Load from application.yml
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",  // React dev server (Create React App)
            "http://localhost:5173"   // Vite dev server
        ));

        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * TODO: Create JwtAuthenticationFilter
     *
     * This filter should:
     * 1. Extract JWT token from Authorization header
     * 2. Validate token (signature, expiration)
     * 3. Extract user ID from token
     * 4. Set authentication in SecurityContext
     *
     * Example structure:
     * ```
     * @Component
     * public class JwtAuthenticationFilter extends OncePerRequestFilter {
     *     @Override
     *     protected void doFilterInternal(HttpServletRequest request,
     *                                     HttpServletResponse response,
     *                                     FilterChain filterChain) {
     *         String token = extractToken(request);
     *         if (token != null && jwtTokenProvider.validateToken(token)) {
     *             String userId = jwtTokenProvider.getUserIdFromToken(token);
     *             // Set authentication in SecurityContext
     *         }
     *         filterChain.doFilter(request, response);
     *     }
     * }
     * ```
     */

    /**
     * TODO: Create JwtTokenProvider
     *
     * This component should:
     * 1. Generate JWT tokens (called by AuthService)
     * 2. Validate JWT tokens (called by JwtAuthenticationFilter)
     * 3. Extract claims from tokens
     *
     * Use io.jsonwebtoken library (already in pom.xml)
     */
}
