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

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ow.messaging.Message;
import ow.messaging.MessageSender;
import ow.messaging.MessagingAddress;
import ow.messaging.emulator.EmuMessageReceiver.EmuMessageHandler;
import ow.stat.MessagingReporter;
import ow.util.AlarmClock;
import ow.util.ClockJumpAwareSystem;

public final class EmuMessageSender implements MessageSender {
	private final static Logger logger = Logger.getLogger("messaging");

	private final EmuMessageReceiver receiver;

	protected EmuMessageSender(EmuMessageReceiver receiver) {
		this.receiver = receiver;
	}

	public void send(MessagingAddress dest, Message msg) throws IOException {
		this.send(dest, msg, false);
	}

	private EmuMessageHandler send(MessagingAddress dest, Message msg, boolean toReturn) throws IOException {
		// set signature
		byte[] sig = this.receiver.provider.getMessageSignature();
		msg.setSignature(sig);

		// send
		EmuMessageReceiver receiver = null;
		synchronized (EmuMessageReceiver.receiverTable) {
			receiver = EmuMessageReceiver.receiverTable.get(dest);
		}

		if (receiver == null) {
			logger.log(Level.WARNING, "No such node: " + dest);

			MessagingReporter msgReporter = this.receiver.getMessagingReporter();
			if (msgReporter != null) {
				msgReporter.notifyStatCollectorOfDeletedNode(
						msg.getSource(), dest, msg.getTag());
			}

			throw new IOException("No such node: " + dest);
		}

		EmuMessageHandler msgHandler =
			receiver.processAMessage(msg, (toReturn ? this : null));

		// logging
//		logger.log(Level.INFO, "send: " + Tag.getStringByNumber(emuMsg.getMessage().getTag()) +
//				" from " + this.receiver.getSelfAddress() + " to " + emuAddr);

		// notify statistics collector
		MessagingReporter msgReporter = this.receiver.getMessagingReporter();
		if (msgReporter != null && !this.receiver.getSelfAddress().equals(dest)) {
			// TODO: measures the length of the message.
			msgReporter.notifyStatCollectorOfMessageSent(dest, msg, 0);
		}

		return msgHandler;
	}

	public Message sendAndReceive(MessagingAddress dest, Message msg) throws IOException {
		EmuMessageHandler marker = this.send(dest, msg, true);

		Message ret = null;

		synchronized (marker) {
			long timeout = this.receiver.provider.getTimeoutCalculator().calculateTimeout(dest);
			long start = System.currentTimeMillis();

			if (this.receiver.config.getUseTimerForTimeout()) {
				try {
					AlarmClock.setAlarm(timeout);

					while (true) {
						synchronized (this.returnedMsgMap) {
							ret = this.returnedMsgMap.get(marker);
							if (ret == null || !(ret instanceof EmuMessageHandler)) {
								// received
								this.returnedMsgMap.remove(marker);
								break;
							}
						}

						marker.wait();
					}

					AlarmClock.clearAlarm();

					// timeout calculation
					this.receiver.provider.getTimeoutCalculator().updateRTT(dest, (int)(System.currentTimeMillis() - start));
				}
				catch (Exception e) {
					// timeout
					synchronized (this.returnedMsgMap) {
						this.returnedMsgMap.remove(marker);
					}

					Thread.interrupted();	// clear the interrupted state
					logger.log(Level.INFO, "interrupted: " + e);

					throw new IOException("Timeout:" + timeout + " msec.");
				}
			}
			else {
				long timelimit = System.currentTimeMillis() + timeout;

				while (true) {
					synchronized (this.returnedMsgMap) {
						ret = this.returnedMsgMap.get(marker);
						if (ret == null || !(ret instanceof EmuMessageHandler)) {
							// received
							this.returnedMsgMap.remove(marker);
							break;
						}
					}

					long currentTimeout = timelimit - System.currentTimeMillis();
					if (currentTimeout > 0L) {
						try {
							ClockJumpAwareSystem.wait(marker, currentTimeout);
						}
						catch (InterruptedException e) {
							// NOTREACHED
							logger.log(Level.WARNING, "sendAndReceive() interrupted.", e);
						}
					}
					else {
						// timeout
						synchronized (this.returnedMsgMap) {
							this.returnedMsgMap.remove(marker);
						}

						throw new IOException("Timeout:" + timeout + " msec.");
					}
				}	// while (true)

				// timeout calculation
				this.receiver.provider.getTimeoutCalculator().updateRTT(dest, (int)(System.currentTimeMillis() - start));
			}	// if (config.getUseTimerForTimeout)
		}	// synchronized (nullMsg)

		return ret;
	}

	private final Map<EmuMessageHandler,Message> returnedMsgMap =
		new HashMap<EmuMessageHandler,Message>();

	void prepareForReturnedMessage(EmuMessageHandler marker) {
		synchronized (this.returnedMsgMap) {
			this.returnedMsgMap.put(marker, marker);
		}
	}

	void setReturnedMessage(EmuMessageHandler marker, Message msg) {
		// set signature
		if (msg != null) {
			msg.setSignature(this.receiver.provider.getMessageSignature());
		}

		synchronized (this.returnedMsgMap) {
//			Message markerMsg = this.returnedMsgMap.get(marker);
//			if (markerMsg != null && markerMsg instanceof EmuMessageHandler) {	// OK
				this.returnedMsgMap.put(marker, msg);
//			}
//			else {
//				logger.log(Level.WARNING, "Timeouted or there is an unhandled message from " + markerMsg.getSource());
//			}
		}

		synchronized (marker) {
			marker.notify();
		}
	}

	public SocketChannel sendAndCommunicate(MessagingAddress dest, Message msg) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	public SocketChannel sendAndSockGet(MessagingAddress dest, Message msg) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	public boolean sendData(SocketChannel sock, MessagingAddress dest, Message msg) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}
}
