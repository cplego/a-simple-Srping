package com.servlet;

import com.mvc.aonotation.MyService;

@MyService
public class EchoDao {
	
	public void doPrint(String name){
		System.out.println("recive : "+name );
	}
	
}
