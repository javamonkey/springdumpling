package org.bee.spring.dumpling.clustersync.zk;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;


import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

/**
 * @author KevinLiao 2011-12-29
 */
public class ZKCandidate extends AbstractZKClient implements AsyncCallback.VoidCallback, ZKClient
{

	/**
	 * 创建节点的目录（结尾不带"/"）
	 */
	private String rootDirectory;
	/**
	 * 创建临时节点的前缀
	 */
	private String znodePrefix;
	/**
	 * 选举执行节点的方式<br>
	 * 0 初始化时选举一次（默认）<br>
	 * 1 每次执行任务皆重新选举
	 */
	@SuppressWarnings("unused")
	private int electionType = 0;
	/**
	 * 选取master的timeout
	 */
	private int justifyMasterTimeout = 10000;

	/**
	 * 创建的临时节点的全路径
	 */
	private String path;
	/**
	 * 是否是master
	 */
	private boolean isMaster;

	private int acessCount = 0;

	/**
	 * 通过这个方法取是否是master<br>
	 * 如果设置选取master方法是0（默认），且设置监听的path已经存在，则直接返回isMaster值<br>
	 * 否则需要wait指定时间（默认3s），待确认后再返回，如果没有取到自己设定的path则认为自己不是master，否则直接返回isMaster值<br>
	 * 
	 * @return
	 */
	@Override
	public boolean isMaster()
	{

		// if (electionType == 0 && null != path)
		// {
		// return isMaster;
		// }
		synchronized (this)
		{
			try
			{
				// 等于重新初始化一次
				// init();
				doLeaderElection();
				// 等着吧，领导们要开会
				this.wait(justifyMasterTimeout);
			}
			catch (InterruptedException e)
			{
				logger.error(e.getMessage(), e);
				return false;
			}
			return null == path ? false : isMaster;
		}
	}

	/**
	 * 返回zk是否在正常连接，如果没有请应用自己判断如何处理
	 * 
	 * @return
	 */
	@Override
	public boolean isAlive()
	{

		return zk == null ? false : zk.getState() == null ? false : zk.getState().isAlive();
	}

