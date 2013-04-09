package org.bee.spring.dumpling;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.bee.spring.dumpling.annotation.ClusterSync;
import org.bee.spring.dumpling.annotation.CooperationService;
import org.bee.spring.dumpling.annotation.Publish;
import org.bee.spring.dumpling.annotation.RemoteNotify;
import org.bee.spring.dumpling.annotation.RemotePublish;
import org.bee.spring.dumpling.annotation.RunPolicy;
import org.bee.spring.dumpling.util.PointCut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 用于初始化
 * 
 * @author jzli
 */
@Aspect
public class SpringBowl implements BeanPostProcessor, ApplicationContextAware, ApplicationListener<ApplicationEvent> {
	public List<String> allService = new ArrayList<String>();
	public List<ClusterSync> clusterSyncList = new ArrayList<ClusterSync>();
	private ClusterSyncProvider clusterSyncProvider;
	private ApplicationContext context;
	private Logger logger = LoggerFactory.getLogger(getClass());

	/** RemoteNotify&RemoteWait */
	public List<String> notifyList = new ArrayList<String>();
	private NotifyWaitProvider nwProvider;

	public ThreadPoolExecutor pool = null;

	private int poolSize = 2;

	private PSProvider localProvider;
	public List<String> publishList = new ArrayList<String>();
	private RemotePSProvider remoteProvider;
	/** RemotePublish@RemoteSubscribe */
	public List<String> remotePublishList = new ArrayList<String>();

	public Map<String, List<TargetCall>> remoteSubscribeMap = new HashMap<String, List<TargetCall>>();
	public Map<String, List<TargetCall>> subScribeCallMap = new HashMap<String, List<TargetCall>>();
	/** 记录哪些path是需要在事物提交后执行的 */
	public List<String> subscribeRunAfterCommitList = new ArrayList<String>();
	public Map<String, List<TargetCall>> waitCallMap = new HashMap<String, List<TargetCall>>();

