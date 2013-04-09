package org.bee.spring.dumpling;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.bee.spring.dumpling.annotation.RunPolicy;
import org.bee.spring.dumpling.annotation.Publish;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * 事物提交的时候在执行@Publish动作
 * 
 * @author jzli
 */
public class PSTransactionSynchronization implements TransactionSynchronization {
	private static class CallPara {
		private JoinPoint point;
		private Object returnValue;

		private CallPara(JoinPoint point, Object returnValue) {
			this.point = point;
			this.returnValue = returnValue;
		}
	}
	private SpringBowl bowl;
	private List<CallPara> list = new ArrayList<CallPara>(1);

	private Publish publish;

	public PSTransactionSynchronization(Publish publish, SpringBowl bowl) {
		this.publish = publish;
		this.bowl = bowl;
	}

	public void addCallPara(JoinPoint point, Object returnValue) {
		list.add(new CallPara(point, returnValue));
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
			bowl.getPsProvider().run(para.point, para.returnValue, publish, bowl, RunPolicy.AfterCommit);
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
