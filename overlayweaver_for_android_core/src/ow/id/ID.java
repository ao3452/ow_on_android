/*
 * Copyright 2006-2008 National Institute of Advanced Industrial Science
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

package ow.id;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import ow.messaging.MessagingAddress;

/**
 * An ID with arbitrary length. Note that internal representation is big endian.
 */
public final class ID implements java.io.Externalizable, Comparable<ID> {
	public final static boolean USE_INT_ARRAY = false;

	private final static Logger logger = Logger.getLogger("id");

	private final static int MAX_SIZE = 127; // due to externalized format

	// private final static Map<ID,ID> canonicalizingMap = new
	// WeakHashMap<ID,ID>();

	// for message digest
	private static MessageDigest md = null;
	private final static String mdAlgoName = "SHA1";
	static {
		try {
			md = MessageDigest.getInstance(mdAlgoName);
		}
		catch (NoSuchAlgorithmException e) { /* NOTREACHED */
		}
	}

	private int size; // size in byte: 20 means 160 bit
	private byte[] value;
	// private volatile int intSize;
	// private volatile int[] intValue;
	// big endian
	private volatile BigInteger bigInteger;
	// can keep a BigInteger because it is immutable
	private volatile int hashCode;

	// //////////////////////////// 追加部分 2011.09.11
	// //////////////////////////////

	public static final int bitLength = 160; // IDの個数が2^160であるってこと
	public static final int byteLength = 20; // IDをbyteで表すと20byteってこと

	// //////////////////////////////////////////////////////////////////////////////

	/**
	 * A constructor. Do not copy the given byte array and keep it.
	 * 
	 * @param id
	 *            ID in big endian.
	 * @param size
	 *            ID size in byte.
	 */
	private ID(byte[] id, int size) {
		if (size > MAX_SIZE) {
			logger.log(Level.WARNING, "size set as " + MAX_SIZE + " even though the give size is " + size + ".");
			size = MAX_SIZE;
		}

		this.size = size;
		this.value = new byte[size];
		int idLength = Math.min(id.length, size);
		System.arraycopy(id, 0, this.value, size - idLength, idLength);

		this.init();
	}

	private void init() {
		// if (USE_INT_ARRAY) {
		// // generate int array
		// this.intSize = (this.size + 3) / 4;
		// this.intValue = new int[this.intSize];
		//
		// int value = 0, shift = 0, intIdx = this.intSize - 1;
		// for (int i = this.size - 1; i >= 0; i--) {
		// int b = 0xff & this.value[i];
		// b <<= (shift * 8);
		// value |= b;
		//
		// shift++;
		// if (shift > 3) {
		// shift = 0;
		//
		// intValue[intIdx] = value;
		// value = 0;
		//
		// intIdx--;
		// }
		// }
		// if (shift != 0) {
		// intValue[intIdx] = value;
		// }
		// }

		// create a BigInteger
		this.bigInteger = new BigInteger(1 /* positive */, this.value);

		// calculate the hashed value
		int hashedvalue = 0;

		// if (USE_INT_ARRAY) {
		// for (int i = 0; i < this.intSize; i++) {
		// hashedvalue ^= this.intValue[i];
		// }
		// }
		// else {
		int pos = 24;
		for (int i = 0; i < this.size; i++) {
			hashedvalue ^= (this.value[i] << pos);

			if (pos <= 0)
				pos = 24;
			else
				pos -= 8;
		}
		// }
		this.hashCode = hashedvalue;
	}

	private static ID canonicalize(ID obj) {
		return obj;

		// ID ret;
		// synchronized (canonicalizingMap) {
		// ret = canonicalizingMap.get(obj);
		// if (ret == null) { canonicalizingMap.put(obj, obj); ret = obj; }
		// }
		// return ret;
	}

	public ID copy(int newSize) {
		return canonicalize(new ID(this.value, newSize));
	}

	/**
	 * Returns a new ID instance with the value specified by the given byte
	 * array.
	 * 
	 * @param id
	 *            value in big endian.
	 * @param size
	 *            ID size in byte.
	 */
	public static ID getID(byte[] id, int size) {
		// copy the given byte array
		byte[] value = new byte[size];
		int copyLen = Math.min(id.length, size);
		int fromIdx = id.length - 1;
		int toIdx = size - 1;
		for (int i = 0; i < copyLen; i++) {
			value[toIdx--] = id[fromIdx--];
		}

		return canonicalize(new ID(value, size));
	}

