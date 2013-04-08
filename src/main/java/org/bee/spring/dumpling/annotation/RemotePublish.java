package org.bee.spring.dumpling.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * 发布到所有远程机器上
 * @author jzli
 * 
 */
@Target(
{ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface RemotePublish
{

	public static final String SAME = "...";

	public String path();

	public String argExp() default SAME;

	public String ruleExp() default "";

	public boolean persisit() default false;
	
	/*  目前暂时不支持  */
	public static final String PUBLISH_AFTER_RUN = "run";	
	public static final String PUBLISH_AFTER_COMMIT = "commit";
	
	public String pubishAfter() default PUBLISH_AFTER_RUN ;
}
