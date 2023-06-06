package com.example.springbootpractice.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {
    private Long id;
    private  String name;
    private String email;
    private String password;
}