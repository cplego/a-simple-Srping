package com.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mvc.aonotation.MyAutowired;
import com.mvc.aonotation.MyController;
import com.mvc.aonotation.MyRequestMapping;

@MyController
public class EchoController {
	@MyAutowired
	public EchoService service ;
	
	@MyRequestMapping("/test")
	public void doTest(HttpServletRequest req, HttpServletResponse resp,String name){
		System.out.println("in doTest");
		service.doSomething(name);
	}
	
	@MyRequestMapping("/print")
	public void doPrint(HttpServletRequest req, HttpServletResponse resp,String name){
		System.out.println("in doPrint");
		service.doSomething(name);
		PrintWriter pw = null;
		try {
			pw = resp.getWriter();
			pw.write("Echo String -> "+name);
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(null != pw)
			pw.close();
		}
	}
	
	
}
