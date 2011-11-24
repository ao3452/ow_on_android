package mypackage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import edu.cityu.ibe.IBECipher;
import edu.cityu.ibe.IBEDecrypt;
import edu.cityu.ibe.IBEEncrypt;
import edu.cityu.util.Point;

/**
 * �Ź�˴ؤ���᥽�åɤ���������륯�饹 �ҤȤޤ���static�ʥ᥽�åɤΤߤ��ݻ��������󥹥��󥹤������Ϥ��ʤ�
 * �ʥ��󥹥ȥ饯����private����������
 * 
 * @author nozomu
 * 
 */

public class CipherTools {

	/*
	 * ���󥹥��󥹤��������ʤ��褦���󥹥ȥ饯����private���������
	 */
	private CipherTools() {}

	// ///////////////////////////////////////////////////////////
	// ����˴ط�������ʬ
	// ///////////////////////////////////////////////////////////

	/*
	 * �ºݤ�����򤪤��ʤ��ؿ� ���顼�ΤȤ��ˤϤɤ�����٤��� �ҤȤޤ������顼���ˤ�null���֤��Ƥ���
	 */
	static private byte[] decrypt(SecretKey secKey, String alg, byte[] contents) throws Exception {
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(alg);
			cipher.init(Cipher.DECRYPT_MODE, secKey);
			byte[] ret = cipher.doFinal(contents);
			return ret;
		}
		catch(Exception e){
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * �������ΥХ�������������������̩����Ȥä����椹�� �ѥǥ��󥰤Ϥʤ�
	 * 
	 * @param contents
	 *            �Ź沽���줿�Х�����
	 * @param secKey
	 *            ����˻Ȥ���̩��
	 * @return ���椵�줿��ΥХ�����
	 * @throws Exception 
	 */
	static public byte[] decryptDataNoPadding(byte[] contents, SecretKey secKey) throws Exception {
		byte[] ret = decrypt(secKey, "AES/ECB/NoPadding", contents);
		return ret;
	}

	/**
	 * �������ΥХ�������������������̩����Ȥä����椹�� �ѥǥ��󥰤Ϥ���
	 * 
	 * @param contents
	 *            �Ź沽���줿�Х�����
	 * @param secKey
	 *            ����˻Ȥ���̩��
	 * @return ���沽���줿��ΥХ�����
	 * @throws Exception 
	 */
	static public byte[] decryptDataPadding(byte[] contents, SecretKey secKey) throws Exception {
		byte[] ret = decrypt(secKey, "AES", contents);
		return ret;
	}

	// ////////////////////////////////////////////////////////////////////////
	// �Ź沽�˴ط�������ʬ
	// ////////////////////////////////////////////////////////////////////////
	static private byte[] encrypt(SecretKey secKey, String alg, byte[] contents) {
		try {
			Cipher cipher = Cipher.getInstance(alg);
			cipher.init(Cipher.ENCRYPT_MODE, secKey);
			byte[] ret = cipher.doFinal(contents);
			return ret;
		}
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * ��������byte����������������̩�����Ѥ��ưŹ�򤫤��� ���κݥѥǥ��󥰤��ʤ�
	 * 
	 * @param contents
	 * @param secKey
	 * @return
	 */
	static public byte[] encryptDataNoPadding(byte[] contents, SecretKey secKey) {
		byte[] ret = encrypt(secKey, "AES/ECB/NoPadding", contents);
		return ret;
	}

	/**
	 * ��������byte����������������̩�����Ѥ��ưŹ�򤫤��� ���κݥѥǥ��󥰤�Ԥ�
	 * 
	 * @param contents
	 * @param secKey
	 * @return
	 */
	static public byte[] encryptDataPadding(byte[] contents, SecretKey secKey) {
		//System.out.println("encrypt with key : " + secKey);
		byte[] ret = encrypt(secKey, "AES", contents);
		return ret;
	}

	// ////////////////////////////////////////////////////////////////////////
	// IBE�˴ؤ�����ʬ
	// ////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * @throws Exception 
	 * 
	 */
	static public byte[] decryptByIBEPadding(byte[] contents, Point privateKey) throws Exception {
		byte[] ret = null;
		try {
			byte[] unpadData = MyUtility.PKCS5UnPadding(contents);
			Object obj = MyUtility.bytes2Object(unpadData);

			if (!(obj instanceof IBECipher))
				return null;
			IBECipher ibeCipher = (IBECipher) obj;

			byte[] dec = IBEDecrypt.decrypt(ibeCipher, privateKey);
			//if (dec == null)
			//	System.out.println("IBE null");

			// �������鲼������äƤ�Τ����äѤ�ʬ�����
			byte[] check = Arrays.copyOf(dec, C.ALL_CORRECT.length);
			if(Arrays.equals(check, C.ALL_CORRECT) == false)
			{
				//System.out.println("decryption by IBE is fault @ " + System.currentTimeMillis());
				throw new InvalidKeyException();
			}
			else
			{
				//System.out.println("decryption by IBE is success");
				ret = Arrays.copyOfRange(dec, C.ALL_CORRECT.length, dec.length);
			}

			ret = Arrays.copyOfRange(dec, C.ALL_CORRECT.length, dec.length);
		}
		catch(Exception e){
			e.printStackTrace();
			throw e;
		}
		return ret;
	}

	/**
	 * �������ΥХ���������������ʸ������Ѥ��ưŹ沽�򤪤��ʤ�
	 * @param contents
	 * @param str
	 * @param masterKey
	 * @return
	 * @throws Exception 
	 */
	static public byte[] encryptByIBEPadding(byte[] contents, String str, Point masterKey) throws Exception {
		try {
		byte[] ret = null;
		ByteBuffer byteBuffer = ByteBuffer.allocate(contents.length + C.ALL_CORRECT.length);
		byteBuffer.put(C.ALL_CORRECT);
		byteBuffer.put(contents);
		byte[] plain = byteBuffer.array();
		
		//System.out.println("encrypt by String : " + str);
		IBECipher cipher = IBEEncrypt.encrypt(str, plain, masterKey);
		byte[] enc;
			enc = MyUtility.object2Bytes(cipher);

		ret = MyUtility.PKCS5Padding(enc);
		return ret;
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
	}

}
