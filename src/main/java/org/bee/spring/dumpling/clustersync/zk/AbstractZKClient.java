package org.bee.spring.dumpling.clustersync.zk;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

public abstract class AbstractZKClient implements Watcher
{
	protected Log logger = LogFactory.getLog(getClass());
	/**
	 * 连接的实例
	 */
	protected ZooKeeper zk;

	protected ZKConfig conf;

	private CountDownLatch connectedSignal;

	/**
	 * 返回ZK是否在正常连接，如果没有请应用自己判断如何处理
	 */
	public boolean isAlive()
	{

		return zk == null ? false : zk.getState() == null ? false : zk.getState().isAlive();
	}

	protected void connect() throws IOException, NoSuchAlgorithmException, InterruptedException
	{
		// 如果之前连着，要先断掉
		if (null != zk && zk.getState().isAlive())
		{
			zk.close();
		}
		zk = new ZooKeeper(conf.getServer(), conf.getTimeout(), this);
		connectedSignal = new CountDownLatch(1);
		connectedSignal.await();
		if (null != conf.getScheme())
		{
			zk.addAuthInfo(conf.getScheme(), conf.getAuth().getBytes());
		}
		if (0 != conf.getUseACL() && conf.getAcls() == Ids.OPEN_ACL_UNSAFE)
		{
			List<ACL> acls = conf.getAcls();
			Id authId = new Id("digest", DigestAuthenticationProvider.generateDigest(conf.getAuth()));
			Id anyId = new Id("world", "anyone");
			acls.clear();
			acls.add(new ACL(ZooDefs.Perms.ALL ^ ZooDefs.Perms.DELETE, anyId));
			acls.add(new ACL(ZooDefs.Perms.DELETE, authId));
			conf.setAcls(acls);
		}
		connectedSignal = null;
	}

	public void init() throws Exception
	{
		connect();
	}

	@PreDestroy
	public void destroy()
	{
		if (zk != null)
		{
			try
			{
				zk.close();
			}
			catch (InterruptedException e)
			{
				logger.fatal(e.getMessage(), e);
			}
		}
	}

	@Override
	public void process(WatchedEvent event)
	{
		if (event.getState() == KeeperState.SyncConnected)
		{
			if (null != connectedSignal)
			{
				connectedSignal.countDown();
			}
		}
		else if (event.getState() == KeeperState.Expired)
		{
			try
			{
				init();
			}
			catch (Exception e)
			{
				logger.error(e);
			}
		}
	}

	public ZooKeeper getZk()
	{
		return zk;
	}

	public ZKConfig getConf()
	{
		return conf;
	}

	public void setConfig(ZKConfig conf)
	{
		this.conf = conf;
	}

}
