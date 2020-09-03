package com.steve.db.test;

import javax.xml.bind.annotation.XmlRootElement;

/**
*
* @author longtai
*/
@XmlRootElement
public class Person {
	public String name;
	private String email;
	private Address address;
	

	
	/**
	* @return the email
	*/
	public String getEmail() {
	return email;
	}
	
	/**
	* @param email the email to set
	*/
	public void setEmail(String email) {
	this.email = email;
	}

	/**
	* @return the address
	*/
	public Address getAddress() {
	return address;
	}

	/**
	* @param address the address to set
	*/
	public void setAddress(Address address) {
	this.address = address;
	}

}

