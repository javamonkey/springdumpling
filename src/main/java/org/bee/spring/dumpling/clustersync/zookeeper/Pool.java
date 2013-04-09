package org.bee.spring.dumpling.clustersync.zookeeper;

import java.util.HashMap;
import java.util.Map;

public class Pool {
	private static Map<String, Client> map = new HashMap<String, Client>();

	public static void add(String rootDirectory, String znodePrefix, Client client) {
		String key = getKey(rootDirectory, znodePrefix);
		Client oldZkCli = map.get(key);
		// 如果原先有一个已经存在的ZKClient保存在池中，则先close旧的，再存入新的
		if (null != oldZkCli && oldZkCli.isAlive()) {
			oldZkCli.destroy();
		}
		map.put(key, client);
	}

	public static Client get(String rootDirectory, String znodePrefix) {
		return map.get(getKey(rootDirectory, znodePrefix));
	}

	private static String getKey(String rootDirectory, String znodePrefix) {
		return rootDirectory + ',' + znodePrefix;
	}
	
	private Pool() {
	}
}
