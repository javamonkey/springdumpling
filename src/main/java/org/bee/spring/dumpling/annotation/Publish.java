package org.bee.spring.dumpling.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * 
 * @author jzli
 * 
 */
@Target(
{ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Publish
{

	public static final String SAME = "...";
	

	public String path();

	public String argExp() default SAME;

	public String ruleExp() default "";

}
