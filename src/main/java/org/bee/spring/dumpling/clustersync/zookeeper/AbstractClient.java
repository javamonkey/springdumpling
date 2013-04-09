package org.bee.spring.dumpling.clustersync.zookeeper;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.annotation.PreDestroy;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractClient implements Watcher {
	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	/** 连接的实例 */
	protected ZooKeeper keeper;

	protected Config config;

	private CountDownLatch connectedSignal;

	/** 返回ZK是否在正常连接，如果没有请应用自己判断如何处理 */
	public boolean isAlive() {
		return keeper == null || keeper.getState() == null ? false : keeper.getState().isAlive();
	}

	public void connect() throws IOException, NoSuchAlgorithmException, InterruptedException {
		// 如果之前连着，要先断掉
		if (null != keeper && keeper.getState().isAlive()) {
			keeper.close();
		}
		keeper = new ZooKeeper(config.getServer(), config.getTimeout(), this);
		connectedSignal = new CountDownLatch(1);
		connectedSignal.await();
		if (null != config.getScheme()) {
			keeper.addAuthInfo(config.getScheme(), config.getAuth().getBytes());
		}
		if (0 != config.getUseACL() && config.getACLs() == Ids.OPEN_ACL_UNSAFE) {
			List<ACL> acls = config.getACLs();
			Id authId = new Id("digest", DigestAuthenticationProvider.generateDigest(config.getAuth()));
			Id anyId = new Id("world", "anyone");
			acls.clear();
			acls.add(new ACL(ZooDefs.Perms.ALL ^ ZooDefs.Perms.DELETE, anyId));
			acls.add(new ACL(ZooDefs.Perms.DELETE, authId));
			config.setACLs(acls);
		}
		connectedSignal = null;
	}

	public void init() throws Exception {
		connect();
	}

	@PreDestroy
	public void destroy() {
		if (keeper != null) {
			try {
				keeper.close();
			} catch (InterruptedException e) {
				logger.info(e.getMessage(), e);
			}
		}
	}

	@Override
	public void process(WatchedEvent event) {
		switch (event.getState()) {
		case SyncConnected:
			if (null != connectedSignal) {
				connectedSignal.countDown();
			}
			break;
		case Expired:
			try {
				init();
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
	}

	public ZooKeeper getKeeper() {
		return keeper;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}
}
