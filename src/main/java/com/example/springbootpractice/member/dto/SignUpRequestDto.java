package com.example.springbootpractice.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequestDto {
    private Long id;
    private String email;
    private  String name;
    private  String phone;
    private String code;
    private String password;
}