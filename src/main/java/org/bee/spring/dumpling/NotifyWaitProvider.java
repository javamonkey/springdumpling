package org.bee.spring.dumpling;

import org.aspectj.lang.JoinPoint;
import org.bee.spring.dumpling.annotation.RemoteNotify;


public interface NotifyWaitProvider
{
	public void notify(JoinPoint joinPoint, Object returnValue, RemoteNotify notify);

	/*逻辑中，直接发送*/
	public void notify(Object[] args, String path, boolean persist);

	public void init(SpringBowl bowl);

	public void close();
}
