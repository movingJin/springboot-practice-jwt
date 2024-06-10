package com.example.springbootpractice.member.dto;

import com.example.springbootpractice.member.entity.Authority;
import com.example.springbootpractice.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private List<Authority> roles = new ArrayList<>();
    private TokenDto tokens;

    public LoginResponseDto(Member member) {
        this.id = member.getId();
        this.email = member.getEmail();
        this.name = member.getName();
        this.phone = member.getPhone();
        this.roles = member.getRoles();
    }
}