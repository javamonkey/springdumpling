package org.bee.spring.dumpling;

import java.lang.reflect.Method;

import org.bee.spring.dumpling.annotation.RunPolicy;

public class TargetCall {
	private Method method;
	private String path;
	private String beanName;
	private RunPolicy runPolicy = RunPolicy.AfterCommit;

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public RunPolicy getRunPolicy() {
		return runPolicy;
	}

	public void setRunPolicy(RunPolicy runPolicy) {
		this.runPolicy = runPolicy;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((beanName == null) ? 0 : beanName.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((runPolicy == null) ? 0 : runPolicy.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TargetCall other = (TargetCall) obj;
		if (beanName == null) {
			if (other.beanName != null)
				return false;
		} else if (!beanName.equals(other.beanName))
			return false;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (runPolicy != other.runPolicy)
			return false;
		return true;
	}
}
