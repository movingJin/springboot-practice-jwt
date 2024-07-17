package com.example.springbootpractice.member.controller;

import com.example.springbootpractice.member.dto.LoginRequestDto;
import com.example.springbootpractice.member.dto.LoginResponseDto;
import com.example.springbootpractice.member.dto.ModifyUserInfoDto;
import com.example.springbootpractice.member.dto.SignUpRequestDto;
import com.example.springbootpractice.member.entity.Member;
import com.example.springbootpractice.member.security.JwtProvider;
import com.example.springbootpractice.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/login")
    public String login() {
        return "member/login";
    }

    @ResponseBody
    @PostMapping(value = "/login")
    public ResponseEntity<LoginResponseDto> signin(
            @RequestBody LoginRequestDto request,
            HttpServletResponse response) throws Exception {
        return new ResponseEntity<>(memberService.logIn(request, response), HttpStatus.OK);
    }

    @ResponseBody
    @PostMapping(value = "/user/refresh")
    public ResponseEntity<LoginResponseDto> refresh(@RequestHeader (name= JwtProvider.REFRESH_TOKEN) String refreshToken) {
        return new ResponseEntity<>(memberService.refreshUserInfo(refreshToken), HttpStatus.OK);
    }

    @ResponseBody
    @PostMapping(value = "/user/signout")
    public ResponseEntity<String> signout(@RequestHeader (name="Authorization") String token) throws Exception {
        memberService.logOut(token);
        return new ResponseEntity<>("success", HttpStatus.OK);
    }

    @GetMapping("/register")
    public String register() {
        return "member/register";
    }

    @PostMapping("/emails/send-authcode")
    public ResponseEntity<Void> sendMessage(@RequestBody Map<String, String> param) throws Exception {
        memberService.sendCodeToEmail(param.get("email"));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/emails/verifications")
    public ResponseEntity<Boolean> verificationEmail(@RequestBody Map<String, String> param) {
        boolean response = memberService.verifiedCode(param.get("email"), param.get("code"));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ResponseBody
    @PostMapping(value = "/register")
    public ResponseEntity<Boolean> signup(@RequestBody SignUpRequestDto request) throws Exception {
        return new ResponseEntity<>(memberService.register(request), HttpStatus.OK);
    }

    @GetMapping("/find-pwd")
    public String findPassword() {
        return "member/find_pwd";
    }

    @ResponseBody
    @PostMapping("/find-email")
    private ResponseEntity<String> findEmail(@RequestBody Map<String, String> request) throws Exception {
        String email = memberService.findRegisteredEmail(request.get("phone"), request.get("code"));

        return new ResponseEntity<>(email, HttpStatus.OK);
    }

    @ResponseBody
    @PostMapping("/find-pwd")
    private ResponseEntity<String> findPassword(@RequestBody Map<String, String> request) throws Exception {
        String status = memberService.forwardTempPassword(request.get("email"), request.get("phone"), request.get("code"));

        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    @GetMapping("/modify-pwd")
    public String modifyPassword() {
        return "member/modify_pwd";
    }

    @ResponseBody
    @PostMapping("/user/modify-pwd")
    private ResponseEntity<Boolean> modifyPassword(
            @RequestHeader (name="Authorization") String token,
            @RequestBody ModifyUserInfoDto request) throws Exception {
        memberService.modifyUserPassword(token, request);

        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @GetMapping("/modify-info")
    public String modifyInfo(Model model) throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            LoginResponseDto loginResponseDto = memberService.getMember(email);
            model.addAttribute("name", loginResponseDto.getName());
            model.addAttribute("phone", loginResponseDto.getPhone());
        }
        return "member/modify_info";
    }

    @ResponseBody
    @PostMapping("/user/modify-info")
    private ResponseEntity<LoginResponseDto> modifyInfo(
            @RequestHeader (name="Authorization") String token,
            @RequestBody ModifyUserInfoDto request) throws Exception {

        return new ResponseEntity<>(memberService.modifyUserInfo(token, request), HttpStatus.OK);
    }

    @GetMapping("/user/home")
    public String home(Model model, @RequestParam String name) {
        model.addAttribute("name", name);
        return "home";
    }

    @ResponseBody
    @GetMapping("/user/get")
    public ResponseEntity<LoginResponseDto> getUser(@RequestParam String account) throws Exception {
        return new ResponseEntity<>( memberService.getMember(account), HttpStatus.OK);
    }

    @ResponseBody
    @GetMapping("/admin/get")
    public ResponseEntity<LoginResponseDto> getUserForAdmin(@RequestParam String account) throws Exception {
        return new ResponseEntity<>( memberService.getMember(account), HttpStatus.OK);
    }
}