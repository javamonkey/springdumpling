package org.bee.spring.dumpling.clustersync.zk;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.data.Stat;

public class ZKCli extends AbstractZKClient {
	public static void main(String[] args) {
		ZKCli client = new ZKCli();
		ZKConfig cfg = new ZKConfig();
		cfg.setServer("localhost:2181");
		client.setConfig(cfg);
		client.connect();
		client.setData("/abc", "000");
	}

	/**
	 * 创建/更新一个节点，使用持久化方式
	 */
	public void setData(String path, String data) throws KeeperException, InterruptedException {
		setData(path, data, CreateMode.PERSISTENT);
	}

	/**
	 * 创建更新一个节点
	 */
	public void setData(String path, String data, CreateMode createMode) throws KeeperException, InterruptedException {
		Stat stat = zk.exists(path, null);
		if (null == stat) {
			zk.create(path, data.getBytes(), conf.getAcls(), createMode);
		} else {
			zk.setData(path, data.getBytes(), -1);
		}
	}

	@Override
	public void process(WatchedEvent event) {
		super.process(event);
	}
}
