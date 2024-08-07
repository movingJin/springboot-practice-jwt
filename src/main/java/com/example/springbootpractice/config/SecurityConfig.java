package com.example.springbootpractice.config;

import com.example.springbootpractice.member.security.JwtAuthenticationFilter;
import com.example.springbootpractice.member.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtProvider jwtProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ID, Password 문자열을 Base64로 인코딩하여 전달하는 구조
                .httpBasic().disable()
                // 쿠키 기반이 아닌 JWT 기반이므로 사용하지 않음
                .csrf().disable()
                // CORS 설정
                .cors(c -> {
                            CorsConfigurationSource source = request -> {
                                // Cors 허용 패턴
                                CorsConfiguration config = new CorsConfiguration();
                                config.setAllowedOrigins(
                                        List.of(
                                            "http://192.168.0.3:8083/",
                                            "http://localhost:8083/"
                                        )
                                );
                                config.setMaxAge(3600L);
                                config.setAllowCredentials(true);
                                config.setAllowedMethods(
                                        List.of("PUT", "GET", "POST", "OPTIONS", "DELETE", "PATCH")
                                );
                                config.setExposedHeaders(List.of(
                                        "Authorization",
                                        "X-CSRF-TOKEN"
                                ));
                                config.setAllowedHeaders(List.of(
                                        "Access-Control-Allow-Headers",
                                        "Access-Control-Allow-Origin",
                                        "Access-Control-Expose-Headers",
                                        "Access-control-allow-credentials",
                                        "Accept",
                                        "X-Requested-With",
                                        "Authorization",
                                        "Cache-Control",
                                        "Content-Type",
                                        "Expires",
                                        "Last-Modified",
                                        "Content-Language",
                                        "Pragma",
                                        "Baeldung-Allowed",
                                        "Credential",
                                        "X-AUTH-TOKEN",
                                        "X-CSRF-TOKEN"
                                ));
                                return config;
                            };
                            c.configurationSource(source);
                        }
                )
                // Spring Security 세션 정책 : 세션을 생성 및 사용하지 않음
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 조건별로 요청 허용/제한 설정
                .authorizeRequests()
                // 회원가입과 로그인은 모두 승인
                .antMatchers("/register", "/login", "/emails/send-authcode", "/emails/verifications", "/find-email", "/find-pwd", "/modify-pwd", "/modify-info").permitAll()
                // /admin으로 시작하는 요청은 ADMIN 권한이 있는 유저에게만 허용
                .antMatchers("/admin/**").hasRole("ADMIN")
                // /user 로 시작하는 요청은 USER 권한이 있는 유저에게만 허용
                .antMatchers("/user/**").hasRole("USER")
                .anyRequest().denyAll()
                .and()
                // JWT 인증 필터 적용
                .addFilterBefore(new JwtAuthenticationFilter(redisTemplate, jwtProvider), UsernamePasswordAuthenticationFilter.class)
                // 에러 핸들링
                .exceptionHandling()
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                        // 권한 문제가 발생했을 때 이 부분을 호출한다.
                        response.setStatus(403);
                        response.setCharacterEncoding("utf-8");
                        response.setContentType("text/html; charset=UTF-8");
                        response.getWriter().write("권한이 없는 사용자입니다.");
                    }
                })
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                        // 인증문제가 발생했을 때 이 부분을 호출한다.
                        response.setStatus(401);
                        response.setCharacterEncoding("utf-8");
                        response.setContentType("text/html; charset=UTF-8");
                        response.getWriter().write("Unauthenticated user.");
                    }
                });
//                .and()
//                .formLogin()
//                .loginPage("/login")
//                .defaultSuccessUrl("/");

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}