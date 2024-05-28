package com.example.springbootpractice.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequestDto {
    private Long id;
    private  String name;
    private String email;
    private String code;
    private String password;
}