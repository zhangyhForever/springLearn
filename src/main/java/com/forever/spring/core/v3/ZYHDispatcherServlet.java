package com.forever.spring.core.v3;

import com.forever.spring.core.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZYHDispatcherServlet extends HttpServlet {

//    private Properties properties = new Properties();

    //需要扫描的包名
    private String packageName;

    //被扫描包中的类文件
    private List<String> classNames = new ArrayList<String>();

    //ioc容器
    private Map<String, Object> ioc = new HashMap<String, Object>();

    //url和方法映射
//    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

    //将url和方法的映射，以及请求参数和方法参数封装成一个类
    private List<HandlerMapping> handlerMapping = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exection,Detail : " + Arrays.toString(e.getStackTrace()));
        }

    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse res) throws InvocationTargetException, IllegalAccessException {

        HandlerMapping handler = getHandler(req);

        if(handler == null){
            try {
                res.getWriter().write("404 Not Found!!!");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Class<?>[] paramTypes = handler.getParamTypes();

        Map<String, Integer> paramIndexMapping = handler.getParamIndexMapping();

        Map<String, String[]> parameterMap = req.getParameterMap();

        Object[] invokeParams = new Object[paramTypes.length];


        for(Map.Entry<String, String[]> entry: parameterMap.entrySet()){
            if(paramIndexMapping.containsKey(entry.getKey())){
                Integer index = paramIndexMapping.get(entry.getKey());
                invokeParams[index] = convert(paramTypes[index],
                        Arrays.toString(entry.getValue()).replaceAll("\\[|\\]",""));
            }
        }

        if(handler.paramIndexMapping.containsKey(HttpServletRequest.class.getSimpleName())){
            Integer reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getSimpleName());
            invokeParams[reqIndex] = req;
        }

        if(handler.paramIndexMapping.containsKey(HttpServletResponse.class.getSimpleName())){
            Integer resIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getSimpleName());
            invokeParams[resIndex] = res;
        }


        Object invoke = handler.getMethod().invoke(handler.getController(), invokeParams);
        if(invoke instanceof String){
            res.setContentType("text/html; charset=utf-8");
            try {
                res.getWriter().write(invoke.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 从handlerMapping中获取该url下的详细参数
     * @param req
     * @return
     */
    private HandlerMapping getHandler(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String requestUrl = uri.replace(contextPath, "").replace("/+","/");

        for(HandlerMapping handler: handlerMapping){
            Matcher matcher = handler.getUrl().matcher(requestUrl);
            if(matcher.matches()){
                return handler;
            }
        }
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //1、加载配置文件
        loadConfig(config);

        //2、扫描所有的类
        doScanner(packageName);

        //3、将扫描到的类实例化并放入到ioc容器中
        doInstance();

        //4、完成依赖注入
        doAutowired();

        //5、初始化HandlerMapping(将url和和方法一一映射)
        doHandlerMapping();

        System.out.println("spring容器初始化完成");
    }

    /**
     * 将url和方法做一一映射
     */
    private void doHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(ZYHController.class)) {
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(ZYHRequestMapping.class)) {
                ZYHRequestMapping requestMapping = clazz.getAnnotation(ZYHRequestMapping.class);
                baseUrl = requestMapping.value().trim();
                if ("".equals(baseUrl)) {
                    throw new RuntimeException(clazz.getSimpleName() + " ZYHRequestMapping not found value!!!");
                }
            }
            Method[] methods = entry.getValue().getClass().getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(ZYHRequestMapping.class)) {
                    continue;
                }
                ZYHRequestMapping methodRequestMapping = method.getAnnotation(ZYHRequestMapping.class);
                String methodUrl = methodRequestMapping.value();
                if ("".equals(methodUrl)) {
                    methodUrl = toLowerFirstCase(method.getName());
                }
                String regex = ("/" + baseUrl + "/" + methodUrl).replaceAll("/+", "/");
                Pattern url = Pattern.compile(regex);
                handlerMapping.add(new HandlerMapping(url, method, entry.getValue()));
                System.out.println("mapped===>"+"url:"+url+">>>>>>>>method:"+method);
            }
        }
    }

    /**
     * 依赖注入
     */
    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field f : fields) {
                if (!f.isAnnotationPresent(ZYHAutowired.class)) {
                    continue;
                }

                //获得注解中自定义的值
                ZYHAutowired autowired = f.getAnnotation(ZYHAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    beanName = toLowerFirstCase(f.getType().getSimpleName());
                }
                f.setAccessible(true);

                try {

                    //完成注入
                    f.set(entry.getValue(), ioc.get(beanName));
                    System.out.println("依赖注入：将"+ioc.get(beanName)+">>>>>>"+entry.getValue());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * 实例化对象并注册到ioc容器
     */
    private void doInstance() {
        if (classNames == null) {
            return;
        }
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(ZYHController.class)) {
                    Object instance = clazz.newInstance();
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, instance);
                    System.out.println("controler:"+beanName+">>>实例对象："+instance);
                } else if (clazz.isAnnotationPresent(ZYHService.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());

                    //自定义命名
                    ZYHService service = clazz.getAnnotation(ZYHService.class);
                    if (!"".equals(service.value())) {
                        beanName = service.value();
                    }

                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    System.out.println("service:"+beanName+">>>实例对象："+instance);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 扫描包下的类
     *
     * @param scanPackage
     */
    private void doScanner(String scanPackage) {
        String packagePath = scanPackage.replaceAll("\\.", "/");
        URL url = this.getClass().getClassLoader().getResource("/" + packagePath);
        File packageFile = new File(url.getFile());
        if (!packageFile.exists()) {
            throw new RuntimeException(packagePath + "===>Not Found!!!");
        }
        File[] files = packageFile.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                doScanner(scanPackage + "." + f.getName());
            }
            if (!f.getName().endsWith(".class")) {
                continue;
            }
            classNames.add(scanPackage + "." + f.getName().replace(".class", ""));
        }
    }


    /**
     * spring配置文件读取
     *
     * @param config
     */
    private void loadConfig(ServletConfig config) {
        //配置文件文件名
        String configName = config.getInitParameter("contextConfigLocation");
        System.out.println("加载配置文件:" + configName);

        //配置文件相对路径
        String configPath = ZYHDispatcherServlet.class.getResource("/").getPath();

        //读取配置文件
        FileReader fr = null;
        try {
            fr = new FileReader(new File(configPath, configName));
            Properties properties = new Properties();
            properties.load(fr);
            this.packageName = properties.getProperty("scanPackage");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 将类名首字母小写
     *
     * @param className
     * @return
     */
    private String toLowerFirstCase(String className) {
        char[] chars = className.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * 将字符串转换成成相对应的类型
     * @param clazz
     * @param value
     * @return
     */
    private Object convert(Class<?> clazz, String value){
        if(Integer.class == clazz){
            return Integer.valueOf(value);
        }
        if(String.class == clazz){
            return value;
        }
        return value;
    }

    private class HandlerMapping {

//        private String url;

        //做通配符的话url应该为正则类型
        private Pattern url;
        private Method method;
        private Object controller;

        //封装
        private Class<?>[] paramTypes;
        //方法中加了注解的参数，注解里的值作为key，参数index作为value
        private Map<String, Integer> paramIndexMapping;

        public HandlerMapping(Pattern url, Method method, Object controller) {
            this.url = url;
            this.method = method;
            this.controller = controller;

            this.paramTypes = method.getParameterTypes();

            //方法直接可以拿到，因此直接进行初始化
            this.paramIndexMapping = new HashMap<>();
            initParamIndexMapping(method);
        }

        private void initParamIndexMapping(Method method) {

            //方法上参数注解的长度和方法参数的个数相等
            Annotation[][] ps = method.getParameterAnnotations();
            for(int i=0; i<ps.length; i++){
                for(Annotation annotation: ps[i]){
                    if(annotation instanceof ZYHRequestParam){
                        String paraName = ((ZYHRequestParam) annotation).value();
                        paramIndexMapping.put(paraName, i);
                    }
                }
            }

            //HttpServletRequest和HttpServletResponse特殊处理
            Class<?>[] parameterTypes = method.getParameterTypes();
            for(int i=0; i<parameterTypes.length; i++){
                if(parameterTypes[i] == HttpServletRequest.class){
                    paramIndexMapping.put(parameterTypes[i].getSimpleName(), i);
                }else if(parameterTypes[i] == HttpServletResponse.class){
                    paramIndexMapping.put(parameterTypes[i].getSimpleName(), i);
                }
            }
        }


        public Pattern getUrl() {
            return url;
        }

        public Method getMethod() {
            return method;
        }

        public Object getController() {
            return controller;
        }

        public Class<?>[] getParamTypes() {
            return paramTypes;
        }

        public Map<String, Integer> getParamIndexMapping() {
            return paramIndexMapping;
        }
    }
}