package org.bee.spring.dumpling.clustersync.zookeeper;

import org.apache.zookeeper.ZooKeeper;

public class CliFactory {
	private static Cli cli;

	public void setCli(Cli zk) {
		cli = zk;
	}

	public static ZooKeeper getZk() {
		return cli.getKeeper();
	}
	
	private CliFactory() {
	}
}
