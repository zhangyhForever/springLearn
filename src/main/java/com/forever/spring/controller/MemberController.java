package com.forever.spring.controller;

import com.forever.spring.annotation.ZYHAutowired;
import com.forever.spring.annotation.ZYHController;
import com.forever.spring.annotation.ZYHRequestMapping;
import com.forever.spring.annotation.ZYHRequestParam;
import com.forever.spring.service.MemberService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@ZYHController
@ZYHRequestMapping("/member")
public class MemberController {

    @ZYHAutowired
    private MemberService memberService;

    @ZYHRequestMapping("/login")
    public void login(HttpServletResponse res, @ZYHRequestParam String name){
        res.setContentType("text/html, charset=utf-8");
        try {
            String message = memberService.login(name);
            res.getWriter().write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
