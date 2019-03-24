package com.forever.spring.service;

import com.forever.spring.annotation.ZYHService;

@ZYHService
public class MemberServiceImpl implements MemberService {

    public String login(String name){
        return "登陆成功，欢迎您："+name;
    }
}
