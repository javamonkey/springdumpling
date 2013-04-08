package org.bee.spring.dumpling.util;

import org.bee.tl.core.BeeNumber;
import org.bee.tl.core.Context;
import org.bee.tl.core.Function;

public class FooFunction implements Function
{

	public Object call(Object[] args, Context ctx)
	{

		for (int i = 0; i < args.length; i++)
		{
			if (args[i] instanceof BeeNumber)
			{
				args[i] = ((BeeNumber) args[i]).orginalObject();
			}
		}
		return args;
	}

}
