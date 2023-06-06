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
    private List<Authority> roles = new ArrayList<>();
    private String token;

    public LoginResponseDto(Member member) {
        this.id = member.getId();
        this.email = member.getEmail();
        this.name = member.getName();
        this.roles = member.getRoles();
    }
}