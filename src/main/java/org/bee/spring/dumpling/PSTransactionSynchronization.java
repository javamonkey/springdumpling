package org.bee.spring.dumpling;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.bee.spring.dumpling.annotation.RunPolicy;
import org.bee.spring.dumpling.annotation.Publish;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * 事物提交的时候在执行Pub动作
 * 
 * @author jzli
 */
public class PSTransactionSynchronization implements TransactionSynchronization {
	private static class CallPara {
		JoinPoint joinPoint;
		Object retVal;
		CallPara(JoinPoint joinPoint, Object retVal) {
			this.joinPoint = joinPoint;
			this.retVal = retVal;
		}
	}
	private SpringBowl bowl;
	private List<CallPara> list = new ArrayList<CallPara>(1);

	private Publish pub;

	public PSTransactionSynchronization(Publish pub, SpringBowl bowl) {
		this.pub = pub;
		this.bowl = bowl;
	}

	public void addCallPara(JoinPoint joinPoint, Object retVal) {
		list.add(new CallPara(joinPoint, retVal));
	}

	@Override
	public void afterCommit() {
		doPub();
	}

	@Override
	public void afterCompletion(int status) {
	}

	@Override
	public void beforeCommit(boolean readOnly) {
	}

	@Override
	public void beforeCompletion() {
	}

	private void doPub() {
		for (CallPara para : list) {
			bowl.getPsProvider().run(para.joinPoint, para.retVal, pub, bowl, RunPolicy.AfterCommit);
		}
	}

	@Override
	public void flush() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void suspend() {
	}
}
