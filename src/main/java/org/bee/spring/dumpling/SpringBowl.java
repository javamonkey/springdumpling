package org.bee.spring.dumpling;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.bee.spring.dumpling.annotation.ClusterSync;
import org.bee.spring.dumpling.annotation.CooperationService;
import org.bee.spring.dumpling.annotation.Publish;
import org.bee.spring.dumpling.annotation.RemoteNotify;
import org.bee.spring.dumpling.annotation.RemotePublish;
import org.bee.spring.dumpling.annotation.RemoteSubscribe;
import org.bee.spring.dumpling.annotation.RemoteWait;
import org.bee.spring.dumpling.annotation.Subscribe;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.support.TransactionSynchronizationManager;


/**
 * 用于初始化
 * @author jzli
 *
 */
@Aspect
public class SpringBowl implements BeanPostProcessor, ApplicationContextAware, ApplicationListener /*, Ordered8*/
{

	Logger logger = Logger.getLogger(SpringBowl.class);
	int poolSize = 2;
	public ThreadPoolExecutor pool = null;
	public List<String> allService = new ArrayList<String>();
	public List<String> pubList = new ArrayList<String>();
	public Map<String, List<TargetCall>> subScribeCallMap = new HashMap<String, List<TargetCall>>();
	//记录哪些path是需要在事物提交后执行的
	public List<String> subscribeRunAfterCommitList = new ArrayList<String>();

	/* RemoteNotify&RemoteWait*/
	public List<String> notifyList = new ArrayList<String>();
	public Map<String, List<TargetCall>> waitCallMap = new HashMap<String, List<TargetCall>>();

	/*RemotePublish@RemoteSubscribe*/
	public List<String> remotePubList = new ArrayList<String>();
	public Map<String, List<TargetCall>> remoteSubscribeMap = new HashMap<String, List<TargetCall>>();

	public List<ClusterSync> clusterSyncList = new ArrayList<ClusterSync>();

	private ApplicationContext context;

	public static final String ALL_PUB_ANNOATION = "@annotation(com.bee.spring.dumpling.annotation.Publish)&&@annotation(pub)";//"execution(* com.mytest.HelloServiceImpl.*(..))" ;
	public static final String ALL_CLUSTERSYNC_ANNOTATION = "@annotation(com.bee.spring.dumpling.annotation.ClusterSync) && @annotation(clusterSync)";
	public static final String ALL_REMOTE_NOTIFY_ANNOTATION = "@annotation(com.bee.spring.dumpling.annotation.RemoteNotify) && @annotation(notify)";
	public static final String ALL_REMOTE_PUBLISH_ANNOTATION = "@annotation(com.bee.spring.dumpling.annotation.RemotePublish) && @annotation(pub)";

	PSProvider psProvider = null;
	ClusterSyncProvider clusterSyncProvider = null;
	NotifyWaitProvider nwProvider = null;
	RemotePSProvider remotePSProvider = null;

	
//	@Before(ALL_PUB_ANNOATION)
//	@Order(99)
//	public void beforeCall(JoinPoint joinPoint,  Publish pub){
//		int a = 1 ;
//		if(TransactionSynchronizationManager.isSynchronizationActive()){
//			List list = TransactionSynchronizationManager.getSynchronizations();
//			TransactionSynchronizationManager.registerSynchronization(new PSTransactionSynchronization());
//		}
//	}
	
	
	/**
	 * 任何使用了@Pub的方法将在成功返回后调用（同步或者异步）使用了同样path的@Subscribe的方法
	 * @param joinPoint
	 * @param pub
	 */
	@AfterReturning(pointcut = ALL_PUB_ANNOATION, returning = "retVal")
	@Order(99)
	public void afterReturning(JoinPoint joinPoint, Object retVal, Publish pub)
	{
		try
		{
			if(this.subscribeRunAfterCommitList.contains(pub.path())){
				//事物提交后执行
				if(TransactionSynchronizationManager.isSynchronizationActive()){
					List list = TransactionSynchronizationManager.getSynchronizations();
					boolean findPSSYnc = false ;
					PSTransactionSynchronization sync = null;
					//检查是否存在PSTransactionSynchronization
					for(Object s:list){
						if(s instanceof PSTransactionSynchronization ){
							findPSSYnc = true;
							sync = (PSTransactionSynchronization)s ;
							break;
						}
					}
					if(!findPSSYnc){
						sync = new  PSTransactionSynchronization(pub,this);
						TransactionSynchronizationManager.registerSynchronization(sync);
					}
					sync.addCallPara(joinPoint, retVal);
					
				}else{
					//不可能发生，不过保险起见
					throw new RuntimeException("没有事物，不能使用Publish.PUBLISH_AFTER_COMMIT");
				}
			}
			
			//立即执行
			psProvider.run(joinPoint, retVal, pub, this,Subscribe.SAME_TRANSATION);
			
			
			
			
		}
		catch (Throwable t)
		{
			//DO NOT THROW EXCEPTION
			logger.info(t, t);
		}

	}

