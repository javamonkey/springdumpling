package org.bee.spring.dumpling;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 控制事物策略
 * @author jzli
 *
 */
@Service("dumpling-runnerWrapperService")
public class RunnerWrapperService
{

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void runWithNewTransaction(Runnable runner)
	{
		runner.run();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void runWithRequiredTransaction(Runnable runner)
	{
		runner.run();
	}
}
