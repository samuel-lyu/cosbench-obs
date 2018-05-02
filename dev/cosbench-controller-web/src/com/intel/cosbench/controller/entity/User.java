package com.intel.cosbench.controller.entity;

public class User {
	private String id;
	private String userName;
	private String password;
	private String description;
	private String userGroup;
	
	public User() {
		super();
	}
	
	public User(String id, String userName, String password, String description, String userGroup) {
		super();
		this.id = id;
		this.userName = userName;
		this.password = password;
		this.description = description;
		this.userGroup = userGroup;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getUserGroup() {
		return userGroup;
	}
	public void setUserGroup(String userGroup) {
		this.userGroup = userGroup;
	}
	
}
