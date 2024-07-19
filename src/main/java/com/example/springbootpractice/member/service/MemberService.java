package com.example.springbootpractice.member.service;

import com.example.springbootpractice.member.dto.*;
import com.example.springbootpractice.member.entity.Member;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface MemberService {
    List<Member> findMembers();
    LoginResponseDto logIn(LoginRequestDto request, HttpServletResponse response);
    LoginResponseDto refreshUserInfo(String refreshToken);
    void logOut(String token);
    boolean register(SignUpRequestDto request) throws Exception;
    String findRegisteredEmail(String phone, String code) throws Exception;
    String forwardTempPassword(String email, String phone, String code) throws Exception;
    LoginResponseDto getMember(String account) throws Exception;

    void sendCodeToEmail(String email) throws Exception;
    boolean verifiedCode(String email, String authCode);
    void modifyUserPassword(String accessToken, ModifyUserInfoDto modifyUserInfoDto) throws Exception;
    LoginResponseDto modifyUserInfo(String accessToken, ModifyUserInfoDto modifyUserInfoDto) throws Exception;
    void withdraw(String token, String password);
}
