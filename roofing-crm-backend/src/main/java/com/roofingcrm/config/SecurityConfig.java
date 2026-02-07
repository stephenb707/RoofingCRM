package com.roofingcrm.config;

import com.roofingcrm.security.JwtAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.roofingcrm.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

        private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                       JwtService jwtService,
                                                       SecurityErrorHandlers errorHandlers,
                                                       CorsProperties corsProperties) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource(corsProperties)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(errorHandlers.authenticationEntryPoint())
                    .accessDeniedHandler(errorHandlers.accessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/api/public/**").permitAll()
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/ws", "/ws/**").permitAll()
                    .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        
            return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
                log.info("CORS configured: allowedOrigins={}, allowedHeaders={}, allowCredentials={}",
                        corsProperties.getAllowedOrigins(),
                        corsProperties.getAllowedHeaders(),
                        corsProperties.isAllowCredentials());

                if (corsProperties.isAllowCredentials()
                        && corsProperties.getAllowedOrigins().contains("*")) {
                    throw new IllegalStateException(
                            "CORS wildcard origin (*) cannot be used when allowCredentials is true. "
                                    + "Use specific origins or allowedOriginPatterns instead.");
                }

                CorsConfiguration config = new CorsConfiguration();
                if (corsProperties.getAllowedOriginPatterns() != null
                        && !corsProperties.getAllowedOriginPatterns().isEmpty()) {
                    config.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
                } else {
                    config.setAllowedOrigins(corsProperties.getAllowedOrigins());
                }
                config.setAllowedMethods(corsProperties.getAllowedMethods());
                config.setAllowedHeaders(corsProperties.getAllowedHeaders());
                config.setAllowCredentials(corsProperties.isAllowCredentials());
                config.setMaxAge(corsProperties.getMaxAge());

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
