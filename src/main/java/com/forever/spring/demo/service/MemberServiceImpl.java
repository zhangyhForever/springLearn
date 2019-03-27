package com.forever.spring.demo.service;

import com.forever.spring.core.annotation.ZYHService;

@ZYHService("memberService")
public class MemberServiceImpl implements MemberService {

    private String name;

    public String getName() {
        return name;
    }


    public String login(String name){
        return "登陆成功，欢迎您："+name;
    }
}
