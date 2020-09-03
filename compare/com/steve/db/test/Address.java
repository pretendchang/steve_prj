package com.steve.db.test;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
*
* @author longtai
*/
@XmlAccessorType(XmlAccessType.FIELD)
public class Address {
private String address;
@XmlElement(name = "zip")
private String zipcode;

/**
* @return the address
*/
public String getAddress() {
return address;
}

/**
* @param address the address to set
*/
public void setAddress(String address) {
this.address = address;
}

/**
* @return the zipcode
*/
public String getZipcode() {
return zipcode;
}

/**
* @param zipcode the zipcode to set
*/
public void setZipcode(String zipcode) {
this.zipcode = zipcode;
}
}
