package org.bee.spring.dumpling;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.aspectj.lang.JoinPoint;
import org.bee.spring.dumpling.annotation.Publish;
import org.bee.spring.dumpling.annotation.Subscribe;
import org.bee.spring.dumpling.util.FooFunction;
import org.bee.tl.core.SimpleRuleEval;
import org.bee.tl.core.exception.SimpleEvalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.UnexpectedRollbackException;

public class DefaultPSProviderImpl implements PSProvider {
	Logger logger = LoggerFactory.getLogger(PSProvider.class);

	@Override
	public void run(JoinPoint joinPoint, Object returnValue, Publish pub,
			SpringBowl bowl, String runPolicy) {
		Map<String, List<TargetCall>> map = bowl.getPsCallMap();
		ApplicationContext context = bowl.getContext();
		ThreadPoolExecutor pool = bowl.getPool();
		String path = pub.path();
		List<TargetCall> listCall = map.get(path);
		if (listCall == null) {
			return;
		}
		String ruleExp = pub.ruleExp();
		String argExp = pub.argExp();
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
			} catch (SimpleEvalException e) {
				throw new RuntimeException(e);
			}

		}
		Object[] realArgs = null;
		if (!argExp.equals(Publish.SAME)) {
			try {
				SimpleRuleEval evl = new SimpleRuleEval(argExp,
						"var kk=foo({0});");
				evl.registerFunction("foo", new FooFunction());
				evl.set("args", joinPoint.getArgs());
				evl.set("returnValue", returnValue);
				Object[] args = (Object[]) evl.calc("kk");
				realArgs = args;
			} catch (RuntimeException e) {
				throw e;
			} catch (SimpleEvalException e) {
				throw new RuntimeException(e);
			}

		} else {
			realArgs = joinPoint.getArgs();
		}
		final RunnerWrapperService serviceWrapper = (RunnerWrapperService) context
				.getBean("dumpling-runnerWrapperService");
		final Object[] args = realArgs;
		for (TargetCall call : listCall) {
			if (!call.getRunPolicy().equals(runPolicy)) {
				continue;
			}
			final Method m = call.getMethod();
			// 允许调用
			m.setAccessible(true);
			final String beanName = call.getBeanName();
			final Object proxy = context.getBean(beanName);
			try {
				String methodName = m.getName();
				Class[] types = m.getParameterTypes();
				final Method proxyMethod = proxy.getClass().getMethod(
						methodName, types);

				if (call.getRunPolicy().equals(Subscribe.SAME_TRANSATION)) {
					serviceWrapper.runWithRequiredTransaction(new Runnable() {

						@Override
						public void run() {
							runMethod(proxyMethod, proxy, args, true);

						}

					});
				} else {

					pool.execute(new Runnable() {
						public void run() {
							try {
								serviceWrapper.runWithNewTransaction(new Runnable() {
									@Override
									public void run() {
										runMethod(proxyMethod, proxy, args, false);
									}
								});
							} catch (UnexpectedRollbackException e) {
								e.printStackTrace();
								logger.info(e, e);
							} catch (Exception e) {
							}
						}
					});

				}
			} catch (IllegalArgumentException e) {
				logger.info(e, e);
			} catch (SecurityException e) {
				logger.info(e, e);
			} catch (NoSuchMethodException e) {
				logger.info(e, e);
			} catch (Throwable t) {
				logger.info(t, t);
			}
		}
	}

	private void runMethod(final Method proxyMethod, final Object proxy,
			final Object[] args, boolean isThrowError) {
		try {
			proxyMethod.invoke(proxy, args);
		} catch (IllegalArgumentException e) {
			if (isThrowError)
				throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			if (isThrowError)
				throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (isThrowError)
				throw new RuntimeException(e);
		}
	}

}
