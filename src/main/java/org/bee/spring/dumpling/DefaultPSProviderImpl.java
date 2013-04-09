package org.bee.spring.dumpling;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.aspectj.lang.JoinPoint;
import org.bee.spring.dumpling.annotation.Publish;
import org.bee.spring.dumpling.annotation.RunPolicy;
import org.bee.spring.dumpling.util.FooFunction;
import org.bee.tl.core.SimpleRuleEval;
import org.bee.tl.core.exception.SimpleEvalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.UnexpectedRollbackException;

public class DefaultPSProviderImpl implements PSProvider {
	Logger logger = LoggerFactory.getLogger(PSProvider.class);

	private void runMethod(final Method proxyMethod, final Object proxy, final Object[] args, boolean isThrowError) {
		try {
			proxyMethod.invoke(proxy, args);
		} catch (ReflectiveOperationException e) {
			logger.info(e.getMessage(), e);
		}
	}

	@Override
	public void run(JoinPoint joinPoint, Object returnValue, Publish pub, SpringBowl bowl, RunPolicy runPolicy) {
		Map<String, List<TargetCall>> map = bowl.getPsCallMap();
		ApplicationContext context = bowl.getContext();
		ThreadPoolExecutor pool = bowl.getPool();
		String path = pub.path();
		List<TargetCall> listCall = map.get(path);
		if (listCall == null) {
			return;
		}
		String ruleExp = pub.ruleExp();
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
		String argExp = pub.argExp();
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
			} catch (SimpleEvalException e) {
				throw new RuntimeException(e);
			}

		} else {
			realArgs = joinPoint.getArgs();
		}
		final RunnerWrapperService service = (RunnerWrapperService) context.getBean("dumpling-runnerWrapperService");
		final Object[] args = realArgs;
		for (TargetCall call : listCall) {
			if (!call.getRunPolicy().equals(runPolicy)) {
				continue;
			}
			final Method method = call.getMethod();
			method.setAccessible(true);// 允许调用
			final String beanName = call.getBeanName();
			final Object proxy = context.getBean(beanName);
			try {
				Class<?>[] types = method.getParameterTypes();
				final Method proxyMethod = proxy.getClass().getMethod(method.getName(), types);
				if (call.getRunPolicy().equals(RunPolicy.SameTransation)) {
					service.runWithRequiredTransaction(new Runnable() {
						@Override
						public void run() {
							runMethod(proxyMethod, proxy, args, true);
						}
					});
				} else {
					pool.execute(new Runnable() {
						public void run() {
							try {
								service.runWithNewTransaction(new Runnable() {
									@Override
									public void run() {
										runMethod(proxyMethod, proxy, args, false);
									}
								});
							} catch (UnexpectedRollbackException e) {
								logger.info(e.getMessage(), e);
							}
						}
					});
				}
			} catch (ReflectiveOperationException e) {
				logger.info(e.getMessage(), e);
			}
		}
	}
}
