package org.bee.spring.dumpling;

import org.aspectj.lang.ProceedingJoinPoint;
import org.bee.spring.dumpling.annotation.ClusterSync;
import org.springframework.context.ApplicationContext;


public interface ClusterSyncProvider
{
	public Object doBefore(ApplicationContext context, ProceedingJoinPoint pjp, ClusterSync clusterSync)
			throws Throwable;

	public void process(ApplicationContext context, ClusterSync clusterSync);
}
