package com.example.springbootpractice.member.service;

import com.example.springbootpractice.common.BusinessLogicException;
import com.example.springbootpractice.common.ExceptionCode;
import com.example.springbootpractice.member.dto.LoginRequestDto;
import com.example.springbootpractice.member.dto.LoginResponseDto;
import com.example.springbootpractice.member.dto.SignUpRequestDto;
import com.example.springbootpractice.member.dto.TokenDto;
import com.example.springbootpractice.member.entity.Authority;
import com.example.springbootpractice.member.entity.Member;
import com.example.springbootpractice.member.repository.MemberRepository;
import com.example.springbootpractice.member.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService{
    private final RedisTemplate<String, Object> redisTemplate;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private static final String AUTH_CODE_PREFIX = "AuthCode ";
    private final MailService mailService;

    @Value("${spring.mail.auth-code-expiration-millis}")
    private long authCodeExpirationMillis;

    @Override
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    @Override
    @Transactional
    public LoginResponseDto logIn(LoginRequestDto request, HttpServletResponse response) {
        Member member = memberRepository.findByEmail(request.getEmail()).orElseThrow(() ->
                new BadCredentialsException("Invalid E-mail Information."));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BadCredentialsException("Password not matched.");
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
                .phone(member.getPhone())
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
    public boolean register(SignUpRequestDto request) {
        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .build();
        member.setRoles(Collections.singletonList(Authority.builder().name("ROLE_USER").build()));
        this.checkDuplicatedEmail(member.getEmail());
        if(!verifiedCode(member.getEmail(), request.getCode())){
            throw new BusinessLogicException(ExceptionCode.AUTH_CODE_NOT_VALID);
        }
        memberRepository.save(member);
        return true;
    }

    @Override
    public String forwardTempPassword(String email, String phone, String code) throws Exception {
        Member member = memberRepository.findByEmail(email).orElseThrow(() ->
                new BadCredentialsException("Invalid E-mail Information."));
        if(!member.getPhone().equals(phone)){
            throw new BadCredentialsException("Phone is not matched.");
        }

        // generate temporary password
        char[] charSet = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
                'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };

        StringBuilder tempPw = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            int idx = (int) (charSet.length * Math.random());
            tempPw.append(charSet[idx]);
        }
        String text = String.format("초기화된 임시비밀번호:\n%s\n\n임시비밀번호로 비밀번호가 초기화되었습니다. MyPage에서 비밀번호를 설정해주세요.", tempPw);
        mailService.sendEmail(email, "모두의전세 임시비밀번호 발송", text);

        // set a member's password as a temporary password
        member.setPassword(passwordEncoder.encode(tempPw));
        memberRepository.save(member);

        return "Temporary password issued.";
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

    @Override
    public void sendCodeToEmail(String toEmail) throws Exception {
        String title = "모두의전세 이메일 인증 번호";
        String authCode = this.createCode();
        mailService.sendEmail(toEmail, title, authCode);
        // 이메일 인증 요청 시 인증 번호 Redis에 저장 ( key = "AuthCode " + Email / value = AuthCode )
        redisTemplate.opsForValue().set(AUTH_CODE_PREFIX + toEmail,
                authCode, Duration.ofMillis(this.authCodeExpirationMillis));
    }

    private void checkDuplicatedEmail(String email) {
        Optional<Member> member = memberRepository.findByEmail(email);
        if (member.isPresent()) {
            log.debug("MemberServiceImpl.checkDuplicatedEmail exception occur email: {}", email);
            throw new BusinessLogicException(ExceptionCode.MEMBER_EXISTS);
        }
    }

    private String createCode() {
        int lenth = 6;
        try {
            Random random = SecureRandom.getInstanceStrong();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < lenth; i++) {
                builder.append(random.nextInt(10));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            log.debug("MemberService.createCode() exception occur");
            throw new BusinessLogicException(ExceptionCode.NO_SUCH_ALGORITHM);
        }
    }

    public boolean verifiedCode(String email, String authCode) {
        this.checkDuplicatedEmail(email);
        String redisAuthCode = (String)redisTemplate.opsForValue().get(AUTH_CODE_PREFIX + email);

        return redisAuthCode != null && redisAuthCode.equals(authCode);
    }
}
