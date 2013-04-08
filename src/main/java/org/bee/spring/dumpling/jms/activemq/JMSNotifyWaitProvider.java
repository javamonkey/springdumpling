package org.bee.spring.dumpling.jms.activemq;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.bee.spring.dumpling.NotifyWaitProvider;
import org.bee.spring.dumpling.SpringBowl;
import org.bee.spring.dumpling.TargetCall;
import org.bee.spring.dumpling.annotation.Publish;
import org.bee.spring.dumpling.annotation.RemoteNotify;
import org.bee.spring.dumpling.util.FooFunction;
import org.bee.tl.core.SimpleRuleEval;

public class JMSNotifyWaitProvider implements NotifyWaitProvider {
	JMSConfig cfg = null;
	SpringBowl bowl = null;
	final Map<String, QueueListener> listenerMap = new HashMap<String, QueueListener>();

	@Override
	public void notify(JoinPoint joinPoint, Object returnValue,
			final RemoteNotify notify) {
		final String path = notify.path();

		String ruleExp = notify.ruleExp();
		String argExp = notify.argExp();
		if (ruleExp.length() != 0) {
			SimpleRuleEval evl = new SimpleRuleEval(ruleExp);
			evl.set("args", joinPoint.getArgs());
			evl.set("returnValue", returnValue);
			try {
				Object o = evl.calc();
				if (!(o instanceof Boolean)) {
					throw new RuntimeException("表达式应该返回布尔值");
				}
				boolean isContinueRun = (Boolean) o;
				if (!isContinueRun) {
					return;
				}
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		final Object[] realArgs;
		if (!argExp.equals(Publish.SAME)) {
			try {
				SimpleRuleEval evl = new SimpleRuleEval(argExp, "var kk=foo({0});");
				evl.registerFunction("foo", new FooFunction());
				evl.set("args", joinPoint.getArgs());
				evl.set("returnValue", returnValue);
				Object[] args = (Object[]) evl.calc("kk");
				realArgs = args;
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}

		} else {
			realArgs = joinPoint.getArgs();
		}

		bowl.getPool().execute(new Runnable() {
			public void run() {
				QueueSender queueSender = new QueueSender();
				queueSender.sendObject(cfg.getURL(), path, cfg.getUsername(),
						cfg.getPassword(), notify.persisit(), realArgs);
			}
		});

	}

	public void init(SpringBowl springBowl) {
		this.bowl = springBowl;
		Map<String, List<TargetCall>> map = bowl.getWaitCallMap();
		/* 初始化所有的message consumer */
		final Iterator<String> it = map.keySet().iterator();
		new Thread() {
			public void run() {
				while (it.hasNext()) {
					String path = it.next();
					QueueListener ls = new QueueListener();
					ls.setPath(path);
					ls.setSpringBowl(bowl);
					try {
						ls.init(cfg.getURL(), cfg.getUsername(),
								cfg.getPassword());
						listenerMap.put(path, ls);
					} catch (Exception e) {
						throw new RuntimeException("init listener failure "
								+ e.getMessage());
					}
				}

			}
		}.start();
	}

	public void close() {
		for (QueueListener listener : listenerMap.values()) {
			listener.close();
		}
	}

	public JMSConfig getJmsConf() {
		return cfg;
	}

	public void setJmsConf(JMSConfig jmsConf) {
		this.cfg = jmsConf;
	}

	@Override
	public void notify(final Object[] args, final String path,
			final boolean persist) {
		bowl.getPool().execute(new Runnable() {
			public void run() {
				new QueueSender().sendObject(cfg.getURL(), path,
						cfg.getUsername(), cfg.getPassword(), persist, args);
			}
		});
	}
}
