package org.bee.spring.dumpling;

import org.aspectj.lang.JoinPoint;
import org.bee.spring.dumpling.annotation.RemoteNotify;

public class DoNothingNotifyWaitProvider implements NotifyWaitProvider {
	@Override
	public void notify(JoinPoint joinPoint, Object returnValue, RemoteNotify notify) {
		System.out.print("DoNothingNotifyWaitProvider: notify path=");
		System.out.print(notify.path());
		System.out.print(" ,target=");
		System.out.print(joinPoint.getTarget());
		System.out.print(" method:");
		System.out.print(joinPoint.getSignature().getName());
		System.out.println();
	}

	@Override
	public void init(SpringBowl bowl) {
		System.out.println("DoNothingNotifyWaitProvider: init");
		System.out.print("DoNothingNotifyWaitProvider listen:");
		System.out.print(bowl.getWaitCallMap().keySet());
		System.out.println();
	}

	@Override
	public void close() {
		System.out.println("DoNothingNotifyWaitProvider: close");
	}

	@Override
	public void notify(Object[] args, String path, boolean persist) {
	}
}
