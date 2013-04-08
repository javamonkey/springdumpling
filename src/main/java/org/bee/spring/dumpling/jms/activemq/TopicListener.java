package org.bee.spring.dumpling.jms.activemq;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.bee.spring.dumpling.SpringBowl;
import org.bee.spring.dumpling.TargetCall;
import org.springframework.context.ApplicationContext;


public class TopicListener implements MessageListener
{
	Logger logger = Logger.getLogger(TopicListener.class);
	SpringBowl springBowl;
	String path = null;
	Connection connection = null;

	public void init(String url, String user, String password) throws Exception
	{

		try
		{
			// Create the connection.
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, url);
			connection = connectionFactory.createConnection();
			connection.start();

			// Create the session
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createTopic(path);
			MessageConsumer consumer = session.createConsumer(destination);
			consumer.setMessageListener(this);

		}
		catch (Exception e)
		{

			throw e;
		}

	}

	public void close()
	{

		try
		{

			if (connection != null)
				connection.close();
		}
		catch (Throwable ignore)
		{
		}

	}

	public SpringBowl getSpringBowl()
	{
		return springBowl;
	}

	public void setSpringBowl(SpringBowl springBowl)
	{
		this.springBowl = springBowl;
	}

	public String getPath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
	}

	/**
	 *  这个是block的么？如果是，则得改为交给pool处理
	 */
	@Override
	public void onMessage(Message arg0)
	{

		ObjectMessage objectMesssage = (ObjectMessage) arg0;
		try
		{

			Object[] args = (Object[]) objectMesssage.getObject();
			if (arg0.getJMSRedelivered())
			{
				logger.info("@RemoteSubscribe,Redelivered Message,Ignore it " + Arrays.asList(args));
				return;
			}

			List<TargetCall> list = springBowl.getRemoteSubscribeMap().get(path);
			ApplicationContext context = springBowl.getContext();
			for (TargetCall call : list)
			{
				String name = call.getBeanName();
				Object bean = context.getBean(name);
				Method m = call.getM();
				try
				{
					m.invoke(bean, args);
				}
				catch (IllegalArgumentException e)
				{
					logger.error(e, e);
				}
				catch (IllegalAccessException e)
				{
					logger.error(e, e);
				}
				catch (InvocationTargetException e)
				{
					Throwable a = e.getTargetException();
					logger.error(a, a);
				}
			}

		}
		catch (JMSException e)
		{
			logger.info(e, e);
		}

	}
}
