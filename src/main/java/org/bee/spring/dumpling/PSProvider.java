package org.bee.spring.dumpling;

import org.aspectj.lang.JoinPoint;
import org.bee.spring.dumpling.annotation.RunPolicy;
import org.bee.spring.dumpling.annotation.Publish;

public interface PSProvider {
	public void run(JoinPoint joinPoint, Object returnValue, Publish pub, SpringBowl bowl, RunPolicy runPolicy);
}