	/* 
	 * 找到容器管理bean的所有使用了@Subscribe的方法
	 */
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
	{
		//don't check parent class
		//just check the service with scope is singleton,but ignore for this demo
		Class c = bean.getClass();
		if (c.getAnnotation(CooperationService.class) != null)
		{
			
			if(allService.contains(c.getName())){
				return bean;
			}else{
				allService.add(c.getName());
			}
			logger.info("CooperationService of " + c.getName());
			Method[] methods = bean.getClass().getMethods();
			for (Method m : methods)
			{
				if (m.getAnnotations().length == 0)
				{
					continue;
				}

				Annotation a = m.getAnnotation(Subscribe.class);
				if (null != a)
				{
					Subscribe s = (Subscribe) a;
					String path = s.path();
					TargetCall tc = new TargetCall();
					tc.setM(m);
					tc.setBeanName(beanName);
					tc.setRunPolicy(s.runPolicy());
					if(s.runPolicy().equals(Subscribe.AFTER_COMMIT)&&!this.subscribeRunAfterCommitList.contains(path)){
						this.subscribeRunAfterCommitList.add(path);
					}
					
					List<TargetCall> listCall = subScribeCallMap.get(path);
					if (listCall == null)
					{
						listCall = new ArrayList<TargetCall>();
						subScribeCallMap.put(path, listCall);
					}
					listCall.add(tc);
					

				}
				else
				{
					Publish pub = (Publish) m.getAnnotation(Publish.class);
					if (null != pub)
					{
						String path = pub.path();
						pubList.add(path);
					}
				}

				//继续检查其他annotation，如 @RemoteSynchronized ,@RemoteNotify等
				Annotation cs = m.getAnnotation(ClusterSync.class);
				if (null != cs)
				{
					ClusterSync clusterSync = (ClusterSync) cs;
					this.clusterSyncList.add(clusterSync);

				}

				RemoteNotify rn = m.getAnnotation(RemoteNotify.class);
				if (rn != null)
				{
					String path = rn.path();
					if (!notifyList.contains(path))
					{
						notifyList.add(path);
					}

				}

				RemoteWait rw = m.getAnnotation(RemoteWait.class);
				if (rw != null)
				{
					String path = rw.path();

					TargetCall tc = new TargetCall();
					tc.setM(m);
					tc.setBeanName(beanName);

					List<TargetCall> listCall = waitCallMap.get(path);
					if (listCall == null)
					{
						listCall = new ArrayList<TargetCall>();
						waitCallMap.put(path, listCall);
					}
					//检查是否重复，否则,这会导致消息被处理俩次

					if (listCall.size() != 0)
					{
						boolean isDuplicate = false;
						for (TargetCall call : listCall)
						{
							if (call.getBeanName().equals(beanName))
							{
								Method other = call.getM();
								if (other.equals(m))
								{
									isDuplicate = true;
									break;
								}
							}
						}
						if (!isDuplicate)
						{
							listCall.add(tc);
						}
					}
					else
					{
						listCall.add(tc);
					}

				}

				RemotePublish rp = m.getAnnotation(RemotePublish.class);
				if (rp != null)
				{
					String path = rp.path();
					if (!remotePubList.contains(path))
					{
						remotePubList.add(path);
					}
				}

				RemoteSubscribe rs = m.getAnnotation(RemoteSubscribe.class);
				if (rs != null)
				{
					String path = rs.path();

					TargetCall tc = new TargetCall();
					tc.setM(m);
					tc.setBeanName(beanName);

					List<TargetCall> listCall = remoteSubscribeMap.get(path);
					if (listCall == null)
					{
						listCall = new ArrayList<TargetCall>();
						remoteSubscribeMap.put(path, listCall);
					}
					//检查是否重复，否则,这会导致消息被处理俩次

					if (listCall.size() != 0)
					{
						boolean isDuplicate = false;
						for (TargetCall call : listCall)
						{
							if (call.getBeanName().equals(beanName))
							{
								Method other = call.getM();
								if (other.equals(m))
								{
									isDuplicate = true;
									break;
								}
							}
						}
						if (!isDuplicate)
						{
							listCall.add(tc);
						}
					}
					else
					{
						listCall.add(tc);
					}
				}
			}
		}

		return bean;
	}

	/**
	 * 在clusterSync之前的操作
	 * @param pjp
	 * @param clusterSync
	 * @return
	 * @throws Throwable
	 */
	@Around(ALL_CLUSTERSYNC_ANNOTATION)
	@Order(120)
	public Object doBeforeClusterSync(ProceedingJoinPoint pjp, ClusterSync clusterSync) throws Throwable
	{
		return clusterSyncProvider.doBefore(context, pjp, clusterSync);
	}

	@AfterReturning(pointcut = ALL_REMOTE_NOTIFY_ANNOTATION, returning = "retVal")
	@Order(100)
	public void doNotify(JoinPoint joinPoint, Object retVal, RemoteNotify notify)
	{
		try
		{

			this.nwProvider.notify(joinPoint, retVal, notify);
		}
		catch (Throwable t)
		{
			//DO NOT THROW EXCEPTION
			logger.info(t, t);
		}

	}

