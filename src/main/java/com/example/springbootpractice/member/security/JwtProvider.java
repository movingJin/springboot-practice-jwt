package com.example.springbootpractice.member.security;

import com.example.springbootpractice.member.dto.TokenDto;
import com.example.springbootpractice.member.entity.Authority;
import com.example.springbootpractice.member.service.UserDetailServiceImpl;
import io.jsonwebtoken.Claims;
import com.example.springbootpractice.member.service.MemberServiceImpl;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class JwtProvider {

    @Value("${jwt.secret.key}")
    private String salt;

    private Key secretKey;

    // Access token 만료시간 : 1Hour
    public static final long ACCESS_TIME =  24 * 60 * 60 * 1000L;   //하루
    // Refresh token 만료시간 : 1Hour
    public static final long REFRESH_TIME =  14 * 24 * 60 * 60 * 1000L; //2주
    public static final String ACCESS_TOKEN = "Access_Token";
    public static final String REFRESH_TOKEN = "Refresh_Token";


    private final UserDetailServiceImpl userDetailsService;

    private final RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    protected void init() {
        secretKey = Keys.hmacShaKeyFor(salt.getBytes(StandardCharsets.UTF_8));
    }

    // 토큰 생성
    public String createToken(String account, List<Authority> roles, String type) {
        long time = type.equals(ACCESS_TOKEN) ? ACCESS_TIME : REFRESH_TIME;
        Claims claims = Jwts.claims().setSubject(account);
        claims.put("roles", roles);
        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + time))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // header 토큰을 가져오는 기능
    public String getHeaderToken(HttpServletRequest request, String type) {
        return type.equals(ACCESS_TOKEN) ? request.getHeader(ACCESS_TOKEN) :request.getHeader(REFRESH_TOKEN);
    }

    // 토큰 생성
    public TokenDto createAllToken(String email, List<Authority> roles) {
        return new TokenDto(createToken(email, roles, ACCESS_TOKEN), createToken(email, roles, REFRESH_TOKEN));
    }

    // 권한정보 획득
    // Spring Security 인증과정에서 권한확인을 위한 기능
    public Authentication getAuthentication(String token) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(this.getAccount(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    // Bearer 검증
    private String getClaimFromToken(String token){
        if (!token.substring(0, "BEARER ".length()).equalsIgnoreCase("BEARER ")) {
            return null;
        } else {
            token = token.split(" ")[1].trim();
        }
        return token;
    }

    // 토큰에 담겨있는 유저 account 획득
    public String getAccount(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(getClaimFromToken(token))
                .getBody()
                .getSubject();
    }

    public List<Authority> getRoles(String token) {
        token = getClaimFromToken(token);
        return (List<Authority>) Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody().get("roles");
    }

    // Authorization Header를 통해 인증을 한다.
    public String resolveToken(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            token = getClaimFromToken(token);
            Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            // 만료되었을 시 false
            return !claims.getBody().getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public Long getExpiration(String accessToken){
        accessToken = getClaimFromToken(accessToken);
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(secretKey).build()
                .parseClaimsJws(accessToken).getBody()
                .getExpiration();
        long now = new Date().getTime();
        return (expiration.getTime() - now);
    }

    public Boolean refreshTokenValidation(String token) {
        // 1차 토큰 검증
        String refreshToken = getClaimFromToken(token);
        if(!validateToken(token)) return false;

        // UserDetail의 username인 Email 정보를 얻어옴
        String email = getAccount(token);
        String tokenFromRedis = (String) redisTemplate.opsForValue().get("RT:"+ email);

        return refreshToken != null && refreshToken.equals(tokenFromRedis);
    }

    // 어세스 토큰 헤더 설정
    public void setHeaderAccessToken(HttpServletResponse response, String accessToken) {
        response.setHeader(ACCESS_TOKEN, accessToken);
    }

    // 리프레시 토큰 헤더 설정
    public void setHeaderRefreshToken(HttpServletResponse response, String refreshToken) {
        response.setHeader(REFRESH_TOKEN, refreshToken);
    }
}