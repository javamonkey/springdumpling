package org.bee.spring.dumpling.clustersync.zk;

public interface ZKClient {
	void destroy();

	void doFollowerThings();

	void doSth();

	void init() throws Exception;

	boolean isAlive();

	boolean isMaster();
}
