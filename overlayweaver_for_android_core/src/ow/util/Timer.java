/*
 * Copyright 2007-2009 Kazuyuki Shudo, and contributors.
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

package ow.util;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An alternative to {@link java.util.Timer Timer} class.
 */
public final class Timer implements ClockJumpAwareSystem.ClockJumpHandler {
	final static Logger logger = Logger.getLogger("util");

	public final static int NUM_THREADS = 1;
	public final static long TASK_CHECK_INTERVAL = 5000L;
	public final static boolean EXECUTE_TASK_DIRECTLY = true;
	public final static long MAX_TARDINESS = 2000L;
	public final static boolean TERMINATE_THREADS_IF_NO_TASK = true;
	private final static long TIME_TO_KEEP_THREAD_AFTER_FINISH = 20 * 1000L;

	private SortedSet<ScheduledTask> taskSet;
	private Map<Runnable,ScheduledTask> taskTable;

	private ExecutorService threadPool = null;
	private TimerThread schedThread;
	private String threadName;
	private boolean isDaemon;
	private int threadPriority;
	private int numThreads = 0;

	public Timer() {
		this("Timer thread", false);
	}

	public Timer(String threadName, boolean isDaemon) {
		this(threadName, isDaemon, Thread.currentThread().getPriority());
	}

	public Timer(String threadName, boolean isDaemon, int threadPriority) {
		this.threadName = threadName;
		this.isDaemon = isDaemon;
		this.threadPriority = threadPriority;

		if (this.threadPriority > Thread.MAX_PRIORITY) this.threadPriority = Thread.MAX_PRIORITY;
		else if (this.threadPriority < Thread.MIN_PRIORITY) this.threadPriority = Thread.MIN_PRIORITY;

		// initialize
		this.taskSet = new TreeSet<ScheduledTask>();
		this.taskTable = new HashMap<Runnable,ScheduledTask>();

		if (!EXECUTE_TASK_DIRECTLY) {
			// instantiate a thread pool
			this.threadPool = Executors.newCachedThreadPool(new ScheduledThreadFactory());

			ThreadPoolExecutor ex = (ThreadPoolExecutor)this.threadPool;
			ex.setCorePoolSize(0);
			ex.setKeepAliveTime(3L, TimeUnit.SECONDS);
		}

		ClockJumpAwareSystem.registerClockJumpHandler(this);

		this.schedThread = new TimerThread();
	}

	private synchronized void startThreads() {
		while (this.numThreads < NUM_THREADS) {
			this.numThreads++;

			Thread t = new Thread(this.schedThread);
			t.setName(this.threadName);
			t.setDaemon(this.isDaemon);
			try {
				t.setPriority(this.threadPriority);
			}
			catch (Exception e) {
				logger.log(Level.WARNING, "Could not set thread priority: " + this.threadPriority, e);
			}

			t.start();
		}
	}

	private static class ScheduledThreadFactory implements ThreadFactory {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName("A scheduled thread");

			// a task should not be a daemon thread
			// to keep an emulator running while a task is running
			t.setDaemon(false);

