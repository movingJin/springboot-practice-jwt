package com.example.springbootpractice.member.security;

import com.example.springbootpractice.member.entity.Authority;
import com.example.springbootpractice.member.service.UserDetailServiceImpl;
import io.jsonwebtoken.Claims;
import com.example.springbootpractice.member.service.MemberServiceImpl;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Component
public class JwtProvider {

    @Value("${jwt.secret.key}")
    private String salt;

    private Key secretKey;

    // 만료시간 : 1Hour
    public final long exp = 1000L * 60 * 60;

    private final UserDetailServiceImpl userDetailsService;

    @PostConstruct
    protected void init() {
        secretKey = Keys.hmacShaKeyFor(salt.getBytes(StandardCharsets.UTF_8));
    }

    // 토큰 생성
    public String createToken(String account, List<Authority> roles) {
        Claims claims = Jwts.claims().setSubject(account);
        claims.put("roles", roles);
        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + exp))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
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
        token = getClaimFromToken(token);
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody().getSubject();
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
}