package com.example.springbootpractice.common;

import lombok.Getter;

@Getter
public enum ExceptionCode {
    UNABLE_TO_SEND_EMAIL(100, "Unable to send email."),
    MEMBER_EXISTS(101, "Member already registered."),
    NO_SUCH_ALGORITHM(102, "No such algorithm."),
    AUTH_CODE_NOT_VALID(103, "Auth code is not valid."),
    MEMBER_NOT_FOUND(104, "Member not found.");

    private final int status;

    private final String message;

    ExceptionCode(int status, String message) {
        this.status = status;
        this.message = message;
    }
}