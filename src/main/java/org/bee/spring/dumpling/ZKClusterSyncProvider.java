package org.bee.spring.dumpling;

import org.aspectj.lang.ProceedingJoinPoint;
import org.bee.spring.dumpling.annotation.ClusterSync;
import org.bee.spring.dumpling.clustersync.zk.ZKCandidate;
import org.bee.spring.dumpling.clustersync.zk.ZKConfig;
import org.bee.spring.dumpling.clustersync.zk.ZKPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class ZKClusterSyncProvider implements ClusterSyncProvider {
	private ZKConfig config;
	private Logger logger = LoggerFactory.getLogger(ZKClusterSyncProvider.class);
	private String znodePrefix = "cluster_lock";

	@Override
	public Object doBefore(ApplicationContext context, ProceedingJoinPoint pjp, ClusterSync sync) throws Throwable {
		ZKCandidate candidate = (ZKCandidate) ZKPool.get(sync.path(), znodePrefix);
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

	public ZKConfig getConfig() {
		return config;
	}

	public String getZNodePrefix() {
		return znodePrefix;
	}

	private ZKCandidate makeCandidate(ApplicationContext context, ClusterSync sync) throws Exception {
		// 构建ZKCandidate的三要素
		// rootDirectory(sync.path()) / znodePrefix / config
		config = (config == null) ? (ZKConfig) context.getBean("zkConf") : config;
		ZKCandidate candidate = new ZKCandidate();
		candidate.setConfig(config);
		candidate.setRootDirectory(sync.path());
		candidate.setZnodePrefix(znodePrefix);
		candidate.init();
		ZKPool.add(sync.path(), znodePrefix, candidate);
		return candidate;
	}

	@Override
	public void process(ApplicationContext context, ClusterSync clusterSync) {
	}

	public void setConfig(ZKConfig config) {
		this.config = config;
	}

	public void setZNodePrefix(String znodePrefix) {
		this.znodePrefix = znodePrefix;
	}
}
