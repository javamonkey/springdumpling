package org.bee.spring.dumpling;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.bee.spring.dumpling.annotation.ClusterSync;
import org.bee.spring.dumpling.clustersync.zk.ZKCandidate;
import org.bee.spring.dumpling.clustersync.zk.ZKConf;
import org.bee.spring.dumpling.clustersync.zk.ZkPool;
import org.springframework.context.ApplicationContext;


public class ZkClusterSyncProvider implements ClusterSyncProvider
{
	Logger logger = Logger.getLogger(ZkClusterSyncProvider.class);
	String znodePrefix = "cluster_lock";
	ZKConf zkConf = null;

	@Override
	public Object doBefore(ApplicationContext context, ProceedingJoinPoint pjp, ClusterSync clusterSync)
			throws Throwable
	{
		String rootDirectory = clusterSync.path();
		System.out.println("doBeforeZkMethod, " + rootDirectory + ", " + znodePrefix);
		ZKCandidate zkClient = (ZKCandidate) ZkPool.get(rootDirectory, znodePrefix);
		if (null == zkClient || !zkClient.isAlive())
		{
			try
			{
				zkClient = makeZKCandidate(context, clusterSync);
			}
			catch (Exception e)
			{
				logger.fatal(e.getMessage(), e);
				throw e;
			}
		}

		if (clusterSync.allowAcessAsFistTime())
		{
			int count = zkClient.getAcessCount();
			if (count == 0)
			{
				zkClient.setAcessCount(count + 1);
				return pjp.proceed();
			}
		}

		if (null != zkClient && zkClient.isAlive() && zkClient.isMaster())
		{
			int count = zkClient.getAcessCount();
			zkClient.setAcessCount(count + 1);
			return pjp.proceed();
		}
		return null;
	}

	@Override
	public void process(ApplicationContext context, ClusterSync clusterSync)
	{
		//		try
		//		{
		//			makeZKCandidate(context, clusterSync);
		//		}
		//		catch (Exception e)
		//		{
		//			logger.fatal(e.getMessage(), e);
		//		}
	}

	public String getZnodePrefix()
	{
		return znodePrefix;
	}

	public void setZnodePrefix(String znodePrefix)
	{
		this.znodePrefix = znodePrefix;
	}

	public ZKConf getZkConf()
	{
		return zkConf;
	}

	public void setZkConf(ZKConf zkConf)
	{
		this.zkConf = zkConf;
	}

	private ZKCandidate makeZKCandidate(ApplicationContext context, ClusterSync clusterSync) throws Exception
	{
		// 取得生成zkClient的三要素
		String rootDirectory = clusterSync.path();
		String znodePrefix = this.znodePrefix;
		if (zkConf == null)
		{
			//从服务器获取
			zkConf = (ZKConf) context.getBean("zkConf");
		}

		ZKCandidate zkCandidate = new ZKCandidate();
		zkCandidate.setConf(zkConf);
		zkCandidate.setRootDirectory(rootDirectory);
		zkCandidate.setZnodePrefix(znodePrefix);
		zkCandidate.init();
		ZkPool.add(rootDirectory, znodePrefix, zkCandidate);
		return zkCandidate;
	}
}
