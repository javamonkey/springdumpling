package org.bee.spring.dumpling.util;

import java.lang.reflect.Method;
import java.util.List;

import org.bee.spring.dumpling.TargetCall;

public class Util {
	/**
	 * 检查是否重复,否则,这会导致消息被处理俩次
	 */
	public static void handlrTargetCallList(List<TargetCall> listCall, String beanName, Method method) {
		if (listCall.size() != 0) {
			for (TargetCall call : listCall) {
				if (call.getBeanName().equals(beanName) && call.getMethod().equals(method)) {
					// Duplicate
					listCall.add(new TargetCall(method, null, beanName, null));
					break;
				}
			}
		} else {
			listCall.add(new TargetCall(method, null, beanName, null));
		}
	}
}
