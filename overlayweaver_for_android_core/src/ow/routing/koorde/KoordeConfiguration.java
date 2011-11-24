/*
 * Copyright 2006 Kazuyuki Shudo, and contributors.
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

package ow.routing.koorde;

import ow.routing.linearwalker.LinearWalkerConfiguration;

public final class KoordeConfiguration extends LinearWalkerConfiguration {
	public static final int DEFAULT_DIGIT_BITS = 1;	// should be < 32
	public static final int DEFAULT_NUM_EDGES = 3;

	public static final long DEFAULT_FIX_EDGE_MIN_INTERVAL = 10 * 1000L;
	public static final long DEFAULT_FIX_EDGE_MAX_INTERVAL = 120 * 1000L;
	public static final long DEFAULT_FIX_EDGE_INTERVAL_DELTA = 10 * 1000L;
	public static final double DEFAULT_FIX_EDGE_INTERVAL_PLAY_RATIO = 0.3;

	protected KoordeConfiguration() {}

	private int digitBits = DEFAULT_DIGIT_BITS;
	public int getDigitBits() { return digitBits; }
	public int setDigitBits(int bits) {
		int old = digitBits;
		digitBits = bits;
		return old;
	}

	private int numEdges = DEFAULT_NUM_EDGES;
	public int getNumEdges() { return numEdges; }
	public int setNumEdges(int n) {
		int old = numEdges;
		numEdges = n;
		return old;
	}

	private long fixEdgeMinInterval = DEFAULT_FIX_EDGE_MIN_INTERVAL;
	public long getFixEdgeMinInterval() { return fixEdgeMinInterval; }
	public long setFixEdgeMinInterval(long interval) {
		long old = fixEdgeMinInterval;
		fixEdgeMinInterval = interval;
		return old;
	}

	private long fixEdgeMaxInterval = DEFAULT_FIX_EDGE_MAX_INTERVAL;
	public long getFixEdgeMaxInterval() { return fixEdgeMaxInterval; }
	public long setFixEdgeMaxInterval(long interval) {
		long old = fixEdgeMaxInterval;
		fixEdgeMinInterval = interval;
		return old;
	}

	private long fixEdgeIntervalDelta = DEFAULT_FIX_EDGE_INTERVAL_DELTA;
	public long getFixEdgeIntervalDelta() { return fixEdgeIntervalDelta; }
	public long setFixEdgeIntervalDelta(long delta) {
		long old = fixEdgeIntervalDelta;
		fixEdgeIntervalDelta = delta;
		return old;
	}

	private double fixEdgeIntervalPlayRatio = DEFAULT_FIX_EDGE_INTERVAL_PLAY_RATIO;
	public double getFixEdgeIntervalPlayRatio() { return fixEdgeIntervalPlayRatio; }
	public double setFixEdgeIntervalPlayRatio(double ratio) {
		double old = fixEdgeIntervalPlayRatio;
		fixEdgeIntervalPlayRatio = ratio;
		return old;
	}
}
