package org.bee.spring.dumpling.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Subscribe {
	/**
	 * 订阅的path
	 */
	public String path();

	public RunPolicy runPolicy() default RunPolicy.AfterCommit;
}
