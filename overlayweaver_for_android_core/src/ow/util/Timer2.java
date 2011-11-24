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
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import ow.util.concurrent.GlobalThreadPoolExecutors;

/**
 * An alternative to {@link java.util.Timer Timer} class.
 */
public final class Timer2 {
	final static Logger logger = Logger.getLogger("util");

	//
	// parameters
	//

	// timer thread
	public final static boolean TERMINATE_THREADS_IF_NO_TASK = true;

	// thread pool
	public final static boolean USE_THREAD_POOL = true;

	// adaption to overload / clock jump
	public final static boolean ADAPT_TIMER_TO_OVERLOAD_AND_CLOCK_JUMP = true;
	public final static long MAX_JUMP_WIDTH = 1000L;
	public final static long ADDITIONAL_WAIT = 50L;	// 50 msec


	private SortedSet<ScheduledTask> taskSet;
	private Map<Runnable,ScheduledTask> taskTable;
	private int numNonDaemonTask;	// for TERMINATE_THREADS_IF_NO_TASK

	private TimerRunner timerRunner;
	private Set<Thread> timerThreadSet = new HashSet<Thread>();
	private String timerThreadName;
	private boolean isDaemon;
	private int timerThreadPriority;
	private int numThreads = 0;

	private long deferredTime = 0L;

//	private static Timer singletonTimer = null;

	public Timer2() {
		this("Timer thread", false);
	}

	public Timer2(String threadName, boolean isDaemon) {
		this(threadName, isDaemon, Thread.currentThread().getPriority());
	}

	public Timer2(String threadName, boolean isDaemon, int threadPriority) {
		this.timerThreadName = threadName;
		this.isDaemon = isDaemon;
		this.timerThreadPriority = threadPriority;

		if (this.timerThreadPriority > Thread.MAX_PRIORITY) this.timerThreadPriority = Thread.MAX_PRIORITY;
		else if (this.timerThreadPriority < Thread.MIN_PRIORITY) this.timerThreadPriority = Thread.MIN_PRIORITY;

		// initialize
		this.taskSet = new TreeSet<ScheduledTask>();
		this.taskTable = new HashMap<Runnable,ScheduledTask>();
		this.numNonDaemonTask = 0;

		this.timerRunner = new TimerRunner();
	}

	private synchronized void startAThread() {
		// instantiate a thread
		if (this.numThreads < 1) {
			Thread t = new Thread(this.timerRunner);
			t.setName(this.timerThreadName);
			t.setDaemon(this.isDaemon);
			try {
				t.setPriority(this.timerThreadPriority);
			}
			catch (Exception e) {
				logger.log(Level.WARNING, "Could not set thread priority: " + this.timerThreadPriority, e);
			}

			t.start();

			this.numThreads++;
			this.timerThreadSet.add(t);
		}
	}

	private synchronized void interruptAThread() {
		// kick existing threads
		for (Thread t: this.timerThreadSet) {
			t.interrupt();
		}
	}

//	public static Timer getSingletonTimer() {
//		synchronized (Timer.class) {
//			if (singletonTimer == null) {
//				singletonTimer =
//					new Timer("Singleton Timer", true /*isDaemon*/, Thread.NORM_PRIORITY);
//			}
//		}
//
//		return singletonTimer;
//	}

	/**
	 * Schedules the specified task for execution at the specified (absolute) time.
	 */
	public void schedule(Runnable r, long absoluteTime) {
		this.schedule(r, absoluteTime, false, false);
	}

	public void schedule(Runnable r, long absoluteTime, boolean isDaemon) {
		this.schedule(r, absoluteTime, isDaemon, false);
	}

	public void schedule(Runnable r, long absoluteTime, boolean isDaemon, boolean executeConcurrently) {
		ScheduledTask task = new ScheduledTask(r, absoluteTime, 0L, isDaemon, executeConcurrently);

		synchronized (this.taskSet) {
			this.taskSet.add(task);
			this.taskTable.put(r, task);
			if (!isDaemon) this.numNonDaemonTask++;

			this.taskSet.notify();
		}

		this.interruptAThread();
		this.startAThread();
	}

	/**
	 * Schedules the specified task for repeated execution, beginning at the specified (absolute) time.
	 */
	public void scheduleAtFixedRate(Runnable r, long absoluteTime, long interval) {
		this.scheduleAtFixedRate(r, absoluteTime, interval, false, false);
	}

	public void scheduleAtFixedRate(Runnable r, long absoluteTime, long interval, boolean isDaemon) {
		this.scheduleAtFixedRate(r, absoluteTime, interval, isDaemon, false);
	}

	public void scheduleAtFixedRate(Runnable r, long absoluteTime, long interval, boolean isDaemon, boolean executeConcurrently) {
		ScheduledTask task = new ScheduledTask(r, absoluteTime, interval, isDaemon, executeConcurrently);

		synchronized (this.taskSet) {
			this.taskSet.add(task);
			this.taskTable.put(r, task);
			if (!isDaemon) this.numNonDaemonTask++;

			this.taskSet.notify();
		}

		this.interruptAThread();
		this.startAThread();
	}

	/**
	 * Cancels the specified {@link Runnable Runnable} instance.
	 */
	public boolean cancel(Runnable r) {
		boolean scheduled = false;

		synchronized (this.taskSet) {
			ScheduledTask task = this.taskTable.get(r);
			if (task != null) {
				this.taskSet.remove(task);
				this.taskTable.remove(task.getTask());
				if (!task.isDaemon) this.numNonDaemonTask--;

				scheduled = true;
			}
		}

		this.interruptAThread();

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
		this.timerRunner.stop();

		synchronized (Timer2.class) {
			for (Thread t: this.timerThreadSet) {
				t.interrupt();
			}

//			if (this == singletonTimer) {
//				singletonTimer = null;
//			}
		}
	}

