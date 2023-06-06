package com.example.springbootpractice.member.service;

import com.example.springbootpractice.member.dto.LoginRequestDto;
import com.example.springbootpractice.member.dto.LoginResponseDto;
import com.example.springbootpractice.member.entity.Member;

import java.util.List;

public interface MemberService {
    public List<Member> findMembers();
    public LoginResponseDto login(LoginRequestDto request);
    public boolean register(LoginRequestDto request) throws Exception;
    public LoginResponseDto getMember(String account) throws Exception;
}