	@AfterReturning(pointcut = ALL_REMOTE_PUBLISH_ANNOTATION, returning = "retVal")
	@Order(101)
	public void doRemotePublish(JoinPoint joinPoint, Object retVal, RemotePublish pub)
	{
		try
		{

			this.remotePSProvider.publish(joinPoint, retVal, pub);
		}
		catch (Throwable t)
		{
			//DO NOT THROW EXCEPTION
			logger.info(t, t);
		}

	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
	{
		return bean;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		this.context = applicationContext;

	}

	public int getPoolSize()
	{
		return poolSize;
	}

	public void setPoolSize(int poolSize)
	{
		this.poolSize = poolSize;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event)
	{
		//或者考虑一个ArryQueue+Thread+ CallerRunsPolicy,
		if (event instanceof ContextRefreshedEvent)
		{
			pool = new ThreadPoolExecutor(poolSize, poolSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue());
			if (psProvider == null)
			{
				//使用默认协调者
				psProvider = new DefaultPSProviderImpl();
			}
			if (this.clusterSyncProvider == null)
			{

				this.clusterSyncProvider = new DoNothingClusterSyncProvider();
			}

			if (nwProvider == null)
			{
				//使用一个懒惰的provider
				this.nwProvider = new DoNothingNotifyWaitProvider();
			}

			if (this.remotePSProvider == null)
			{
				this.remotePSProvider = new DoNothingRemotePublishProvider();
			}

			this.nwProvider.init(this);
			this.remotePSProvider.init(this);
			for(String str:allService){
				logger.info("CooperationService:"+str);
			}
			
			for (ClusterSync cs : this.clusterSyncList)
			{
				clusterSyncProvider.process(context, cs);
				logger.info("ClusterSync:  ,path=" + cs.path());
			}

			checkSubPub();
		}
		else if (event instanceof ContextStoppedEvent)
		{
			nwProvider.close();
			remotePSProvider.close();

			//停止，确保 pool执行完毕
			pool.shutdown();
			int size = pool.getQueue().size();
			logger.info("Srping-Dumpling Sotp:poolsize=" + size);
			if (!pool.isTerminated())
			{
				logger.info("Srping-Dumpling wait shutdown");
			}
			while (!pool.isTerminated())
			{
				try
				{
					Thread.sleep(1000 * 2);
					logger.info("Srping-Dumpling wait shutdown:left " + pool.getQueue().size());
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					break;
				}
			}

			logger.info("Srping-Dumpling Stop OK!");

		}

	}

	private void checkSubPub()
	{
		for (String path : this.pubList)
		{
			if (!this.subScribeCallMap.containsKey(path))
			{
				logger.error("没有订阅者,缺少@Subscribe方法,Path:" + path);
				//				throw new RuntimeException("没有订阅者,Pub-path:"+path);
			}
		}

		for (String key : subScribeCallMap.keySet())
		{
			if (!this.pubList.contains(key))
			{
				logger.error("没有发布者,缺少@Publish方法,Path:" + key);
			}
		}
	}

	public ThreadPoolExecutor getPool()
	{
		return pool;
	}

	public void setPool(ThreadPoolExecutor pool)
	{
		this.pool = pool;
	}

	public ApplicationContext getContext()
	{
		return context;
	}

	public void setContext(ApplicationContext context)
	{
		this.context = context;
	}

	public Map<String, List<TargetCall>> getPsCallMap()
	{
		return subScribeCallMap;
	}

	public PSProvider getPsProvider()
	{
		return psProvider;
	}

	public void setPsProvider(PSProvider psProvider)
	{
		this.psProvider = psProvider;
	}

	public ClusterSyncProvider getClusterSyncProvider()
	{
		return clusterSyncProvider;
	}

	public void setClusterSyncProvider(ClusterSyncProvider clusterSyncProvider)
	{
		this.clusterSyncProvider = clusterSyncProvider;
	}

	public NotifyWaitProvider getNwProvider()
	{
		return nwProvider;
	}

	public void setNwProvider(NotifyWaitProvider nwProvider)
	{
		this.nwProvider = nwProvider;
	}

	public Map<String, List<TargetCall>> getWaitCallMap()
	{
		return waitCallMap;
	}

	public void setWaitCallMap(Map<String, List<TargetCall>> waitCallMap)
	{
		this.waitCallMap = waitCallMap;
	}

	public RemotePSProvider getRemotePSProvider()
	{
		return remotePSProvider;
	}

	public void setRemotePSProvider(RemotePSProvider remotePSProvider)
	{
		this.remotePSProvider = remotePSProvider;
	}

	public Map<String, List<TargetCall>> getRemoteSubscribeMap()
	{
		return remoteSubscribeMap;
	}

	
	public int getOrder()
	{
		return 80;
	}

}
