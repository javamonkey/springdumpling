package org.bee.spring.dumpling.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClusterSync
{
	public String path();

	/*设置为true，第一次调用无视Cluster Lock*/
	public boolean allowAcessAsFistTime() default false;
}
