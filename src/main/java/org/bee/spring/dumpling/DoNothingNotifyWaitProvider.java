package org.bee.spring.dumpling;

import java.util.List;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.bee.spring.dumpling.annotation.RemoteNotify;


public class DoNothingNotifyWaitProvider implements NotifyWaitProvider
{

	@Override
	public void notify(JoinPoint joinPoint, Object returnValue, RemoteNotify notify)
	{
		System.out.println("DoNothingNotifyWaitProvider: notify path=" + notify.path() + " ,target="
				+ joinPoint.getTarget() + " method:" + joinPoint.getSignature().getName());

	}

	@Override
	public void init(SpringBowl bowl)
	{
		System.out.println("DoNothingNotifyWaitProvider: init");
		Map<String, List<TargetCall>> map = bowl.getWaitCallMap();
		System.out.println("DoNothingNotifyWaitProvider listen:" + map.keySet());

	}

	@Override
	public void close()
	{
		System.out.println("DoNothingNotifyWaitProvider: close");

	}

	@Override
	public void notify(Object[] args, String path, boolean persist)
	{
		// TODO Auto-generated method stub

	}

}
