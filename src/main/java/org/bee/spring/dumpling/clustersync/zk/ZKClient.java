package org.bee.spring.dumpling.clustersync.zk;

public interface ZKClient
{

	void doSth();

	void doFollowerThings();

	boolean isMaster();

	boolean isAlive();

	void init() throws Exception;

	void destroy();
}
