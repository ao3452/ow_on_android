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

package ow.messaging.emulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ow.messaging.ExtendedMessageHandler;
import ow.messaging.Message;
import ow.messaging.MessageHandler;
import ow.messaging.MessageReceiver;
import ow.messaging.MessageSender;
import ow.messaging.MessagingAddress;
import ow.messaging.MessagingFactory;
import ow.messaging.Relayhandler;
import ow.messaging.Signature;
import ow.stat.MessagingReporter;
import ow.stat.StatConfiguration;
import ow.stat.StatFactory;

public final class EmuMessageReceiver implements MessageReceiver {
	private final static Logger logger = Logger.getLogger("messaging");

	protected static Map<EmuMessagingAddress,EmuMessageReceiver> receiverTable =
		new HashMap<EmuMessagingAddress,EmuMessageReceiver>();

	protected final EmuMessagingConfiguration config;
	private EmuMessagingAddress selfAddr;
	protected final EmuMessagingProvider provider;
	private final EmuMessageSender singletonSender;

	private final MessagingReporter msgReporter;

	private long latencyMillis;
	private int latencyNanos;
	private final SleepPeriodMeasure sleepPeriodMeasure;

	private List<MessageHandler> handlerList = new ArrayList<MessageHandler>();

	private final boolean useThreadPool;	// cache for performance
	private static boolean oomPrinted = false;

	protected EmuMessageReceiver(EmuMessagingConfiguration config, EmuHostID selfInetAddr, int port, EmuMessagingProvider provider) {
		this.config = config;
		this.selfAddr = new EmuMessagingAddress(selfInetAddr, port);
		this.provider = provider;
		this.singletonSender = new EmuMessageSender(this);

		StatConfiguration conf = StatFactory.getDefaultConfiguration();
		this.msgReporter = StatFactory.getMessagingReporter(conf, this.provider, this.getSender());

		long latencyMicros = this.config.getAdditionalLatencyMicros();
		if (latencyMicros > 0L) {
			this.latencyMillis = latencyMicros / 1000L;
			this.latencyNanos = (int)(latencyMicros - (1000 * this.latencyMillis)) * 1000;

			this.sleepPeriodMeasure = new SleepPeriodMeasure();
		}
		else {
			this.sleepPeriodMeasure = null;
		}

		this.useThreadPool = config.getUseThreadPool();
	}

	public MessagingAddress getSelfAddress() { return this.selfAddr; }
	public void setSelfAddress(String hostname) {
		this.selfAddr = (EmuMessagingAddress)this.provider.getMessagingAddress(
				hostname, this.selfAddr.getPort());
	}

	public int getPort() { return this.selfAddr.getPort(); }

	public MessagingReporter getMessagingReporter() { return this.msgReporter; }

	public void start() {
		synchronized (receiverTable) {
			receiverTable.put(this.selfAddr, this);
		}
	}

	public synchronized void stop() {
		synchronized (receiverTable) {
			receiverTable.remove(this.selfAddr);
		}
	}

	public MessageSender getSender() {
		return this.singletonSender;
	}

	public void addHandler(MessageHandler handler) {
		List<MessageHandler> newHandlerList = new ArrayList<MessageHandler>();

		synchronized (this) {
			newHandlerList.addAll(this.handlerList);	// copy
			newHandlerList.add(handler);

			this.handlerList = newHandlerList;	// substitute
		}
	}

	public void removeHandler(MessageHandler handler) {
		List<MessageHandler> newHandlerList = new ArrayList<MessageHandler>();

		synchronized (this) {
			newHandlerList.addAll(this.handlerList);	// copy
			newHandlerList.remove(handler);

			this.handlerList = newHandlerList;	// substitute
		}
	}

	protected EmuMessageHandler processAMessage(Message msg, EmuMessageSender sender) {
		// check signature
		byte[] sig = msg.getSignature();
		byte[] acceptableSig = this.provider.getMessageSignature();
		if (!Signature.match(sig, acceptableSig))
			return null;

		EmuMessageHandler msgHandler = new EmuMessageHandler(msg, sender);

		try {
			if (this.useThreadPool) {
				this.provider.getThreadPool().submit(msgHandler);
			}
			else {
				Thread t = new Thread(msgHandler);
				t.setDaemon(false);
				t.setName("EmuMessageHandler: " + msg);

				int prio = Thread.currentThread().getPriority()
					+ this.config.getReceiverThreadPriority();
				t.setPriority(prio);

				t.start();
			}
		}
		catch (OutOfMemoryError e) {
			try {
				Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			}
			catch (SecurityException e1) {/* ignore */}

			logger.log(Level.SEVERE, "# of threads: " + Thread.activeCount(), e);

			boolean doesPrint = false;
			synchronized (EmuMessageReceiver.class) {
				if (!EmuMessageReceiver.oomPrinted) { 
					EmuMessageReceiver.oomPrinted = true;
					doesPrint = true;
				}
			}

			if (doesPrint) {
				Thread[] tarray = new Thread[Thread.activeCount()];
				Thread.enumerate(tarray);
				for (Thread t: tarray) //if (t != null) System.out.println("Th: " + t.getName());
			//	System.out.flush();

				// forcibly kill the JVM
				Runtime.getRuntime().halt(1);
			}

			throw e;
		}

		return msgHandler;
	}