	@Override
	@PostConstruct
	public void init() throws Exception
	{

		// 先重置，确保isMaster方法取到的是新的决议
		path = null;
		isMaster = false;
		try
		{
			connect();
			// 如果没有rootDirectoty节点，则创建
			int lastidx = 1;
			int nextidx = 0;
			if (rootDirectory.endsWith("/"))
			{
				rootDirectory = rootDirectory.substring(0, rootDirectory.length() - 1);
			}
			do
			{
				nextidx = rootDirectory.indexOf("/", lastidx + 1);
				if (nextidx >= 0)
				{
					lastidx = nextidx;
					String path = rootDirectory.substring(0, nextidx);
					if (null == zk.exists(path, false))
					{
						zk.create(path, new byte[0], conf.getAcls(), CreateMode.PERSISTENT);
					}
				}
				else
				{
					if (null == zk.exists(rootDirectory, false))
					{
						zk.create(rootDirectory, new byte[0], conf.getAcls(), CreateMode.PERSISTENT);
					}
				}
			}
			while (nextidx >= 0);
			path = zk.create(rootDirectory + "/" + znodePrefix, getZnodeContent().getBytes(), conf.getAcls(),
					CreateMode.EPHEMERAL_SEQUENTIAL);
			// System.out.println("create path " + path);
			if (logger.isInfoEnabled())
			{
				logger.info(" sync success");
			}
			zk.sync(rootDirectory, this, null);
		}
		catch (IOException e)
		{
			logger.fatal(e.getMessage(), e);
			throw e;
		}
		catch (KeeperException e)
		{
			logger.fatal(e.getMessage(), e);
			throw e;
		}
		catch (InterruptedException e)
		{
			logger.fatal(e.getMessage(), e);
			throw e;
		}
		catch (NoSuchAlgorithmException e)
		{
			logger.fatal(e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * 这个方法的最大作用是notify在isMaster()方法中等待的线程
	 * 
	 * @param isMaster
	 */
	private void setMaster(boolean isMaster)
	{

		this.isMaster = isMaster;
		synchronized (this)
		{
			this.notifyAll();
		}
	}

	private String getZnodeContent()
	{
		try
		{
			return InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException e)
		{
			return "0";
		}
	}

	/**
	 * 处理sync方法的异步返回
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void processResult(int rc, String path, Object ctx)
	{

		switch (rc)
		{
			case Code.Ok:
				if (logger.isDebugEnabled())
				{
					logger.debug(" sync success");
				}
				doLeaderElection();
				break;
			default:
				break;
		}

	}

	@Override
	public void process(WatchedEvent event)
	{
		super.process(event);
		String eventPath = event.getPath();
		if (Watcher.Event.EventType.NodeDeleted == event.getType() || !rootDirectory.equals(eventPath))
		{
			if (logger.isDebugEnabled())
			{
				logger.debug(" find " + event.getPath() + " was deleted, it'm my turn!");
			}
			setMaster(true);
			doSth();
		}

	}

	/**
	 * 选取master
	 */
	private void doLeaderElection()
	{

		logger.info("zk = " + zk.toString());
		try
		{
			if (path!=null&&path.length()!=0)
			{
				init();
				return;
			}
			List<String> znodeList = zk.getChildren(rootDirectory, false);
			Collections.sort(znodeList);
			int size = znodeList.size();
			boolean isPathExist = false;
			for (int i = 0; i < size; i++)
			{
				String s = znodeList.get(i);
				if (path.equals(rootDirectory + "/" + s))
				{
					isPathExist = true;
					if (i == 0)
					{
						setMaster(true);
						doSth();
					}
					else
					{
						// 从getChildren方法获取的列表中选择前一个自己的节点，设置watcher，这里要考虑获取时存在，但设置watcher时已经不存在的znode的情形
						int j = i;
						Stat stat = null;
						do
						{
							// 这里先不设置watcher
							stat = zk.exists(rootDirectory + "/" + znodeList.get(--j), false);
						}
						while (j >= 0 && null == stat);
						// 如果没有前一个节点，自己就是master（矬子拔将军？）
						if (null == stat)
						{
							setMaster(true);
							doSth();
						}
						else
						{
							// System.out.println("set watcher on " +
							// znodeList.get(j));
							stat = zk.exists(rootDirectory + "/" + znodeList.get(j), this);
							setMaster(false);
							doFollowerThings();
						}
					}
					// if this is the first node, there 's no need to watch
					// anything
					break;
				}
			}
			if (!isPathExist)
			{
				init();
				return;
			}
		}
		catch (KeeperException e)
		{
			logger.error(e.getMessage(), e);
		}
		catch (InterruptedException e)
		{
			logger.error(e.getMessage(), e);
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
		}

	}

	/**
	 * 非master节点在判断后执行的逻辑
	 */
	@Override
	public void doFollowerThings()
	{

	}

	/**
	 * 要执行的业务方法，这是个空方法，但是可以用来做aop，由具体业务类在after后织入逻辑
	 */
	@Override
	public void doSth()
	{
		//		System.out.println("dosth");
	}

	// the follows are set methods
	public void setRootDirectory(String rootDirectory)
	{
		this.rootDirectory = rootDirectory;
	}

	public void setZnodePrefix(String znodePrefix)
	{
		this.znodePrefix = znodePrefix;
	}

	public void setElectionType(int electionType)
	{
		this.electionType = electionType;
	}

	public void setJustifyMasterTimeout(int justifyMasterTimeout)
	{
		this.justifyMasterTimeout = justifyMasterTimeout;
	}

	public int getAcessCount()
	{
		return acessCount;
	}

	public void setAcessCount(int acessCount)
	{
		this.acessCount = acessCount;
	}

}
