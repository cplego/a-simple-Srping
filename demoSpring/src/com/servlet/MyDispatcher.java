package com.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mvc.aonotation.MyAutowired;
import com.mvc.aonotation.MyController;
import com.mvc.aonotation.MyRequestMapping;
import com.mvc.aonotation.MyService;


public class MyDispatcher extends HttpServlet{

	private Properties contextConfig = new Properties();
	private List<String> classNames = new ArrayList<String>();
	private Map <String,Object> ioc = new HashMap<String,Object>();
	private Map <String,Object> handlerMapping = new HashMap<String,Object>();

	@Override
	public void init(ServletConfig config) throws ServletException {
		
		//1加载配置文件
		//doLoadConfig(config.getInitParameter("contextConfigLocation"));
		doLoadConfig("contextConfig.properties");
		//2扫描包类
		doScanner(contextConfig.getProperty("scanPackage"));
		//3实例化相关类
		doInstance();
		//4完成注入
		doAutowied();
		//5 初始化handlerMapping
		initHandlerMapping();
	}
	
	private void doLoadConfig(String contextConfigLocation) {
		System.out.println(contextConfigLocation);
		
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
		try {
			contextConfig.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				if(null != is)
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	}

	private void initHandlerMapping() {
		
		if(ioc.isEmpty()) return;
		
		for(Map.Entry<String, Object> entry : ioc.entrySet()){
			
			Class clazz = entry.getValue().getClass();
			
			if(! clazz.isAnnotationPresent(MyController.class)){
				continue;
			}
			
			for(Method method: clazz.getMethods()){
				if(!method.isAnnotationPresent(MyRequestMapping.class)) continue;
				
				MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
				
				String url =requestMapping.value();
				
				handlerMapping.put(url,method);
				
			}
			
			
		}
		
		
	}

	private void doAutowied() {
		if(ioc.isEmpty()) return;
		for(Map.Entry<String, Object> entry : ioc.entrySet()){
			Field [] fields = entry .getValue().getClass().getDeclaredFields();
			for(Field field : fields){
				MyAutowired autowired = (MyAutowired)field.getAnnotation(MyAutowired.class);
				String beanName = autowired.value().trim();
				System.out.println("bN-->" +beanName);
				if("".equals(beanName)){
					beanName = toLowerFirstCase(field.getType().getSimpleName());
					System.out.println("beanName in autowired ->" +beanName);
				}
				field.setAccessible(true);
				try {
					field.set(entry.getValue(), ioc.get(beanName));
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		}
		
	}

	private void doInstance() {
		if(classNames.isEmpty()) return;
		
		try {
			for(String className : classNames){
				Class clazz = Class.forName(className);
				System.out.println("cn ->"+className);
				System.out.println("ture ??? -> "+clazz.isAnnotationPresent(MyController.class));
				System.out.println("ture ??? -> "+clazz.isAnnotationPresent(MyService.class));
				if(!clazz.isAnnotationPresent(MyController.class) &&
						!clazz.isAnnotationPresent(MyService.class))continue;
				System.out.println("cn after ->"+className);
				
				
				if(clazz.isAnnotationPresent(MyController.class)){
					//beanName 默认首字母小写
					String beanName = toLowerFirstCase(clazz.getSimpleName());
					ioc.put(beanName, clazz.newInstance());
				}else if(clazz.isAnnotationPresent(MyService.class)){
					//beanName 默认首字母小写
					String beanName = toLowerFirstCase(clazz.getSimpleName());
					
					//自定义命名
					MyService service =(MyService)clazz.getAnnotation(MyService.class);
					System.out.println("service " + service.value());
					if(!"".equals(service.value()))
						beanName = service.value();

					ioc.put(beanName, clazz.newInstance());
					
					//3 接口不能直接实例化，实例化其子类
					
					
				}else {
					continue;
				}
				
				
			}
			
		} catch (Exception e) {
				e.printStackTrace();
			}
		
		
	}
	
	//首字母小写
	private String toLowerFirstCase(String simpleName) {
		char[] chars = simpleName.toCharArray();
		chars[0]+=32;
		return String.valueOf(chars);
	}

	private void doScanner(String scanPackage) {
		URL url =this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.", "/"));
		System.out.println("url -> " + url.getFile());
		File classPath = new File(url.getFile());
		
		for(File file : classPath.listFiles()){
			if(file.isDirectory()){
				doScanner(scanPackage+"."+file.getName());
			}
			
			if(!file.getName().endsWith(".class")) continue;
			String className = scanPackage+"."+file.getName().replace(".class", "");//取得类全名
			classNames.add(className);
			System.out.println("className -> "+className);
		}
		
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		System.out.println("in doGet");
		//6调用
		try {
			doDispatch(req,resp);
		} catch (Exception e) {
			e.printStackTrace();
			resp.getWriter().write("500 ERROR");
			return;
		}
		
		
	}
	
	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		
		if(handlerMapping.isEmpty()) return;
		System.out.println("in dispatch");
		String url = req.getRequestURI();
		String contextPath = req.getContextPath();
		System.out.println("url before ->" +url);
		url = url.replaceAll(contextPath, "").replaceAll("/+", "/").replace(".do", "");
		System.out.println("contextPath ->" +contextPath);
		System.out.println("url ->" +url);
		if(! this.handlerMapping.containsKey(url)){
			resp.getWriter().write("404 Not Found!");
			return;
		}
		Method method = (Method) this.handlerMapping.get(url);
		System.out.println("methodname -> "+method.getName());
		
		String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
		System.out.println("params ->"+Arrays.toString(req.getParameterMap().keySet().toArray()));
		method.invoke(ioc.get(beanName),new Object[]{req,resp,req.getParameterMap().get(req.getParameterMap().keySet().toArray()[0])[0]});
		
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req,resp);
		
		
	}
	
	
	
}
