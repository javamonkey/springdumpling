package org.bee.spring.dumpling.clustersync.zookeeper;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.data.Stat;

public class Cli extends AbstractClient {
	/** 创建/更新一个节点，使用持久化方式 */
	public void setData(String path, String data) throws KeeperException, InterruptedException {
		setData(path, data, CreateMode.PERSISTENT);
	}

	/** 创建更新一个节点 */
	public void setData(String path, String data, CreateMode createMode) throws KeeperException, InterruptedException {
		Stat stat = keeper.exists(path, null);
		if (null == stat) {
			keeper.create(path, data.getBytes(), config.getACLs(), createMode);
		} else {
			keeper.setData(path, data.getBytes(), -1);
		}
	}

	@Override
	public void process(WatchedEvent event) {
		super.process(event);
	}
}
