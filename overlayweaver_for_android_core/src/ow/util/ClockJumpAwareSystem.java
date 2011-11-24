/*
 * Copyright 2007-2008 Kazuyuki Shudo, and contributors.
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

import java.util.HashSet;
import java.util.Set;

public final class ClockJumpAwareSystem {
	public final static boolean ADAPT_CLOCK_JUMP = true;

	public final static long MAX_JUMP_WIDTH = 1000L;
	public final static long ADDITIONAL_WAIT = 50L;	// 50 msec

	private static volatile long jumpWidth = 0L;
	public static long getJumpWidth() { return jumpWidth; }

	/**
	 * An alternative to {@link Thread#sleep(long) Thread#sleep()},
	 * but it adapts to a jump of system clock.
	 * Note that jump width added if the specified time < 0.
	 *
	 * @return increase of jumped time
	 */
	public static long sleep(long millis) throws InterruptedException {
		return waitOrSleep(null, millis, false);
	}

	/**
	 * An alternative to {@link System#wait(long) System#wait()},
	 * but it adapts to a jump of system clock.
	 * Note that jump width added if the specified time < 0.
	 *
	 * This method cannot wait again even if system clock jumped
	 * because it is impossible to distinguish clock jump from notification.
	 *
	 * @return increase of jumped time
	 */
	public static long wait(Object o, long millis) throws InterruptedException {
		return waitOrSleep(o, millis, true);
	}

	private static long waitOrSleep(Object o, long millis, boolean doesWait) throws InterruptedException {
		long sleepTimelimit = 0L;
		long jwCache = -1L;
		long jwDelta;
		long jwAddition = 0L;

//		boolean afterSleep = false;

		while (true) {
			long sleepPeriod;

			synchronized (ClockJumpAwareSystem.class) {	// getJumpWidth() and addJumpWidth() invoked atomically
				if (jwCache < 0L) {
					// initialize
					sleepTimelimit = System.currentTimeMillis() + millis;
					jwCache = getJumpWidth();

					jwDelta = 0L;
				}
				else {
					jwDelta = getJumpWidth() - jwCache;
				}

				sleepPeriod = sleepTimelimit + jwDelta - System.currentTimeMillis();

				if (sleepPeriod <= 0L /*&& afterSleep*/) {
					long excessiveJumpWidth = -sleepPeriod;

					if (ADAPT_CLOCK_JUMP
							&& excessiveJumpWidth > MAX_JUMP_WIDTH) {
						excessiveJumpWidth += ADDITIONAL_WAIT;

						jumpWidth += excessiveJumpWidth;
						jwAddition += excessiveJumpWidth;
					}

					break;
				}
			}

			// sleepPeriod > 0

			if (sleepPeriod > 7 * 24 * 3600 * 1000L /* 1 week */) {
				//System.out.println("[sleep period is very long: " + (sleepPeriod / 1000.0) + "sec]");
				//System.out.flush();
			}

			if (doesWait) {
				if (o == null) break;	// call o.wait() only once

				o.wait(sleepPeriod);	// throws InterruptedException

				o = null;
			}
			else {
				Thread.sleep(sleepPeriod);	// throws InterruptedException
			}

//			afterSleep = true;
		}	// while (true)

		// call clock jump handlers
		if (jwAddition > 0L) {
			//System.out.println("[Clock jump or overload detected: "
			//		+ jwAddition + " msec behind]");
			//System.out.flush();

			synchronized (handlerSet) {
				for (ClockJumpHandler h: handlerSet) {
					h.clockJumped(jwAddition);
				}
			}
		}

		return jwAddition;
	}

	public static interface ClockJumpHandler {
		public void clockJumped(long jumpWidth);
	}

	private static Set<ClockJumpHandler> handlerSet = new HashSet<ClockJumpHandler>();
	public static void registerClockJumpHandler(ClockJumpHandler handler) {
		synchronized (handlerSet) {
			handlerSet.add(handler);
		}
	}
	public static void unregisterClockJumpHandler(ClockJumpHandler handler) {
		synchronized (handlerSet) {
			handlerSet.remove(handler);
		}
	}
	public static void clearClockJumpHandler() {
		synchronized (handlerSet) {
			handlerSet.clear();
		}
	}
}