	/**
	 * Returns a new ID instance with the value specified by the given
	 * BigInteger.
	 */
	public static ID getID(BigInteger id, int size) {
		if (id.compareTo(BigInteger.ZERO) < 0) {
			id = id.add(BigInteger.ONE.shiftLeft(size * 8));
		}

		byte[] value = id.toByteArray();
		return getID(value, size);
	}

	/**
	 * Returns a new ID instance with the value given as a String.
	 * 
	 * @param hexString
	 *            ID string.
	 * @param size
	 *            size of the ID in byte.
	 * @return generated ID.
	 */
	public static ID getID(String hexString, int size) {
		if (hexString.length() < size * 2) {
			throw new IllegalArgumentException("Given ID is too short: " + hexString);
		}

		byte[] id = new byte[size];
		for (int i = 0, idx = (size - 1) * 2; i < size; i++, idx -= 2) {
			int b = Integer.parseInt(hexString.substring(idx, idx + 2), 16);
			id[size - 1 - i] = (byte) (b & 0xff);
		}

		return canonicalize(new ID(id, size));
	}

	private static Random rnd = new Random();

	/**
	 * Returns a new ID having random value.
	 */
	public static ID getRandomID(int size) {
		byte[] value = new byte[size];
		rnd.nextBytes(value);
		return canonicalize(new ID(value, size));
	}

	/**
	 * Returns a newly generated ID with a hashed value of the specified byte
	 * array. The size of ID is 20.
	 */
	public static ID getSHA1BasedID(byte[] bytes) {
		return getSHA1BasedID(bytes, 20); // 20: default length of SHA1
	}

	/**
	 * Returns a newly generated ID with a hashed value of the specified byte
	 * array. Maximum size of ID is 160 bit (20 byte) because the hashing
	 * algorithm is SHA1.
	 * 
	 * @param sizeInByte
	 *            the size of generated ID in byte (<= 20).
	 */
	public static ID getSHA1BasedID(byte[] bytes, int sizeInByte) {
		byte[] value;
		synchronized (md) {
			value = md.digest(bytes);
		}

		if (sizeInByte > value.length) {
			throw new IllegalArgumentException("size is too large: " + sizeInByte + " > " + value.length);
		}

		return canonicalize(new ID(value, sizeInByte));
	}

	/**
	 * Returns a newly generated ID based on the hashcode of the specified
	 * object. Maximum size of ID is 160 bit (20 byte) because the hashing
	 * algorithm is SHA1.
	 * 
	 * @param sizeInByte
	 *            the size of generated ID in byte (<= 20).
	 */
	public static ID getHashcodeBasedID(Object obj, int sizeInByte) {
		int hashcode = obj.hashCode();
		byte[] bytes = new byte[4];
		for (int i = 0; i < 4; i++) {
			bytes[i] = (byte) ((hashcode >>> ((3 - i) * 8)) & 0xff);
		}

		return getSHA1BasedID(bytes, sizeInByte);
	}

	/**
	 * Length of this ID in byte.
	 */
	public int getSize() {
		return this.size;
	}

	public byte[] getValue() {
		return this.value;
	}

	/**
	 * Returns bits.
	 * 
	 * @param from
	 *            starting index from LSB.
	 * @param len
	 *            number of bits.
	 */
	public int getBits(int from, int len) {
		int result = 0;

		for (int i = 0; i < len; i++) {
			int index = from + i;

			if (index >= 0) {
				if (this.bigInteger.testBit(from + i)) {
					result |= (1 << i);
				}
			}
		}

		return result;
	}

	/**
	 * Returns an ID whose value is (this << n).
	 */
	public ID shiftLeft(int n) {
		return getID(this.toBigInteger().shiftLeft(n), this.size);
	}

	/**
	 * Returns an ID whose value is (this >> n).
	 */
	public ID shiftRight(int n) {
		return getID(this.toBigInteger().shiftRight(n), this.size);
	}

	/**
	 * Returns an ID whose value is equivalent to this ID with the designated
	 * bit set.
	 */
	public ID setBit(int n) {
		return getID(this.toBigInteger().setBit(n), this.size);
	}

	/**
	 * Returns an ID whose value is equivalent to this ID with the designated
	 * bit cleared.
	 */
	public ID clearBit(int n) {
		return getID(this.toBigInteger().clearBit(n), this.size);
	}