	private static final Object lockForlastID = new Object();
	private static int lastID = 0;

	protected class EmuMessageHandler extends Message implements Runnable {
		private final int uniqueID;

		private final Message message;
		private final EmuMessageSender sender;

		EmuMessageHandler(Message msg, EmuMessageSender sender /*can be null*/) {
			this.message = msg;
			this.sender = sender;

			synchronized (lockForlastID) {
				this.uniqueID = lastID++;
			}

			if (this.sender != null) {
				this.sender.prepareForReturnedMessage(this);
			}
		}

		public int hashCode() { return this.uniqueID; }

		public boolean equals(Object o) {
			if (o instanceof EmuMessageHandler) {
				EmuMessageHandler other = (EmuMessageHandler)o;
				if (this.uniqueID == other.uniqueID) return true;
			}
			return false;
		}

		public void run() {
			Thread th = Thread.currentThread();
			int receiverThreadPrio = config.getReceiverThreadPriority();
			int origPrio = 0;

			th.setName("EmuMessageHandler: " + this.message.getSource());
			if (receiverThreadPrio != 0) {
				origPrio = th.getPriority();
				th.setPriority(origPrio + receiverThreadPrio);
			}

			// add latency
			if (EmuMessageReceiver.this.sleepPeriodMeasure != null) {
				try {
					sleepPeriodMeasure.sleep(latencyMillis, latencyNanos);
				}
				catch (InterruptedException e) {/*ignore*/}
			}

			// process the received message
			Message ret = null;

			List<MessageHandler> currentHandlerList;
			synchronized (EmuMessageReceiver.this) {
				currentHandlerList = handlerList;
			}

			for (MessageHandler handler: currentHandlerList) {
				try {
					ret = handler.process(this.message);
				}
				catch (Throwable e) {
					logger.log(Level.SEVERE, "A MessageHandler threw an Exception.", e);
				}
			}

			// return a Message (from the last handler)
			if (this.sender != null) {
				if (sleepPeriodMeasure != null) {
					// add latency
					Runnable r = new MessageReturner(this.sender, this, ret);

					if (EmuMessageReceiver.this.useThreadPool) {
						EmuMessageReceiver.this.provider.getThreadPool().submit(r);
					}
					else {
						Thread t = new Thread(r);
						t.setDaemon(false);
						t.setName("MessageReturner: " + this.message);
						t.start();
					}
				}
				else {
					sender.setReturnedMessage(this, ret);
				}

				// logging
//				logger.log(Level.INFO, "return " + Tag.getStringByNumber(ret.getTag()) +
//						" from " + selfAddr + " to " + sender.selfAddr);

				// notify statistics collector
				// TODO: measures the length of the message.
				msgReporter.notifyStatCollectorOfMessageSent(
						this.message.getSource().getAddress(), ret, 0);
			}

			// post-process
			for (MessageHandler handler: currentHandlerList) {
				ExtendedMessageHandler extHandler;
				try {
					extHandler = (ExtendedMessageHandler)handler;
				}
				catch (ClassCastException e) { continue; }

				try {
					extHandler.postProcess(this.message);
				}
				catch (Throwable e) {
					logger.log(Level.SEVERE, "A MessageHandler threw an Exception.", e);
				}
			}

			th.setName(MessagingFactory.DEFAULT_POOLED_THREAD_NAME);
			if (receiverThreadPrio != 0) {
				th.setPriority(origPrio);
			}
		}
	}

	private final class MessageReturner implements Runnable {
		private final EmuMessageSender origSender;
		private final EmuMessageHandler marker;
		private final Message msgToBeReturned;

		MessageReturner(EmuMessageSender originalSender, EmuMessageHandler marker, Message msgToBeReturned) {
			this.origSender = originalSender;
			this.marker = marker;
			this.msgToBeReturned = msgToBeReturned;
		}

		public void run() {
			try {
				sleepPeriodMeasure.sleep(latencyMillis, latencyNanos);
			}
			catch (InterruptedException e) {/*ignore*/}

			this.origSender.setReturnedMessage(this.marker, msgToBeReturned);
		}
	}

	public void addrHandler(Relayhandler handler) {
		// TODO 自動生成されたメソッド・スタブ
		
	}
}
