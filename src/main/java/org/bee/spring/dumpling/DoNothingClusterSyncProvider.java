package org.bee.spring.dumpling;

import org.aspectj.lang.ProceedingJoinPoint;
import org.bee.spring.dumpling.annotation.ClusterSync;
import org.springframework.context.ApplicationContext;


public class DoNothingClusterSyncProvider implements ClusterSyncProvider
{

	@Override
	public Object doBefore(ApplicationContext context, ProceedingJoinPoint pjp, ClusterSync clusterSync)
			throws Throwable
	{
		System.out.println("No Clusterfor path,always run: " + clusterSync.path() + "@" + pjp.getTarget());
		return pjp.proceed();

	}

	@Override
	public void process(ApplicationContext context, ClusterSync clusterSync)
	{

	}

}