	public static int matchLengthFromMSB(ID a, ID b) {
		int aRemainingSize = a.getSize();
		int bRemainingSize = b.getSize();

		int aIndex = 0, bIndex = 0;
		int matchBytes = 0;
		int matchBits = 8;

		while (aRemainingSize > bRemainingSize) { // a is longer than b
			int v = 0xff & a.value[aIndex];
			if (v != 0) {
				while (v != 0) {
					v >>>= 1;
					matchBits--;
				}
				return matchBytes * 8 + matchBits;
			}

			matchBytes++;
			aIndex++;
			aRemainingSize--;
		}

		while (bRemainingSize > aRemainingSize) { // b is longer than a
			int v = 0xff & b.value[bIndex];
			if (v != 0) {
				while (v != 0) {
					v >>>= 1;
					matchBits--;
				}
				return matchBytes * 8 + matchBits;
			}

			matchBytes++;
			bIndex++;
			bRemainingSize--;
		}

		for (int i = 0; i < aRemainingSize; i++) {
			int va = 0xff & a.value[aIndex];
			int vb = 0xff & b.value[bIndex];
			if (va != vb) {
				int xored = va ^ vb;
				while (xored != 0) {
					xored >>>= 1;
					matchBits--;
				}
				return matchBytes * 8 + matchBits;
			}

			matchBytes++;
			aIndex++;
			bIndex++;
		}

		return matchBytes * 8;
	}

	public BigInteger toBigInteger() {
		return this.bigInteger;
	}

	public boolean equals(Object obj) {
		if (obj == null)
			return false;

		ID other;
		try {
			other = (ID) obj;
		}
		catch (ClassCastException e) {
			return false;
		}

		if (this.size != other.size) {
			return false;
		}

		// if (USE_INT_ARRAY) {
		// for (int i = 0; i < this.intSize; i++) {
		// if (this.intValue[i] != other.intValue[i])
		// return false;
		// }
		// }
		// else {
		for (int i = 0; i < this.size; i++) {
			if (this.value[i] != other.value[i])
				return false;
		}
		// }

		return true;
	}

	public int hashCode() {
		return this.hashCode;
	}

	/**
	 * Returns the String representation of this ID instance.
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.size; i++) {
			int b = 0xff & this.value[i];
			if (b < 16)
				sb.append("0");
			sb.append(Integer.toHexString(b));
		}

		return sb.toString();
	}

	//
	// for object serialization
	//

	/**
	 * A public constructor with no argument required to implement
	 * Externalizable interface.
	 */
	public ID() {
	}

	public void writeExternal(java.io.ObjectOutput out) throws java.io.IOException {
		out.writeByte(this.size);
		out.write(this.value, 0, this.size);
	}

	public void readExternal(java.io.ObjectInput in) throws java.io.IOException, ClassNotFoundException {
		this.size = in.readByte();
		this.value = new byte[this.size];
		in.read(this.value, 0, this.size);

		this.init();
	}

	public int compareTo(ID other) {
		return this.toBigInteger().compareTo(other.toBigInteger());
	}

	public ID pregetID(int n) {
		return pregetID(this.toBigInteger(), this.size);
	}

	public static ID pregetID(BigInteger id, int size) {
		// 与えられたID(biginteger)から任意の数だけ前(とりあえず45°)のIDを返す
		BigInteger id2;
		id2 = id.subtract(BigInteger.ONE.shiftLeft(size * 8 - 2));// 全体の1/4に値するビットを引く
		if (id2.compareTo(BigInteger.ZERO) < 0) {// もし負の値なら2^160ビット足す
			id2 = id2.add(BigInteger.ONE.shiftLeft(size * 8));
		}
		byte[] value2 = id.toByteArray();
		byte[] value = id2.toByteArray();
		return getID(value, size);
	}

