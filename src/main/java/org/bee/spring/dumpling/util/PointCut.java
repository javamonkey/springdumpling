package org.bee.spring.dumpling.util;

public interface PointCut {
	String AllClusterSync = "@annotation(com.bee.spring.dumpling.annotation.ClusterSync)&&@annotation(clusterSync)";
	String AllPublish = "@annotation(com.bee.spring.dumpling.annotation.Publish)&&@annotation(pub)";
	String AllRemoteNotify = "@annotation(com.bee.spring.dumpling.annotation.RemoteNotify)&&@annotation(notify)";
	String AllRemotePublish = "@annotation(com.bee.spring.dumpling.annotation.RemotePublish)&&@annotation(pub)";
}
