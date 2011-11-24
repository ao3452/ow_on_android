/*
 * Copyright 2009 Kazuyuki Shudo, and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ow.util.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class GlobalThreadPoolExecutors {
	public final static int NUM_THREADS_FOR_POOL = 32;
	public final static String POOLED_THREAD_NAME = "A pooled thread";
	public final static long KEEP_ALIVE_TIME = 3L;

	private final static ExecutorService concurrentBlockingNonDaemonEx;
	private final static ExecutorService concurrentBlockingDaemonEx;
	private final static ExecutorService concurrentNonBlockingNonDaemonEx;
	private final static ExecutorService concurrentNonBlockingDaemonEx;
	private final static ExecutorService serialBlockingNonDaemonEx;
	private final static ExecutorService serialBlockingDaemonEx;
	private final static ExecutorService serialNonBlockingNonDaemonEx;
	private final static ExecutorService serialNonBlockingDaemonEx;

	static {
		concurrentBlockingNonDaemonEx =
			new ConcurrentBlockingThreadPoolExecutor(0, NUM_THREADS_FOR_POOL,
					KEEP_ALIVE_TIME, TimeUnit.SECONDS,
					new NonDaemonThreadFactory());

		concurrentBlockingDaemonEx =
			new ConcurrentBlockingThreadPoolExecutor(0, NUM_THREADS_FOR_POOL,
					KEEP_ALIVE_TIME, TimeUnit.SECONDS,
					new DaemonThreadFactory());

		concurrentNonBlockingNonDaemonEx =
			new ConcurrentNonBlockingThreadPoolExecutor(0, NUM_THREADS_FOR_POOL,
					KEEP_ALIVE_TIME, TimeUnit.SECONDS,
					new NonDaemonThreadFactory());

		concurrentNonBlockingDaemonEx =
			new ConcurrentNonBlockingThreadPoolExecutor(0, NUM_THREADS_FOR_POOL,
					KEEP_ALIVE_TIME, TimeUnit.SECONDS,
					new DaemonThreadFactory());

		serialBlockingNonDaemonEx =
			new ThreadPoolExecutor(0, 1,
					KEEP_ALIVE_TIME, TimeUnit.SECONDS,
					new SynchronousQueue<Runnable>(),
					new NonDaemonThreadFactory());

		serialBlockingDaemonEx =
			new ThreadPoolExecutor(0, 1,
					KEEP_ALIVE_TIME, TimeUnit.SECONDS,
					new SynchronousQueue<Runnable>(),
					new DaemonThreadFactory());

		serialNonBlockingNonDaemonEx =
			new ThreadPoolExecutor(0, 1,
					KEEP_ALIVE_TIME, TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>(),
					new NonDaemonThreadFactory());

		serialNonBlockingDaemonEx =
			new ThreadPoolExecutor(0, 1,
					KEEP_ALIVE_TIME, TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>(),
					new DaemonThreadFactory());
	}

	private final static class NonDaemonThreadFactory implements ThreadFactory {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName(POOLED_THREAD_NAME);
			t.setDaemon(false);

			return t;
		}
	}

	private final static class DaemonThreadFactory implements ThreadFactory {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName(POOLED_THREAD_NAME);
			t.setDaemon(true);

			return t;
		}
	}

	public static ExecutorService getThreadPool(
			boolean serial, boolean blocking, boolean daemon) {
		if (!serial) {
			if (!blocking) {
				if (!daemon)
					return concurrentNonBlockingNonDaemonEx;
				else
					return concurrentNonBlockingDaemonEx;
			}
			else {
				if (!daemon)
					return concurrentBlockingNonDaemonEx;
				else
					return concurrentBlockingDaemonEx;
			}
		}
		else {
			if (!blocking) {
				if (!daemon)
					return serialNonBlockingNonDaemonEx;
				else
					return serialNonBlockingDaemonEx;
			}
			else {
				if (!daemon)
					return serialBlockingNonDaemonEx;
				else
					return serialBlockingDaemonEx;
			}
		}
	}
}
