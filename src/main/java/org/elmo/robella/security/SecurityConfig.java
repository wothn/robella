package org.elmo.robella.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.config.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    
    private final SecurityProperties securityProperties;
    private final JwtAuthenticationManager jwtAuthenticationManager;
    private final SecurityContextRepository securityContextRepository;
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        if (!securityProperties.isEnabled()) {
            return http.authorizeExchange(exchanges -> exchanges
                    .anyExchange().permitAll())
                    .build();
        }
        
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                
                .securityContextRepository(securityContextRepository)
                .authenticationManager(jwtAuthenticationManager)
                
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(securityProperties.getPublicEndpoints().toArray(new String[0])).permitAll()
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers(securityProperties.getAdminEndpoints().toArray(new String[0])).hasRole("ADMIN")
                        .anyExchange().authenticated()
                )
                
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((exchange, ex) -> {
                            log.debug("Authentication failed: {}", ex.getMessage());
                            return Mono.fromRunnable(() -> 
                                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED));
                        })
                        .accessDeniedHandler((exchange, ex) -> {
                            log.debug("Access denied: {}", ex.getMessage());
                            return Mono.fromRunnable(() -> 
                                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN));
                        })
                )
                
                .cors(cors -> cors.configurationSource(request -> {
                    SecurityProperties.CorsProperties corsProps = securityProperties.getCors();
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(corsProps.getAllowedOrigins());
                    config.setAllowedMethods(corsProps.getAllowedMethods());
                    config.setAllowedHeaders(corsProps.getAllowedHeaders());
                    config.setAllowCredentials(corsProps.isAllowCredentials());
                    return config;
                }))
                
                .build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}