package org.bee.spring.dumpling.clustersync.zk;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.data.Stat;

public class ZKCli extends AbstractZKClient
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		ZKCli cli = new ZKCli();

		try
		{
			ZKConf conf = new ZKConf();
			conf.setServer("127.0.0.1:2181");
			cli.setConf(conf);
			cli.connect();
			cli.setData("/abc", "000");
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 创建/更新一个节点，使用持久化方式
	 * 
	 * @param path
	 * @param data
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public void setData(String path, String data) throws KeeperException, InterruptedException
	{
		setData(path, data, CreateMode.PERSISTENT);
	}

	/**
	 * 创建更新一个节点
	 * 
	 * @param path
	 * @param data
	 * @param createMode
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public void setData(String path, String data, CreateMode createMode) throws KeeperException, InterruptedException
	{
		Stat stat = zk.exists(path, null);
		if (null == stat)
		{
			zk.create(path, data.getBytes(), conf.getAcls(), createMode);
		}
		else
		{
			zk.setData(path, data.getBytes(), -1);
		}
	}

	@Override
	public void process(WatchedEvent event)
	{
		super.process(event);
	}
}
