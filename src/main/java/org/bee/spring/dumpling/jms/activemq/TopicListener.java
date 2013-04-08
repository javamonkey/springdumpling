package org.bee.spring.dumpling.jms.activemq;

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
import org.bee.spring.dumpling.SpringBowl;
import org.bee.spring.dumpling.TargetCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class TopicListener implements MessageListener {
	private Logger logger = LoggerFactory.getLogger(TopicListener.class);
	private SpringBowl springBowl;
	private String path;
	private Connection connection;

	public void init(String url, String user, String password) throws Exception {
		try {
			// Create the connection.
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
					user, password, url);
			connection = connectionFactory.createConnection();
			connection.start();

			// Create the session
			Session session = connection.createSession(false,
					Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createTopic(path);
			MessageConsumer consumer = session.createConsumer(destination);
			consumer.setMessageListener(this);

		} catch (Exception e) {
			throw e;
		}
	}

	public void close() {
		if (connection != null) {
			try {
				connection.close();
			} catch (JMSException e) {
			}
		}
	}

	public SpringBowl getSpringBowl() {
		return springBowl;
	}

	public void setSpringBowl(SpringBowl springBowl) {
		this.springBowl = springBowl;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * 这个是block的么？如果是，则得改为交给pool处理
	 */
	@Override
	public void onMessage(Message arg0) {
		ObjectMessage objectMesssage = (ObjectMessage) arg0;
		try {

			Object[] args = (Object[]) objectMesssage.getObject();
			if (arg0.getJMSRedelivered()) {
				logger.info("@RemoteSubscribe,Redelivered Message,Ignore it "
						+ Arrays.asList(args));
				return;
			}

			List<TargetCall> list = springBowl.getRemoteSubscribeMap()
					.get(path);
			ApplicationContext context = springBowl.getContext();
			for (TargetCall call : list) {
				String name = call.getBeanName();
				Object bean = context.getBean(name);
				Method m = call.getMethod();
				try {
					m.invoke(bean, args);
				} catch (ReflectiveOperationException e) {
					logger.error(e.getMessage(), e);
				}
			}

		} catch (JMSException e) {
			logger.info(e.getMessage(), e);
		}

	}
}
