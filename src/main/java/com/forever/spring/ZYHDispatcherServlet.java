package com.forever.spring;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ZYHDispatcherServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch(req, resp);
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
    }

    @Override
    public void init() throws ServletException {

        //1、加载配置文件
        loadConfig();

        //2、扫描所有的类
        doScanner();

        //3、将扫描到的类实例化并放入到ioc容器中
        doInstance();

        //4、完成依赖注入
        doAutowired();

        //5、初始化HandlerMapping(将url和和方法一一映射)
        doHandlerMapping();

        System.out.println("spring容器初始化完成");
    }

    private void doHandlerMapping() {
    }

    private void doAutowired() {

    }

    private void doInstance() {
    }

    private void doScanner() {
    }

    private void loadConfig() {
    }
}
