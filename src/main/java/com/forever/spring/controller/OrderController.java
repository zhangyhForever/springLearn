package com.forever.spring.controller;

import com.forever.spring.annotation.ZYHAutowired;
import com.forever.spring.annotation.ZYHController;
import com.forever.spring.service.OrderService;

@ZYHController("order")
public class OrderController {

    @ZYHAutowired
    private OrderService orderService;
}