	public synchronized long getDeferredTime() {
		return this.deferredTime;
	}

	// task representation
	private final static class ScheduledTask implements Comparable<ScheduledTask> {
		private final Runnable task;
		private long time;
		private final long interval;
		private boolean isDaemon;
		private final boolean executedConcurrently;

		private ScheduledTask(Runnable task, long absoluteTime,
				boolean isDaemon, boolean executedConcurrently) {
			this(task, absoluteTime, 0L, isDaemon, executedConcurrently);
		}

		private ScheduledTask(Runnable task, long absoluteTime, long interval,
				boolean isDaemon, boolean executedConcurrently) {
			this.task = task;
			this.time = absoluteTime;
			this.interval = interval;
			this.isDaemon = isDaemon;
			this.executedConcurrently = executedConcurrently;
		}

		// accessors for time and interval
		protected Runnable getTask() { return this.task; }
		protected long getScheduledTime() { return this.time; }
		private void deferScheduledTime(long t) { this.time += t; }
		protected long getInterval() { return this.interval; }
		private boolean isDaemon() { return this.isDaemon; }
		private boolean executedConcurrently() { return this.executedConcurrently; }

		// implements Comparable
		public int compareTo(ScheduledTask o) {
			int order = Long.signum(this.time - o.time);

			if (order != 0) return order;

			order = System.identityHashCode(o) - System.identityHashCode(this); 
				// 0 in case that `this' and `o' are the same ScheduledTask instance.

			return order;
		}
	}

	private class TimerRunner implements Runnable {
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
					synchronized (Timer2.this.taskSet) {
						while (true) {
							if (TERMINATE_THREADS_IF_NO_TASK && !Timer2.this.isDaemon) {
								if (Timer2.this.numNonDaemonTask <= 0) {
//System.out.println("Non-daemon task does not exist and break.");
									break outerLoop;
								}
							}

							try {
								currentTask = Timer2.this.taskSet.first();	// throws NoSuchElementException
								break;
							}
							catch (NoSuchElementException e) { /*ignore*/ }

							if (TERMINATE_THREADS_IF_NO_TASK) {
								// finish in case of empty
								break outerLoop;
							}
							else {
								// wait in case of empty
								try { Timer2.this.taskSet.wait(); } catch (InterruptedException e) { /*ignore*/ }
							}
						}
					}	// synchronized (Timer.this.taskSet)

					// sleep
					long sleepPeriod = currentTask.getScheduledTime() - System.currentTimeMillis();
/*
String cname = currentTask.getTask().getClass().getName();
cname = cname.substring(cname.lastIndexOf('.') + 1);
System.out.println("(sleep " + sleepPeriod + " ms: " + cname + " @ " + Integer.toHexString(System.identityHashCode(currentTask)) + ")");
System.out.flush();
*/
					long excessiveSleepPeriod;

					if (sleepPeriod > 0L) {
						try {
							Thread.sleep(sleepPeriod);
						}
						catch (InterruptedException e) { /*ignore*/ }

						excessiveSleepPeriod = System.currentTimeMillis() - currentTask.getScheduledTime();
					}
					else {
						excessiveSleepPeriod = -sleepPeriod;
					}

					// check overload / clock jump
					if (excessiveSleepPeriod > MAX_JUMP_WIDTH) {
						// overload or clock jump detected
						System.out.println("[Clock jump or overload detected: "
								+ currentTask.getTask().getClass()
								+ " @ " + Integer.toHexString(System.identityHashCode(currentTask))
								+ ", " + excessiveSleepPeriod + " msec behind]");
						System.out.flush();

						excessiveSleepPeriod += ADDITIONAL_WAIT;

						synchronized (Timer2.this) {
							Timer2.this.deferredTime += excessiveSleepPeriod;
						}

						// adjust schedules of already-schedule tasks
						synchronized (Timer2.this.taskSet) {
							for (ScheduledTask t: Timer2.this.taskSet) {
								t.deferScheduledTime(excessiveSleepPeriod);
							}
						}
					}

					if (excessiveSleepPeriod >= 0) break;

					// check whether being stopped
					if (this.stopped) break outerLoop;
				} // while (true)

				// execute
				Runnable r = currentTask.getTask();

				if (USE_THREAD_POOL && currentTask.executedConcurrently()) {
					ExecutorService ex = GlobalThreadPoolExecutors.getThreadPool(
							false, true, currentTask.isDaemon());
					ex.submit(r);
				}
				else {
					try {
						r.run();
					}
					catch (Throwable e) {
						logger.log(Level.WARNING, "A task threw an exception: " + e, e);
					}
				}

				// remove current task from the queue
				synchronized (Timer2.this.taskSet) {
					Timer2.this.taskSet.remove(currentTask);
					Timer2.this.taskTable.remove(currentTask.getTask());
					if (!currentTask.isDaemon) Timer2.this.numNonDaemonTask--;
				}

				// re-submit a periodic task
				long interval = currentTask.getInterval();
				if (interval > 0L) {
					Timer2.this.scheduleAtFixedRate(r,
							currentTask.getScheduledTime() + interval, interval,
							currentTask.isDaemon(), currentTask.executedConcurrently());
				}
			}	// outerLoop: while (true)

			// decrement the number of threads
			synchronized (Timer2.this) {
				Timer2.this.numThreads--;
				Timer2.this.timerThreadSet.remove(Thread.currentThread());
			}
		}
	}
}
