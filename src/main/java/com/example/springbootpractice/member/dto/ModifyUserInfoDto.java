package com.example.springbootpractice.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModifyUserInfoDto {
    private Long id;
    private String email;
    private  String name;
    private  String phone;
    private String code;
    private String oldPassword;
    private String newPassword;
}