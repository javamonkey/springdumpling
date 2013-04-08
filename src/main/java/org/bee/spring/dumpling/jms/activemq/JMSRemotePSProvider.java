package org.bee.spring.dumpling.jms.activemq;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.bee.spring.dumpling.RemotePSProvider;
import org.bee.spring.dumpling.SpringBowl;
import org.bee.spring.dumpling.TargetCall;
import org.bee.spring.dumpling.annotation.RemotePublish;
import org.bee.spring.dumpling.util.FooFunction;
import org.bee.tl.core.SimpleRuleEval;


public class JMSRemotePSProvider implements RemotePSProvider
{

	JmsConfig jmsConf = null;
	SpringBowl bowl = null;
	final Map<String, TopicListener> listenerMap = new HashMap<String, TopicListener>();

	@Override
	public void publish(JoinPoint joinPoint, Object returnValue, RemotePublish pub)
	{
		final String path = pub.path();
		final boolean pesisit = pub.persisit();
		String ruleExp = pub.ruleExp();
		String argExp = pub.argExp();
		if (ruleExp.length() != 0)
		{
			SimpleRuleEval evl = new SimpleRuleEval(ruleExp);
			evl.set("args", joinPoint.getArgs());
			evl.set("returnValue", returnValue);
			try
			{
				Object o = evl.calc();
				if (!(o instanceof Boolean))
				{
					throw new RuntimeException("表达式应该返回布尔值");
				}
				boolean isContinueRun = (Boolean) o;
				if (!isContinueRun)
				{
					return;
				}
			}
			catch (RuntimeException e)
			{
				throw e;
			}
			catch(Exception ex){
				throw new RuntimeException(ex);
			}

		}
		final Object[] realArgs;
		if (!argExp.equals(RemotePublish.SAME))
		{
			try
			{
				SimpleRuleEval evl = new SimpleRuleEval(argExp, "var kk=foo({0});");
				evl.registerFunction("foo", new FooFunction());
				evl.set("args", joinPoint.getArgs());
				evl.set("returnValue", returnValue);
				Object[] args = (Object[]) evl.calc("kk");
				realArgs = args;
			}
			catch (RuntimeException e)
			{
				throw e;
			}
			catch(Exception ex){
				throw new RuntimeException(ex);
			}

		}
		else
		{
			realArgs = joinPoint.getArgs();
		}

		bowl.getPool().execute(new Runnable() {
			public void run()
			{
				TopicSender topicSender = new TopicSender();
				topicSender.sendObject(jmsConf.getUrl(), path, jmsConf.getUserName(), jmsConf.getPassword(), pesisit,
						realArgs);
			}
		});

	}

	public void init(SpringBowl springBowl)
	{
		this.bowl = springBowl;
		Map<String, List<TargetCall>> map = bowl.getRemoteSubscribeMap();
		/*初始化所有的message consumer*/
		final Iterator<String> it = map.keySet().iterator();
		new Thread() {
			public void run()
			{
				while (it.hasNext())
				{
					String path = it.next();
					TopicListener ls = new TopicListener();
					ls.setPath(path);
					ls.setSpringBowl(bowl);
					try
					{
						ls.init(jmsConf.getUrl(), jmsConf.getUserName(), jmsConf.getPassword());
						listenerMap.put(path, ls);
					}
					catch (Exception e)
					{
						throw new RuntimeException("init listener failure " + e.getMessage());
					}
				}

			}
		}.start();
	}

	public void close()
	{
		for (TopicListener l : listenerMap.values())
		{
			l.close();
		}
	}

	public JmsConfig getJmsConf()
	{
		return jmsConf;
	}

	public void setJmsConf(JmsConfig jmsConf)
	{
		this.jmsConf = jmsConf;
	}

	@Override
	public void publish(final Object[] args, final String path, final boolean persist)
	{
		bowl.getPool().execute(new Runnable() {
			public void run()
			{
				TopicSender topicSender = new TopicSender();
				topicSender.sendObject(jmsConf.getUrl(), path, jmsConf.getUserName(), jmsConf.getPassword(), persist,
						args);
			}
		});

	}

}
