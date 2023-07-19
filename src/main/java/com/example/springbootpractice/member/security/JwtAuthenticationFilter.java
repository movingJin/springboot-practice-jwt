package com.example.springbootpractice.member.security;

import com.example.springbootpractice.member.entity.Authority;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(RedisTemplate<String, Object> redisTemplate, JwtProvider jwtProvider) {
        this.redisTemplate = redisTemplate;
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Access / Refresh 헤더에서 토큰을 가져옴.
        String accessToken = jwtProvider.resolveToken(request);
        String refreshToken = jwtProvider.getHeaderToken(request, "Refresh");

        if(accessToken != null) {
            // 어세스 토큰값이 유효하다면 setAuthentication를 통해
            // security context에 인증 정보저장
            if(jwtProvider.validateToken(accessToken)){
                // check access token
                // Redis에 해당 accessToken logout 여부를 확인
                String isLogout = (String) redisTemplate.opsForValue().get(accessToken);

                // 로그아웃이 없는(되어 있지 않은) 경우 해당 토큰은 정상적으로 작동하기
                if (ObjectUtils.isEmpty(isLogout)) {
                    Authentication auth = jwtProvider.getAuthentication(accessToken);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
            // 어세스 토큰이 만료된 상황 && 리프레시 토큰 또한 존재하는 상황
            else if (refreshToken != null) {
                // 리프레시 토큰 검증 && 리프레시 토큰 DB에서  토큰 존재유무 확인
                boolean isRefreshToken = jwtProvider.refreshTokenValidation(refreshToken);
                // 리프레시 토큰이 유효하고 리프레시 토큰이 DB와 비교했을때 똑같다면
                if (isRefreshToken) {
                    // 리프레시 토큰으로 아이디 정보 가져오기
                    String email = jwtProvider.getAccount(refreshToken);
                    List<Authority> roles = jwtProvider.getRoles(refreshToken);

                    // 새로운 어세스 토큰 발급
                    String newAccessToken = jwtProvider.createToken(email, roles,"Access");
                    // 헤더에 어세스 토큰 추가
                    jwtProvider.setHeaderAccessToken(response, newAccessToken);
                    // Security context에 인증 정보 넣기
                    Authentication auth = jwtProvider.getAuthentication(newAccessToken);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
                // 리프레시 토큰이 만료 || 리프레시 토큰이 DB와 비교했을때 똑같지 않다면
                else {
                    jwtExceptionHandler(response, "RefreshToken Expired", HttpStatus.BAD_REQUEST);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    // Jwt 예외처리
    public void jwtExceptionHandler(HttpServletResponse response, String msg, HttpStatus status) {
        response.setStatus(status.value());
        response.setContentType("application/json");
//        try {
//            String json = new ObjectMapper().writeValueAsString(new GlobalResDto(msg, status.value()));
//            response.getWriter().write(json);
//        } catch (Exception e) {
//            log.error(e.getMessage());
//        }
    }
}