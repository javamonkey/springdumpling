package org.bee.spring.dumpling;

import java.lang.reflect.Method;

import org.bee.spring.dumpling.annotation.Subscribe;


public class TargetCall
{

	private Method m = null;
	private String path = null;
	private String beanName = null;
	private String runPolicy = Subscribe.AFTER_COMMIT;

	public Method getM()
	{
		return m;
	}

	public void setM(Method m)
	{
		this.m = m;
	}

	public String getPath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
	}

	public String getBeanName()
	{
		return beanName;
	}

	public void setBeanName(String beanName)
	{
		this.beanName = beanName;
	}

	
	
	public String getRunPolicy()
	{
		return runPolicy;
	}

	public void setRunPolicy(String runPolicy)
	{
		this.runPolicy = runPolicy;
	}

	public int hashCode()
	{
		return this.m.hashCode();
	}

	public boolean equals(Object o)
	{
		TargetCall other = (TargetCall) o;
		if (this.getBeanName().equals(other.getBeanName()) && this.getM().equals(other.getM()))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

}
