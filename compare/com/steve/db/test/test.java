package com.steve.db.test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;


public class test {
	public int a=3;
	
	  public static void testtest(int i) throws InterruptedException
	  {
		  System.out.println("a:"+i);
		  Thread.sleep(100);
	  }
	  public static void main(String s[]) throws Exception {
		  int i=0;
		  test t = new test();
		  while(true)
		  {
			  i++;
			  t.testtest(i);
		  }

	  }
/*
 * 
StringReader reader = new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><person><address><address>%u53F0%u7063%u65B0%u7AF9</address><zip>35002</zip></address><email>longtai.tw@gmail.com</email><name>longtai</name></person>");
JAXBContext context = JAXBContext.newInstance(Person.class);
Unmarshaller um = context.createUnmarshaller();
Person p = (Person)um.unmarshal(reader);

System.out.println(p.getName());
System.out.println(p.getAddress().getAddress());
System.out.println(p.getAddress().getZipcode());	  
 */
	  
	  
  }

