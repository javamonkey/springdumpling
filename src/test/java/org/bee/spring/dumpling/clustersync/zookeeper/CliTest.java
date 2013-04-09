package org.bee.spring.dumpling.clustersync.zookeeper;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.apache.zookeeper.KeeperException;
import org.bee.spring.dumpling.clustersync.zookeeper.Cli;
import org.bee.spring.dumpling.clustersync.zookeeper.Config;
import org.junit.Test;

public class CliTest {
	
	@Test
	public void test() throws NoSuchAlgorithmException, IOException, InterruptedException, KeeperException {
		Cli cli = new Cli();
		cli.setConfig(new Config("localhost:2181"));
		cli.connect();
		cli.setData("/abc", "000");
	}
}
