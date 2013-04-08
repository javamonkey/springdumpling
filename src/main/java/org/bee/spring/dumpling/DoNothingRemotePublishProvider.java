package org.bee.spring.dumpling;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.bee.spring.dumpling.annotation.RemotePublish;


public class DoNothingRemotePublishProvider implements RemotePSProvider
{

	@Override
	public void publish(JoinPoint joinPoint, Object returnValue, RemotePublish pub)
	{
		System.out.println("DoNothingRemotePublishProvider: notify path=" + pub.path() + " ,target="
				+ joinPoint.getTarget() + " method:" + joinPoint.getSignature().getName());

	}

	@Override
	public void init(SpringBowl bowl)
	{
		System.out.println("DoNothingRemotePublishProvider: init");
		Map<String, List<TargetCall>> map = bowl.getWaitCallMap();
		System.out.println("DoNothingRemotePublishProvider listen:" + map.keySet());

	}

	@Override
	public void close()
	{
		System.out.println("DoNothingRemotePublishProvider: close");

	}

	@Override
	public void publish(Object[] args, String path, boolean persist)
	{
		System.out.println("DoNothingRemotePublishProvider: publish path=" + path + Arrays.asList(args));

	}

}
