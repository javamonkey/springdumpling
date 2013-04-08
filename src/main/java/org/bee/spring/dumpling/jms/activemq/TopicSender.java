package org.bee.spring.dumpling.jms.activemq;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

public class TopicSender
{
	Logger logger = Logger.getLogger(TopicSender.class);

	public void sendObject(String url, String path, String user, String password, boolean persistent,
			java.io.Serializable o)
	{
		Connection connection = null;
		try
		{
			// Create the connection.
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, url);
			connection = connectionFactory.createConnection();
			connection.start();

			// Create the session
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createTopic(path);

			MessageProducer producer = session.createProducer(destination);
			if (persistent)
			{
				producer.setDeliveryMode(DeliveryMode.PERSISTENT);
			}
			else
			{
				producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			}

			ObjectMessage objectMessage = session.createObjectMessage();
			objectMessage.setObject(o);
			producer.send(objectMessage);

		}
		catch (Exception e)
		{
			logger.warn("RemotePublish Error for " + o.toString(), e);

		}
		finally
		{
			try
			{

				connection.close();
			}
			catch (Throwable ignore)
			{
			}
		}
	}
}
