package org.oltp1.runner.perf;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolBuilder
{
	public static ThreadPoolExecutor newThreadPool(int maxThreads, String threadPrefix)
	{
		// ThreadFactory tf = new DefaultThreadFactory(threadPrefix);
		ThreadPoolExecutor exec_srv = new ThreadPoolExecutor(maxThreads, maxThreads, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(50 * maxThreads), new ThreadPoolExecutor.CallerRunsPolicy());
		return exec_srv;
	}
}
