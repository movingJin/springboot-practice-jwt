package com.example.springbootpractice.member.service;

import com.example.springbootpractice.member.dto.LoginRequestDto;
import com.example.springbootpractice.member.dto.LoginResponseDto;
import com.example.springbootpractice.member.entity.Member;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface MemberService {
    public List<Member> findMembers();
    public LoginResponseDto logIn(LoginRequestDto request, HttpServletResponse response);
    void logOut(String token);
    public boolean register(LoginRequestDto request) throws Exception;
    public LoginResponseDto getMember(String account) throws Exception;
}
