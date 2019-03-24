package com.forever.spring.controller;

import com.forever.spring.annotation.ZYHAutowired;
import com.forever.spring.annotation.ZYHController;
import com.forever.spring.service.SystemService;

@ZYHController
public class SystemController {

    @ZYHAutowired
    private SystemService systemService;
}
