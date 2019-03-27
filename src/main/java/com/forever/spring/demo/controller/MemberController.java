package com.forever.spring.demo.controller;

import com.forever.spring.core.annotation.*;
import com.forever.spring.demo.service.MemberService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@ZYHController
@ZYHRequestMapping("/member")
public class MemberController {

    @ZYHAutowired
    private MemberService memberService;

    @ZYHRequestMapping("/login.*")
    public void login(HttpServletResponse res, @ZYHRequestParam("name") String username){
        System.out.println(username);
        res.setContentType("text/html; charset=utf-8");
        try {
            System.out.println("name===="+username);
            String message = memberService.login(username);
            res.getWriter().write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ZYHRequestMapping("/add")
    public void add(HttpServletResponse res, @ZYHRequestParam("a") Integer a, @ZYHRequestParam("b") Integer b){
        res.setContentType("text/html; chartset=utf-8");
        try {
            res.getWriter().write(a+"+"+b+"="+(a+b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ZYHRequestMapping("/show")
    public String show(@ZYHRequestParam("input") Integer input){
        return "输入==="+input;
    }
}
