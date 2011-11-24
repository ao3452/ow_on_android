/*
 * Copyright 2006 National Institute of Advanced Industrial Science
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

package ow.routing.plaxton;

import ow.routing.RoutingAlgorithmConfiguration;

/**
 * An instance of this class holds a configuration for Plaxton's algorithm.
 */
public class PlaxtonConfiguration extends RoutingAlgorithmConfiguration {
	public static final int DEFAULT_DIGIT_SIZE = 4;
	public static final double DEFAULT_REPLACE_PROBABILITY = 0.25;
	public static final long DEFAULT_WAITING_TIME_FOR_JOIN_COMPLETES = 10000L;

	protected PlaxtonConfiguration() {}

	private int digitSize = DEFAULT_DIGIT_SIZE;
	public int getDigitSize() { return this.digitSize; }
	public int setDigitSize(int size) {
		int old = this.digitSize;
		this.digitSize = size;
		return old;
	}

	private double replaceProbability = DEFAULT_REPLACE_PROBABILITY;
	public double getReplaceProbability() { return this.replaceProbability; }
	public double setReplaceProbability(double prob) {
		double old = this.replaceProbability;
		this.replaceProbability = prob;
		return old;
	}

	private long waitingTimeForJoinCompletes = DEFAULT_WAITING_TIME_FOR_JOIN_COMPLETES;
	public long getWaitingTimeForJoinCompletes() { return this.waitingTimeForJoinCompletes; }
	public long setWaitingTimeForJoinCompletes(long wait) {
		long old = this.waitingTimeForJoinCompletes;
		this.waitingTimeForJoinCompletes = wait;
		return old;
	}
}
