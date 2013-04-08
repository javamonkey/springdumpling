package org.bee.spring.dumpling.jms.activemq;

import java.io.Serializable;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicSender {
	private Logger logger = LoggerFactory.getLogger(TopicSender.class);

	public void sendObject(String url, String path, String user,
			String password, boolean persistent, Serializable o) {
		Connection connection = null;
		try {
			// Create the connection.
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
					user, password, url);
			connection = connectionFactory.createConnection();
			connection.start();

			// Create the session
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createTopic(path);

			MessageProducer producer = session.createProducer(destination);
			producer.setDeliveryMode(persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
			ObjectMessage objectMessage = session.createObjectMessage();
			objectMessage.setObject(o);
			producer.send(objectMessage);
		} catch (Exception e) {
			logger.warn("RemotePublish Error for " + o.toString(), e);
		} finally {
			try {
				connection.close();
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}
}
