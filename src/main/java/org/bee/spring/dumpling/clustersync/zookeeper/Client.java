package org.bee.spring.dumpling.clustersync.zookeeper;

public interface Client {
	void destroy();

	void doFollowerThings();

	void doSth();

	void init() throws Exception;

	boolean isAlive();

	boolean isMaster();
}