	/**
	 * 任何使用了@Pub的方法将在成功返回后调用(同步或者异步)使用了同样path的@Subscribe的方法
	 */
	@AfterReturning(pointcut = PointCut.AllPublish, returning = "retVal")
	@Order(99)
	public void afterReturning(JoinPoint joinPoint, Object retVal, Publish pub) {
		if (subscribeRunAfterCommitList.contains(pub.path())) {
			// 事物提交后执行
			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				List<TransactionSynchronization> list = TransactionSynchronizationManager.getSynchronizations();
				PSTransactionSynchronization sync = null;
				// 检查是否存在PSTransactionSynchronization
				for (Object object : list) {
					if (object instanceof PSTransactionSynchronization) {
						sync = (PSTransactionSynchronization) object;
						sync = new PSTransactionSynchronization(pub, this);
						TransactionSynchronizationManager.registerSynchronization(sync);
						break;
					}
				}
				sync.addCallPara(joinPoint, retVal);
			} else {
				// 不可能发生，不过保险起见
				throw new RuntimeException("没事物，不能使用PubishAfter.Commit");
			}
		}
		// 立即执行
		localProvider.run(joinPoint, retVal, pub, this, RunPolicy.SameTransation);
	}

	private void checkSubPub() {
		for (String path : this.publishList) {
			if (!this.subScribeCallMap.containsKey(path)) {
				logger.error("没有订阅者,缺少@Subscribe方法,Path:" + path);
			}
		}
		for (String key : subScribeCallMap.keySet()) {
			if (!this.publishList.contains(key)) {
				logger.error("没有发布者,缺少@Publish方法,Path:" + key);
			}
		}
	}

	/** 在ClusterSync之前的操作 */
	@Around(PointCut.AllClusterSync)
	@Order(120)
	public Object doBeforeClusterSync(ProceedingJoinPoint pjp, ClusterSync clusterSync) throws Throwable {
		return clusterSyncProvider.doBefore(context, pjp, clusterSync);
	}

	@AfterReturning(pointcut = PointCut.AllRemoteNotify, returning = "retVal")
	@Order(100)
	public void doNotify(JoinPoint joinPoint, Object retVal, RemoteNotify notify) {
		nwProvider.notify(joinPoint, retVal, notify);
	}

	@AfterReturning(pointcut = PointCut.AllRemotePublish, returning = "retVal")
	@Order(101)
	public void doRemotePublish(JoinPoint joinPoint, Object retVal, RemotePublish pub) {
		this.remoteProvider.publish(joinPoint, retVal, pub);
	}

	public ClusterSyncProvider getClusterSyncProvider() {
		return clusterSyncProvider;
	}

	public ApplicationContext getContext() {
		return context;
	}

	public NotifyWaitProvider getNwProvider() {
		return nwProvider;
	}

	public int getOrder() {
		return 80;
	}

	public ThreadPoolExecutor getPool() {
		return pool;
	}

	public int getPoolSize() {
		return poolSize;
	}

	public Map<String, List<TargetCall>> getPsCallMap() {
		return subScribeCallMap;
	}

	public PSProvider getPsProvider() {
		return localProvider;
	}

	public RemotePSProvider getRemotePSProvider() {
		return remoteProvider;
	}

	public Map<String, List<TargetCall>> getRemoteSubscribeMap() {
		return remoteSubscribeMap;
	}

	public Map<String, List<TargetCall>> getWaitCallMap() {
		return waitCallMap;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		// 或者考虑一个ArryQueue+Thread + CallerRunsPolicy,
		if (event instanceof ContextRefreshedEvent) {
			pool = new ThreadPoolExecutor(poolSize, poolSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
			if (localProvider == null) {
				// 使用默认协调者
				localProvider = new DefaultPSProviderImpl();
			}
			if (this.clusterSyncProvider == null) {
				this.clusterSyncProvider = new DoNothingClusterSyncProvider();
			}
			if (nwProvider == null) {
				// 使用一个懒惰的provider
				this.nwProvider = new DoNothingNotifyWaitProvider();
			}
			if (this.remoteProvider == null) {
				this.remoteProvider = new DoNothingRemotePublishProvider();
			}
			this.nwProvider.init(this);
			this.remoteProvider.init(this);
			for (String str : allService) {
				logger.info("CooperationService:" + str);
			}
			for (ClusterSync sync : this.clusterSyncList) {
				clusterSyncProvider.process(context, sync);
				logger.info("ClusterSync:  ,path=" + sync.path());
			}
			checkSubPub();
		} else if (event instanceof ContextStoppedEvent) {
			nwProvider.close();
			remoteProvider.close();
			// 停止，确保 pool执行完毕
			pool.shutdown();
			int size = pool.getQueue().size();
			logger.info("Srping-Dumpling Sotp:poolsize=" + size);
			if (!pool.isTerminated()) {
				logger.info("Srping-Dumpling wait shutdown");
			}
			while (!pool.isTerminated()) {
				try {
					Thread.sleep(1000 * 2);
					logger.info("Srping-Dumpling wait shutdown:left " + pool.getQueue().size());
				} catch (InterruptedException e) {
					break;
				}
			}
			logger.info("Srping-Dumpling Stop OK!");
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		return bean;
	}

	/*
	 * 找到容器管理bean的所有使用了@Subscribe的方法
	 */
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		// don't check parent class
		// just check the service with scope is singleton,but ignore for this
		// demo
		Class<?> clazz = bean.getClass();
		if (clazz.getAnnotation(CooperationService.class) != null) {
			if (allService.contains(clazz.getName())) {
				return bean;
			} else {
				allService.add(clazz.getName());
			}
			logger.info("CooperationService of " + clazz.getName());
			HandlrAnnotation handlrAnnotation = new HandlrAnnotation(beanName, this);
			for (Method m : bean.getClass().getMethods()) {
				if (m.getAnnotations().length == 0) {
					continue;
				}
				handlrAnnotation.handlrSubscribe(m);
				handlrAnnotation.handlrClusterSync(m);
				handlrAnnotation.handlrRemoteNotify(m);
				handlrAnnotation.handlrRemoteWait(m);
				handlrAnnotation.handlrRemotePublish(m);
				handlrAnnotation.handlrRemoteSubscribe(m);
			}
		}

		return bean;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	public void setClusterSyncProvider(ClusterSyncProvider clusterSyncProvider) {
		this.clusterSyncProvider = clusterSyncProvider;
	}

	public void setContext(ApplicationContext context) {
		this.context = context;
	}

	public void setNwProvider(NotifyWaitProvider nwProvider) {
		this.nwProvider = nwProvider;
	}

	public void setPool(ThreadPoolExecutor pool) {
		this.pool = pool;
	}

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	public void setPsProvider(PSProvider psProvider) {
		this.localProvider = psProvider;
	}

	public void setRemotePSProvider(RemotePSProvider remotePSProvider) {
		this.remoteProvider = remotePSProvider;
	}

	public void setWaitCallMap(Map<String, List<TargetCall>> waitCallMap) {
		this.waitCallMap = waitCallMap;
	}
}
