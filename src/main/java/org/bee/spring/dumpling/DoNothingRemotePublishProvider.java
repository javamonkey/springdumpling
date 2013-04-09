package org.bee.spring.dumpling;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.bee.spring.dumpling.annotation.RemotePublish;

public class DoNothingRemotePublishProvider implements RemotePSProvider {
	@Override
	public void publish(JoinPoint joinPoint, Object returnValue, RemotePublish pub) {
		System.out.print("DoNothingRemotePublishProvider: notify path=");
		System.out.print(pub.path());
		System.out.print(" ,target=");
		System.out.print(joinPoint.getTarget());
		System.out.print(" method:");
		System.out.print(joinPoint.getSignature().getName());
		System.out.println();
	}

	@Override
	public void init(SpringBowl bowl) {
		System.out.println("DoNothingRemotePublishProvider: init");
		System.out.print("DoNothingRemotePublishProvider listen:");
		System.out.print(bowl.getWaitCallMap());
		System.out.println();
	}

	@Override
	public void close() {
		System.out.println("DoNothingRemotePublishProvider: close");
	}

	@Override
	public void publish(Object[] args, String path, boolean persist) {
		System.out.print("DoNothingRemotePublishProvider: publish path=");
		System.out.print(path);
		System.out.print(Arrays.asList(args));
		System.out.println();
	}
}
