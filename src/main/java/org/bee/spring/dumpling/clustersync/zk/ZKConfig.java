package org.bee.spring.dumpling.clustersync.zk;

import java.util.List;

import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.ACL;

/**
 * 配置
 * 
 * @author KevinLiao
 */
public class ZKConfig {
	/**
	 * Server 地址
	 */
	private String server;
	/**
	 * 连接Server超时时间
	 */
	private final int timeout = 10000;
	/**
	 * 进行节点ACL限制的Scheme
	 */
	private final String scheme = "digest";
	/**
	 * 进行节点ACL限制的Auth串
	 */
	protected String auth = "admin:admin";
	/**
	 * 是否使用ACL，默认为0，表示不使用不使用则Scheme和Auth无意义
	 */
	private int useACL;
	/**
	 * ACL列表
	 */
	protected List<ACL> acls = Ids.OPEN_ACL_UNSAFE;

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}

	public int getUseACL() {
		return useACL;
	}

	public void setUseACL(int useACL) {
		this.useACL = useACL;
	}

	public List<ACL> getAcls() {
		return acls;
	}

	public void setAcls(List<ACL> acls) {
		this.acls = acls;
	}

	public int getTimeout() {
		return timeout;
	}

	public String getScheme() {
		return scheme;
	}

}
