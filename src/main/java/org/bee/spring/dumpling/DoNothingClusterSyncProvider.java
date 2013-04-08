package org.bee.spring.dumpling;

import org.aspectj.lang.ProceedingJoinPoint;
import org.bee.spring.dumpling.annotation.ClusterSync;
import org.springframework.context.ApplicationContext;

public class DoNothingClusterSyncProvider implements ClusterSyncProvider {
	public Object doBefore(ApplicationContext context, ProceedingJoinPoint pjp, ClusterSync sync) throws Throwable {
		System.out.print("No Clusterfor path,always run: ");
		System.out.print(sync.path());
		System.out.print('@');
		System.out.print(pjp.getTarget());
		System.out.println();
		return pjp.proceed();
	}

	public void process(ApplicationContext context, ClusterSync sync) {
	}
}
