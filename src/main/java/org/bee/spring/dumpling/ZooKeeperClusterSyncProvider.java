package org.bee.spring.dumpling;

import org.aspectj.lang.ProceedingJoinPoint;
import org.bee.spring.dumpling.annotation.ClusterSync;
import org.bee.spring.dumpling.clustersync.zookeeper.Candidate;
import org.bee.spring.dumpling.clustersync.zookeeper.Config;
import org.bee.spring.dumpling.clustersync.zookeeper.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class ZooKeeperClusterSyncProvider implements ClusterSyncProvider {
	private Config config;
	private Logger logger = LoggerFactory.getLogger(ZooKeeperClusterSyncProvider.class);
	private String znodePrefix = "cluster_lock";

	@Override
	public Object doBefore(ApplicationContext context, ProceedingJoinPoint pjp, ClusterSync sync) throws Throwable {
		Candidate candidate = (Candidate) Pool.get(sync.path(), znodePrefix);
		if (null == candidate || !candidate.isAlive()) {
			try {
				candidate = makeCandidate(context, sync);
			} catch (Exception e) {
				logger.debug(e.getMessage(), e);
				throw e;
			}
		}

		if (sync.allowAcessAsFistTime()) {
			int count = candidate.getAcessCount();
			if (count == 0) {
				candidate.setAcessCount(count + 1);
				return pjp.proceed();
			}
		}

		if (null != candidate && candidate.isAlive() && candidate.isMaster()) {
			int count = candidate.getAcessCount();
			candidate.setAcessCount(count + 1);
			return pjp.proceed();
		}
		return null;
	}

	public Config getConfig() {
		return config;
	}

	public String getZNodePrefix() {
		return znodePrefix;
	}

	private Candidate makeCandidate(ApplicationContext context, ClusterSync sync) throws Exception {
		// 构建ZKCandidate的三要素
		// rootDirectory(sync.path()) / znodePrefix / config
		Candidate candidate = new Candidate();
		candidate.setConfig(config = (config == null) ? (Config) context.getBean("zkConf") : config);
		candidate.setRootDirectory(sync.path());
		candidate.setZnodePrefix(znodePrefix);
		candidate.init();
		Pool.add(sync.path(), znodePrefix, candidate);
		return candidate;
	}

	@Override
	public void process(ApplicationContext context, ClusterSync sync) {
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public void setZNodePrefix(String znodePrefix) {
		this.znodePrefix = znodePrefix;
	}
}
