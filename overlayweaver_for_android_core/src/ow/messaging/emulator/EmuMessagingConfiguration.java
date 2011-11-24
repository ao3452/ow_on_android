/*
 * Copyright 2006-2009 National Institute of Advanced Industrial Science
 * and Technology (AIST), and contributors.
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

package ow.messaging.emulator;

import ow.messaging.MessagingConfiguration;

public class EmuMessagingConfiguration extends MessagingConfiguration {
	public final static int DEFAULT_INITIAL_ID = 0;		// initial host name is emu0
	public final static boolean DEFAULT_USE_TIMER_FOR_TIMEOUT = false;
	public final static long DEFAULT_ADDITIONAL_LATENCY_MICROS = 0L;	// usec

	// overrides the corresponding field of MessagingConfiguratoin
	public final static boolean DEFAULT_DO_TIMEOUT_CALCULATION = false;
	public final static int DEFAULT_RECEIVER_THREAD_PRIORITY = 0;

	private int initialID = DEFAULT_INITIAL_ID;
	public final int getInitialID() { return this.initialID; }
	public final int setInitialID(int id) {
		int old = this.initialID;
		this.initialID = id;
		return old;
	}

	private boolean useTimerForTimeout = DEFAULT_USE_TIMER_FOR_TIMEOUT;
	public final boolean getUseTimerForTimeout() { return this.useTimerForTimeout; }
	public final boolean setUseTimerForTimeout(boolean use) {
		boolean old = this.useTimerForTimeout;
		this.useTimerForTimeout = use;
		return old;
	}

	private long additionalLatencyMicros = DEFAULT_ADDITIONAL_LATENCY_MICROS;
	public final long getAdditionalLatencyMicros() { return this.additionalLatencyMicros; }
	public final long setAdditionalLatencyMicros(long t) {
		long old = this.additionalLatencyMicros;
		this.additionalLatencyMicros = t;
		return old;
	}

	//
	// overrides the corresponding methods of MessagingConfiguration
	//
	private boolean doTimeoutCalculation = DEFAULT_DO_TIMEOUT_CALCULATION;
	public final boolean getDoTimeoutCalculation() { return this.doTimeoutCalculation; }
	public final boolean setDoTimeoutCalculation(boolean flag) {
		boolean old = this.doTimeoutCalculation;
		this.doTimeoutCalculation = flag;
		return old;
	}

	private int receiverThreadPriority = DEFAULT_RECEIVER_THREAD_PRIORITY;
	public final int getReceiverThreadPriority() { return this.receiverThreadPriority; }
	public final int setReceiverThreadPriority(int prio) {
		int old = this.receiverThreadPriority;
		this.receiverThreadPriority = prio;
		return old;
	}
}
