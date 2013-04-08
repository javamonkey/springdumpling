package org.bee.spring.dumpling.jms.activemq;

import java.io.Serializable;

public class JMSConfig implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String URL;
	private String username;
	private String password;

	public String getURL() {
		return URL;
	}

	public void setURL(String url) {
		this.URL = url;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}

	public String getUsername() {
		return username;
	}
	
	public void setUsername(String userName) {
		this.username = userName;
	}
}
