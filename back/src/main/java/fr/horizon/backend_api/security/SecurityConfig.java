package fr.horizon.backend_api.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import fr.horizon.backend_api.model.enums.RoleEnum;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Value("#{'${cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/auth/logout").authenticated()

                        .requestMatchers(HttpMethod.DELETE, "/events/*/cancel").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/events/*/participants/*").authenticated()

                        .requestMatchers(HttpMethod.GET, "/events/invitations/me", "/events/*/invitations/status").authenticated()

                        .requestMatchers(HttpMethod.PUT, "/events/*/invitations/respond").authenticated()

                        .requestMatchers(HttpMethod.POST, "/events/*/invitations/").authenticated()

                        .requestMatchers(HttpMethod.GET, "/events/calendar").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/events/**", "/groups/**")
                            .hasAnyAuthority(RoleEnum.ADMIN.value(), RoleEnum.TEACHER.value())

                        .requestMatchers("/auth/me", "/categories/**", "/groups/**").authenticated()

                        .requestMatchers("/admin/**")
                        .hasAuthority(RoleEnum.ADMIN.value())

                        .requestMatchers("/users/**")
                        .hasAnyAuthority(RoleEnum.ADMIN.value(), RoleEnum.TEACHER.value())

                        .requestMatchers("/teacher/**")
                        .hasAnyAuthority(RoleEnum.TEACHER.value(), RoleEnum.ADMIN.value())

                        .requestMatchers("/student/**")
                        .hasAnyAuthority(
                                RoleEnum.STUDENT.value(),
                                RoleEnum.TEACHER.value(),
                                RoleEnum.ADMIN.value())

                        .requestMatchers("/api/events/**").permitAll()

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(allowedOrigins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
