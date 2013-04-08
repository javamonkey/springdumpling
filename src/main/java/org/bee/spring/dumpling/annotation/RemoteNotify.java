package org.bee.spring.dumpling.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 发布到远程任意一个机器上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteNotify {
	/**
	 * 默认情况下，参数列表与方法参数一致
	 */
	public static final String SAME = "...";

	public String argExp() default SAME;

	public String path();

	/**
	 * 是否持久化该消息
	 */
	public boolean persisit() default false;

	public PubishAfter pubishAfter() default PubishAfter.Run;

	/**
	 * 规则列表，默认总是允许
	 */
	public String ruleExp() default "";

}
