package com.example.aitemplate.infra.http;

import com.example.aitemplate.core.PublicApi;
import com.example.aitemplate.infra.security.JwtTokenFilter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtTokenFilter jwtTokenFilter;
    private final RequestMappingHandlerMapping handlerMapping;

    @Value("${app.features.auth-enabled:false}")
    private boolean authEnabled;

    public SecurityConfig(JwtTokenFilter jwtTokenFilter,
                          @Qualifier("requestMappingHandlerMapping")
                          RequestMappingHandlerMapping handlerMapping) {
        this.jwtTokenFilter = jwtTokenFilter;
        this.handlerMapping = handlerMapping;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new CorsConfiguration();
                    config.setAllowedOriginPatterns(List.of("*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!authEnabled) {
            log.info("Auth disabled (app.features.auth-enabled=false) — all endpoints are public");
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        } else {
            String[] publicPatterns = collectPublicPatterns();
            if (publicPatterns.length <= 3) {
                log.warn("Auth enabled but only {} public patterns detected — @PublicApi scan may have failed: {}",
                        publicPatterns.length, List.of(publicPatterns));
            } else {
                log.info("Auth enabled — {} public endpoints: {}", publicPatterns.length, List.of(publicPatterns));
            }

            http
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(publicPatterns).permitAll()
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated()
                    )
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint((req, res, e) ->
                                    res.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                    )
                    .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    /**
     * Scans all handler methods for {@link PublicApi} annotations and collects
     * the corresponding URL patterns. Merges with a fixed infrastructure whitelist.
     */
    private String[] collectPublicPatterns() {
        Set<String> patterns = new LinkedHashSet<>();

        // Fixed infrastructure whitelist
        patterns.add("/swagger-ui/**");
        patterns.add("/v3/api-docs/**");
        patterns.add("/actuator/**");

        // Scan @PublicApi on classes and methods
        for (var entry : handlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod method = entry.getValue();

            boolean isPublic = method.getBeanType().isAnnotationPresent(PublicApi.class)
                    || method.hasMethodAnnotation(PublicApi.class);

            if (isPublic) {
                // getPatternValues() works for both AntPathMatcher and PathPattern
                patterns.addAll(mappingInfo.getPatternValues());
            }
        }

        return patterns.toArray(String[]::new);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
