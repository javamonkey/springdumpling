package org.bee.spring.dumpling;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.bee.spring.dumpling.annotation.Publish;
import org.bee.spring.dumpling.annotation.Subscribe;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * 事物提交的时候在执行Pub动作
 * @author jzli
 *
 */
public class PSTransactionSynchronization implements TransactionSynchronization
{

	Publish pub;
	SpringBowl bowl;
	List<CallPara> list = new ArrayList<CallPara>(1);
	public PSTransactionSynchronization(Publish pub,SpringBowl bowl){
		this.pub = pub;
		this.bowl = bowl;
	}
	
	
	public void addCallPara(JoinPoint joinPoint, Object retVal){
		CallPara para = new CallPara(joinPoint,retVal);
		list.add(para);
		
	}
	
	@Override
	public void suspend()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void resume()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeCommit(boolean readOnly)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeCompletion()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void afterCommit()
	{
		//成功提交
		int a = 1 ;
		doPub();

	}

	@Override
	public void afterCompletion(int status)
	{
//		System.out.println("after completion "+status);

	}
	
	static class CallPara{
		CallPara(JoinPoint joinPoint, Object retVal){
			this.joinPoint = joinPoint;
			this.retVal = retVal;
			
		}
		JoinPoint joinPoint;
		Object retVal;
		
	}
	
	private void doPub(){
		for(CallPara para:list){
			bowl.getPsProvider().run(para.joinPoint, para.retVal, pub, bowl);
		}
	}


	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}
	
	

}
