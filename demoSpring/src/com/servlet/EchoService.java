package com.servlet;

import java.net.HttpRetryException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mvc.aonotation.MyAutowired;
import com.mvc.aonotation.MyService;

@MyService
public class EchoService {

	@MyAutowired
	public EchoDao dao ;
	
	public void doSomething(String name){
		dao.doPrint(name);
	}
	
}
