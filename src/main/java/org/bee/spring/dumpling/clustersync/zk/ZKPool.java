package org.bee.spring.dumpling.clustersync.zk;

import java.util.HashMap;
import java.util.Map;

public class ZKPool {
	private static Map<String, ZKClient> map = new HashMap<String, ZKClient>();

	public static void add(String rootDirectory, String znodePrefix, ZKClient client) {
		String key = getKey(rootDirectory, znodePrefix);
		ZKClient oldZkCli = map.get(key);
		// 如果原先有一个已经存在的ZKClient保存在池中，则先close旧的，再存入新的
		if (null != oldZkCli && oldZkCli.isAlive()) {
			oldZkCli.destroy();
		}
		map.put(key, client);
	}

	public static ZKClient get(String rootDirectory, String znodePrefix) {
		return map.get(getKey(rootDirectory, znodePrefix));
	}

	private static String getKey(String rootDirectory, String znodePrefix) {
		return rootDirectory + ',' + znodePrefix;
	}
}
