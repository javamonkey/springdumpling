package org.bee.spring.dumpling.clustersync.zookeeper;

import java.util.List;

import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.ACL;

/**
 * 配置
 * 
 * @author KevinLiao
 */
public class Config {
	/** ACL列表 */
	protected List<ACL> acls = Ids.OPEN_ACL_UNSAFE;
	/** 进行节点ACL限制的Auth串 */
	protected String auth = "admin:admin";
	/** 进行节点ACL限制的Scheme */
	private final String scheme = "digest";
	/** Server 地址 */
	private String server;
	/** 连接Server超时时间 */
	private final int timeout = 10000;
	/** 是否使用ACL，默认为0，表示不使用不使用则Scheme和Auth无意义 */
	private int useACL;
	
	public Config() {
	}
	
	public Config(String server) {
		this.server = server;
	}

	public List<ACL> getACLs() {
		return acls;
	}

	public String getAuth() {
		return auth;
	}

	public String getScheme() {
		return scheme;
	}

	public String getServer() {
		return server;
	}

	public int getTimeout() {
		return timeout;
	}

	public int getUseACL() {
		return useACL;
	}

	public void setACLs(List<ACL> acls) {
		this.acls = acls;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public void setUseACL(int useACL) {
		this.useACL = useACL;
	}
}