			return t;
		}
	}

	private static Timer singletonTimer = null;

	public static Timer getSingletonTimer() {
		synchronized (Timer.class) {
			if (singletonTimer == null) {
				singletonTimer =
					new Timer("Singleton Timer", true, Thread.NORM_PRIORITY);
			}
		}

		return singletonTimer;
	}

	/**
	 * Schedules the specified task for execution at the specified (absolute) time.
	 */
	public void schedule(Runnable r, long absoluteTime) {
		this.schedule(r, absoluteTime, false);
	}

	/**
	 * Schedules the specified task for execution at the specified (absolute) time.
	 */
	public void schedule(Runnable r, long absoluteTime, boolean isDaemon) {
		ScheduledTask task = new ScheduledTask(r, absoluteTime, 0L, isDaemon);

		synchronized (this.taskSet) {
			this.taskSet.add(task);
			this.taskTable.put(r, task);

			this.taskSet.notify();
		}

		this.startThreads();
	}

	/**
	 * Schedules the specified task for repeated execution, beginning at the specified (absolute) time.
	 */
	public void scheduleAtFixedRate(Runnable r, long absoluteTime, long interval) {
		this.scheduleAtFixedRate(r, absoluteTime, interval, false);
	}

	public void scheduleAtFixedRate(Runnable r, long absoluteTime, long interval, boolean isDaemon) {
		ScheduledTask task = new ScheduledTask(r, absoluteTime, interval, isDaemon);

		synchronized (this.taskSet) {
			this.taskSet.add(task);
			this.taskTable.put(r, task);

			this.taskSet.notify();
		}

		this.startThreads();
	}

	/**
	 * Cancels the specified {@link Runnable Runnalbe} instance.
	 */
	public boolean cancel(Runnable r) {
		boolean scheduled = false;

		synchronized (this.taskSet) {
			ScheduledTask task = this.taskTable.get(r);
			if (task != null) {
				this.taskSet.remove(task);
				scheduled = true;
			}
		}

		return scheduled;
	}

	/*
	 * Returns (absolute) scheduled time of the specified {@link Runnable Runnable} instance.
	 */
	public long getScheduledTime(Runnable r) {
		ScheduledTask task = this.taskTable.get(r);

		if (task != null)
			return task.getScheduledTime();
		else
			return -1L;
	}

	public void stop() {
		synchronized (Timer.class) {
			if (this == singletonTimer) {
				singletonTimer = null;
			}
		}

		this.schedThread.stop();

		ClockJumpAwareSystem.unregisterClockJumpHandler(this);
	}

	// task representation
	private final static class ScheduledTask implements Comparable<ScheduledTask> {
		private final Runnable task;
		private long time;
		private final long interval;
		private boolean isDaemon;

		private ScheduledTask(Runnable task, long absoluteTime, boolean isDaemon) {
			this(task, absoluteTime, 0L, isDaemon);
		}

		private ScheduledTask(Runnable task, long absoluteTime, long interval, boolean isDaemon) {
			this.task = task;
			this.time = absoluteTime;
			this.interval = interval;
			this.isDaemon = isDaemon;
		}

		// accessors for time and interval
		protected Runnable getTask() { return this.task; }
		protected long getScheduledTime() { return this.time; }
		private void deferScheduledTime(long t) { this.time += t; }
		protected long getInterval() { return this.interval; }
		private boolean isDaemon() { return this.isDaemon; }

		// implements Comparable
		public int compareTo(ScheduledTask o) {
			int order = Long.signum(this.time - o.time);

			if (order != 0) return order;

			order = System.identityHashCode(o) - System.identityHashCode(this); 
				// 0 in case that `this' and `o' are the same ScheduledTask instance.

			return order;
		}
	}

	private class TimerThread implements Runnable {
		private boolean stopped = false;

		public void stop() {
			this.stopped = true;
		}

		public void run() {
			outerLoop:
			while (true) {
				ScheduledTask currentTask = null;

				// obtain a task
				// Note that this loop is required to support insertion of a task into
				while (true) {
					if (NUM_THREADS <= 1 || currentTask == null) {
						synchronized (Timer.this.taskSet) {
							while (true) {
								if (TERMINATE_THREADS_IF_NO_TASK && !Timer.this.isDaemon) {
									boolean nonDaemonTaskExists = false;
									for (ScheduledTask t: Timer.this.taskSet) {
										if (!t.isDaemon()) {
											nonDaemonTaskExists = true;
											break;
										}
									}
									if (!nonDaemonTaskExists) {
//System.out.println("Non-daemon task does not exists and break.");
										break outerLoop;
									}
								}

								try {
									currentTask = Timer.this.taskSet.first();	// throws NoSuchElementException

									// remove current task from the queue
									if (NUM_THREADS > 1) {
										Timer.this.taskSet.remove(currentTask);
										Timer.this.taskTable.remove(currentTask.getTask());
									}

									break;
								}
								catch (NoSuchElementException e) { /*ignore*/ }

								if (TERMINATE_THREADS_IF_NO_TASK) {
									// finish in case of empty
									break outerLoop;
								}
								else {
									// wait in case of empty
									try { Timer.this.taskSet.wait(); } catch (InterruptedException e) { /*ignore*/ }
								}
							}
						}	// synchronized (Timer.this.taskSet)
					}

					// sleep
					long sleepPeriod = currentTask.getScheduledTime() - System.currentTimeMillis();
					if (NUM_THREADS <= 1) {
						sleepPeriod = Math.min(sleepPeriod, TASK_CHECK_INTERVAL);
					}
/*
String cname = currentTask.getTask().getClass().getName();
cname = cname.substring(cname.lastIndexOf('.') + 1);
System.out.println("(sleep " + sleepPeriod + " ms: " + cname + " @ " + Integer.toHexString(System.identityHashCode(currentTask)) + ")");
System.out.flush();
*/
					try {
						ClockJumpAwareSystem.sleep(sleepPeriod);
						// invoke sleep() even with a negative period to adapt clock jump or overload.
					}
					catch (InterruptedException e) {
						logger.log(Level.WARNING, "ClockJumpAwareSystem#sleep() interrupted: " + Thread.currentThread().getName(), e);
					}
					if (sleepPeriod <= 0L) break;

					// check whether being stopped
					if (this.stopped) break outerLoop;
				} // while (true)

				// execute
				Runnable r = currentTask.getTask();

				if (EXECUTE_TASK_DIRECTLY) {
					try {
						r.run();
					}
					catch (Throwable e) {
						logger.log(Level.WARNING, "A task threw an exception: " + e, e);
					}
				}
				else {
					Timer.this.threadPool.submit(r);
				}

				// remove current task from the queue
				if (NUM_THREADS <= 1) {
					synchronized (Timer.this.taskSet) {
						Timer.this.taskSet.remove(currentTask);
						Timer.this.taskTable.remove(currentTask.getTask());
					}
				}

				// re-submit a periodic task
				long interval = currentTask.getInterval();
				if (interval > 0L) {
					Timer.this.scheduleAtFixedRate(r,
							currentTask.getScheduledTime() + interval, interval,
							currentTask.isDaemon());
				}
			}	// outerLoop: while (true)

			// decrement the number of threads
			synchronized (Timer.this) {
				Timer.this.numThreads--;
			}

			// sleep to keep the JVM running
			if (!Timer.this.isDaemon) {
				try {
					Thread.sleep(Timer.TIME_TO_KEEP_THREAD_AFTER_FINISH);
				} catch (InterruptedException e) { /*ignore*/ }
			}
		}
	}

	/**
	 * Implements {@link ClockJumpAwareSystem.ClockJumpHandler ClockJumpHandler}.
	 */
	public void clockJumped(long jumpWidth) {
/*
System.out.println("  schedules deferred: " + jumpWidth + " msec @ " + Integer.toHexString(System.identityHashCode(this)));
System.out.flush();
*/
		// system clock jumped and adjust schedules of already-schedule tasks
		synchronized (Timer.this.taskSet) {
			for (ScheduledTask t: Timer.this.taskSet) {
				t.deferScheduledTime(jumpWidth);
			}
		}
	}
}
