package org.bee.spring.dumpling;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.bee.spring.dumpling.annotation.ClusterSync;
import org.bee.spring.dumpling.annotation.PubishAfter;
import org.bee.spring.dumpling.annotation.Publish;
import org.bee.spring.dumpling.annotation.RemoteNotify;
import org.bee.spring.dumpling.annotation.RemotePublish;
import org.bee.spring.dumpling.annotation.RemoteSubscribe;
import org.bee.spring.dumpling.annotation.RemoteWait;
import org.bee.spring.dumpling.annotation.Subscribe;
import org.bee.spring.dumpling.util.Util;

class HandlrAnnotation {
	private final String beanName;
	private final SpringBowl bowl;
	
	public HandlrAnnotation(String beanName, SpringBowl bowl) {
		this.beanName = beanName;
		this.bowl = bowl;
	}
	
	public void handlrSubscribe(Method m) {
		if (m.isAnnotationPresent(Subscribe.class)) {
			Subscribe subcribe = m.getAnnotation(Subscribe.class);
			String path = subcribe.path();
			if (PubishAfter.Commit.equals(subcribe.runPolicy()) && !bowl.subscribeRunAfterCommitList.contains(path)) {
				bowl.subscribeRunAfterCommitList.add(path);
			}
			List<TargetCall> listCall = bowl.subScribeCallMap.get(path);
			if (listCall == null) {
				listCall = new ArrayList<TargetCall>();
				bowl.subScribeCallMap.put(path, listCall);
			}
			listCall.add(new TargetCall(m, null, beanName, subcribe.runPolicy()));
		} else {
			if (m.isAnnotationPresent(Publish.class)) {
				bowl.publishList.add(m.getAnnotation(Publish.class).path());
			}
		}
	}

	public void handlrClusterSync(Method m) {
		if (m.isAnnotationPresent(ClusterSync.class)) {
			bowl.clusterSyncList.add(m.getAnnotation(ClusterSync.class));
		}
	}
	
	public void handlrRemoteNotify(Method m) {
		if (m.isAnnotationPresent(RemoteNotify.class)) {
			String path = m.getAnnotation(RemoteNotify.class).path();
			if (!bowl.notifyList.contains(path)) {
				bowl.notifyList.add(path);
			}
		}
	}
	
	public void handlrRemoteWait(Method m) {
		if (m.isAnnotationPresent(RemoteNotify.class)) {
			String path = m.getAnnotation(RemoteWait.class).path();
			List<TargetCall> listCall = bowl.waitCallMap.get(path);
			if (listCall == null) {
				listCall = new ArrayList<TargetCall>();
				bowl.waitCallMap.put(path, listCall);
			}
			Util.handlrTargetCallList(listCall, beanName, m);
		}
	}
	
	public void handlrRemotePublish(Method m) {
		if (m.isAnnotationPresent(RemotePublish.class)) {
			String path = m.getAnnotation(RemotePublish.class).path();
			if (!bowl.remotePublishList.contains(path)) {
				bowl.remotePublishList.add(path);
			}
		}
	}
	
	public void handlrRemoteSubscribe(Method m) {
		if (m.isAnnotationPresent(RemoteSubscribe.class)) {
			String path = m.getAnnotation(RemoteSubscribe.class).path();
			List<TargetCall> listCall = bowl.remoteSubscribeMap.get(path);
			if (listCall == null) {
				listCall = new ArrayList<TargetCall>();
				bowl.remoteSubscribeMap.put(path, listCall);
			}
			Util.handlrTargetCallList(listCall, beanName, m);
		}
	}
}