	// 実験用
	public static ID getIDforMessagingAddress(MessagingAddress addr, int size) {
		String filename = addr.getHostAddress() + ":" + addr.getPort();

		/*
		 * switch(addr.getPort()){
		 * 
		 * case 3997: return getID("0000000000000000000000000000000000000000",
		 * size); case 3998: return
		 * getID("1000000000000000000000000000000000000000", size); case 3999:
		 * return getID("2000000000000000000000000000000000000000", size); case
		 * 4000: return getID("3000000000000000000000000000000000000000", size);
		 * case 4001: return getID("4000000000000000000000000000000000000000",
		 * size); case 4002: return
		 * getID("5000000000000000000000000000000000000000", size); case 4003:
		 * return getID("6000000000000000000000000000000000000000", size); case
		 * 4004: return getID("7000000000000000000000000000000000000000", size);
		 * case 4005: return getID("8000000000000000000000000000000000000000",
		 * size); case 4006: return
		 * getID("9000000000000000000000000000000000000000", size); case 4007:
		 * return getID("a000000000000000000000000000000000000000", size); case
		 * 4008: return getID("b000000000000000000000000000000000000000", size);
		 * case 4009: return getID("c000000000000000000000000000000000000000",
		 * size); case 4010: return
		 * getID("d000000000000000000000000000000000000000", size); case 4011:
		 * return getID("e000000000000000000000000000000000000000", size); case
		 * 4012: return getID("f000000000000000000000000000000000000000", size);
		 * } System.out.println("ID取れない"); return null;
		 */

		/*
		 * if(filename.equals("133.68.254.85:3997")) return
		 * getID("0000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.86:3997")) return
		 * getID("1000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.87:3997")) return
		 * getID("2000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.88:3997")) return
		 * getID("3000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.89:3997")) return
		 * getID("4000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.90:3997")) return
		 * getID("5000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.91:3997")) return
		 * getID("6000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.92:3997")) return
		 * getID("7000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.93:3997")) return
		 * getID("8000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.94:3997")) return
		 * getID("9000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.95:3997")) return
		 * getID("a000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.96:3997")) return
		 * getID("b000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.130:3997")) return
		 * getID("c000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.131:3997")) return
		 * getID("d000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.132:3997")) return
		 * getID("e000000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.133:3997")) return
		 * getID("f000000000000000000000000000000000000000", size);
		 * 
		 * 
		 * if(filename.equals("133.68.254.134:3997")) return
		 * getID("0500000000000000000000000000000000000000", size);//cs66
		 * if(filename.equals("133.68.254.135:3997")) return
		 * getID("1500000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.136:3997")) return
		 * getID("2500000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.137:3997")) return
		 * getID("3500000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.138:3997")) return
		 * getID("4500000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.139:3997")) return
		 * getID("5500000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.140:3997")) return
		 * getID("6500000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.141:3997")) return
		 * getID("7500000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.142:3997")) return
		 * getID("8500000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.143:3997")) return
		 * getID("9500000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.144:3997")) return
		 * getID("a500000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.145:3997")) return
		 * getID("b500000000000000000000000000000000000000", size);
		 * //if(filename.equals("133.68.254.146:3997")) //return
		 * getID("c500000000000000000000000000000000000000", size);cs78不在
		 * if(filename.equals("133.68.254.147:3997")) return
		 * getID("d500000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.148:3997")) return
		 * getID("e500000000000000000000000000000000000000", size);
		 * if(filename.equals("133.68.254.149:3997")) return
		 * getID("f500000000000000000000000000000000000000", size);//cs081
		 * 
		 * 
		 * if(filename.equals("133.68.254.150:3997")) return
		 * getID("c500000000000000000000000000000000000000",
		 * size);//cs082(cs078不在のため)
		 * 
		 * 
		 * 
		 * 
		 * 
		 * System.out.println("ID取れない"); return null;
		 */

		// if(filename.equals("133.68.42.180:3997"))
		// return getID("0000000000000000000000000000000000000000", size);
		if (filename.equals("133.68.42.176:3997"))
			return getID("0000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.182:3997"))
			return getID("1000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.172:3997"))
			return getID("2000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.174:3997"))
			return getID("3000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.175:3997"))
			return getID("4000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.178:3997"))
			return getID("5000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.179:3997"))
			return getID("6000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.181:3997"))
			return getID("7000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.161:3997"))
			return getID("8000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.162:3997"))
			return getID("9000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.163:3997"))
			return getID("a000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.164:3997"))
			return getID("b000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.165:3997"))
			return getID("c000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.166:3997"))
			return getID("d000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.167:3997"))
			return getID("e000000000000000000000000000000000000000", size);

		if (filename.equals("133.68.42.168:3997"))
			return getID("f000000000000000000000000000000000000000", size);

		//System.out.println("ID取れない");

		return null;

	}

	// //////////////////////////// ここから追加部分 2011.09.11
	// //////////////////////////////

	/**
	 * ID群の値を返す
	 */
	public int getIDLevel() {
		if (this.bigInteger.getLowestSetBit() == -1) {
			return 0;
		}
		else {
			int retLevel = ID.bitLength - this.bigInteger.getLowestSetBit();
			return retLevel;
		}
	}

	/**
	 * 16進数の値（文字列）に直したIDを返す
	 * 
	 * @return
	 */
	public String toHexString() {
		String retStr = this.bigInteger.toString(16);
		int packLength = ID.byteLength - this.bigInteger.toString(16).length();
		for (int i = 0; i < packLength; i++) {
			retStr = "0" + retStr;
		}
		return retStr;
	}

	/**
	 * 2進数の値（文字列）に直したIDを返す
	 */
	public String toBinString() {
		String retStr = this.bigInteger.toString(2);
		int packLength = ID.bitLength - this.bigInteger.toString(2).length();
		for (int i = 0; i < packLength; i++) {
			retStr = "0" + retStr;
		}
		return retStr;
	}

	/**
	 * 10進数の値（文字列）に直したIDを返す
	 */
	public String toDecString() {
		return this.bigInteger.toString(10);
	}

	/**
	 * 最新群までのID群に所属するIDをランダムに生成する 最新群が5群の場合は0群から5群までのどれかの群に所属するIDを一つ生成することを意味する
	 * @param level	
	 * @param seed
	 * @return
	 */
	public static ID getLatestLevelID(final int level, long seed) {
		ID retID;

		Random rand = new Random(seed);
		int bitIndex = level % Byte.SIZE;
		int byteIndex = level / Byte.SIZE;
		if (bitIndex != 0)
			byteIndex++;
		byte[] copyed = new byte[ID.byteLength]; // = byte[20]

		if (byteIndex > 0) {
			byte[] randomPart = new byte[byteIndex];
			rand.nextBytes(randomPart);
			byteIndex--;
			if (bitIndex == 1)
				randomPart[byteIndex] &= (byte) 0x80; // & 10000000
			if (bitIndex == 2)
				randomPart[byteIndex] &= (byte) 0xc0; // & 11000000
			if (bitIndex == 3)
				randomPart[byteIndex] &= (byte) 0xe0; // & 11100000
			if (bitIndex == 4)
				randomPart[byteIndex] &= (byte) 0xf0; // & 11110000
			if (bitIndex == 5)
				randomPart[byteIndex] &= (byte) 0xf8; // & 11111000
			if (bitIndex == 6)
				randomPart[byteIndex] &= (byte) 0xfc; // & 11111100
			if (bitIndex == 7)
				randomPart[byteIndex] &= (byte) 0xfe; // & 11111110

			System.arraycopy(randomPart, 0, copyed, 0, byteIndex + 1);

			retID = ID.getID(copyed, 20);
			return retID;
		}
		// else
		return null;
	}

	/**
	 * バックアップノードのIDを作成
	 */
	public static ID getBackupNodeID(ID leftID) {
		int leftLevel = leftID.getIDLevel();
		if (leftLevel == 0) // バックアップノードが存在しない場合はnullを返却
			return null;

		int byteIndex = leftLevel / Byte.SIZE;
		int bitIndex = leftLevel % Byte.SIZE;

		if (bitIndex == 0)
			byteIndex--;

		byte[] backupArray = leftID.getValue();
		if (bitIndex == 1)
			backupArray[byteIndex] += (byte) 0x80; // + 10000000
		if (bitIndex == 2)
			backupArray[byteIndex] += (byte) 0x40; // + 01000000
		if (bitIndex == 3)
			backupArray[byteIndex] += (byte) 0x20; // + 00100000
		if (bitIndex == 4)
			backupArray[byteIndex] += (byte) 0x10; // + 00010000
		if (bitIndex == 5)
			backupArray[byteIndex] += (byte) 0x08; // + 00001000
		if (bitIndex == 6)
			backupArray[byteIndex] += (byte) 0x04; // + 00000100
		if (bitIndex == 7)
			backupArray[byteIndex] += (byte) 0x02; // + 00000010
		if (bitIndex == 0)
			backupArray[byteIndex] += (byte) 0x01; // + 00000001

		if (backupArray[byteIndex] == 0) { // 桁あがりが発生しない状況ならとばす
			byteIndex--;
			for (; byteIndex >= 0; byteIndex--) {
				backupArray[byteIndex] += (byte) 0x01;

				if (backupArray[byteIndex] != 0) // 桁あがりが発生しない状況ならとばす
					break;
			}
		}
		ID backID = ID.getID(backupArray, ID.byteLength);
		return backID;
	}
}
