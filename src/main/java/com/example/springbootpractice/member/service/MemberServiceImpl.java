package com.example.springbootpractice.member.service;

import com.example.springbootpractice.member.dto.LoginRequestDto;
import com.example.springbootpractice.member.dto.LoginResponseDto;
import com.example.springbootpractice.member.dto.TokenDto;
import com.example.springbootpractice.member.entity.Authority;
import com.example.springbootpractice.member.entity.Member;
import com.example.springbootpractice.member.repository.MemberRepository;
import com.example.springbootpractice.member.security.CustomUserDetails;
import com.example.springbootpractice.member.security.JwtProvider;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService{
    private final RedisTemplate<String, Object> redisTemplate;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Override
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    @Override
    @Transactional
    public LoginResponseDto logIn(LoginRequestDto request, HttpServletResponse response) {
        Member member = memberRepository.findByEmail(request.getEmail()).orElseThrow(() ->
                new BadCredentialsException("잘못된 계정정보입니다."));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BadCredentialsException("잘못된 계정정보입니다.");
        }

        // 아이디 정보로 Token생성
        TokenDto tokenDto = jwtProvider.createAllToken(member.getEmail(), member.getRoles());

        redisTemplate.opsForValue().set(
                "RT:"+member.getEmail(),
                tokenDto.getRefreshToken(),
                JwtProvider.REFRESH_TIME,
                TimeUnit.MILLISECONDS);

        return LoginResponseDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .roles(member.getRoles())
                .tokens(tokenDto)
                .build();
    }

    @Override
    @Transactional
    public void logOut(String token) {
        // 로그아웃 하고 싶은 토큰이 유효한 지 먼저 검증하기
        if (!jwtProvider.validateToken(token)){
            throw new IllegalArgumentException("로그아웃 : 유효하지 않은 토큰입니다.");
        }

        // Access Token에서 User email을 가져온다
        Authentication authentication = jwtProvider.getAuthentication(token);

        // Redis에서 해당 User email로 저장된 Refresh Token 이 있는지 여부를 확인 후에 있을 경우 삭제를 한다.
        if (redisTemplate.opsForValue().get("RT:"+authentication.getName())!=null){
            // Refresh Token을 삭제
            redisTemplate.delete("RT:"+authentication.getName());
        }

        // 해당 Access Token 유효시간을 가지고 와서 BlackList에 저장하기
        Long expiration = jwtProvider.getExpiration(token);
        redisTemplate.opsForValue().set(token,"logout",expiration,TimeUnit.MILLISECONDS);
    }

    @Override
    @Transactional
    public boolean register(LoginRequestDto request) throws Exception {
        try {
            Member member = Member.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .name(request.getName())
                    .build();
            member.setRoles(Collections.singletonList(Authority.builder().name("ROLE_USER").build()));

            memberRepository.save(member);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new Exception("잘못된 요청입니다.");
        }
        return true;
    }

    @Override
    public LoginResponseDto getMember(String account) throws Exception {
        Member member = memberRepository.findByEmail(account)
                .orElseThrow(() -> new Exception("계정을 찾을 수 없습니다."));
        return new LoginResponseDto(member);
    }

    private void setHeader(HttpServletResponse response, TokenDto tokenDto) {
        response.addHeader(JwtProvider.ACCESS_TOKEN, tokenDto.getAccessToken());
        response.addHeader(JwtProvider.REFRESH_TOKEN, tokenDto.getRefreshToken());
    }
}
