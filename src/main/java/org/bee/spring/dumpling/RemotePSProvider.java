package org.bee.spring.dumpling;

import org.aspectj.lang.JoinPoint;
import org.bee.spring.dumpling.annotation.RemotePublish;

public interface RemotePSProvider {
	public void publish(JoinPoint point, Object returnValue, RemotePublish publish);

	/** 逻辑中，直接发送 */
	public void publish(Object[] args, String path, boolean persist);

	public void init(SpringBowl bowl);

	public void close();
}
