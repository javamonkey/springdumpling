package org.bee.spring.dumpling.clustersync.zk;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author KevinLiao
 *
 */
public class ZkPool
{
	private static Map<String, ZKClient> zkMap = new HashMap<String, ZKClient>();

	public static void add(String rootDirectory, String znodePrefix, ZKClient client)
	{
		String key = getKey(rootDirectory, znodePrefix);
		ZKClient oldZkCli = zkMap.get(key);
		// 如果原先有一个已经存在的zkClient保存在池中，则先close旧的，再存入新的
		if (null != oldZkCli && oldZkCli.isAlive())
		{
			oldZkCli.destroy();
		}
		zkMap.put(key, client);
	}

	public static ZKClient get(String rootDirectory, String znodePrefix)
	{
		String key = getKey(rootDirectory, znodePrefix);
		return zkMap.get(key);
	}

	private static String getKey(String rootDirectory, String znodePrefix)
	{
		return new StringBuilder(rootDirectory).append(",").append(znodePrefix).toString();
	}
}
