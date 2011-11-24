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
 * 暗号に関するメソッドを一括管理するクラス ひとまずはstaticなメソッドのみを保持し、インスタンスの生成はしない
 * （コンストラクタはprivateで宣言する）
 * 
 * @author nozomu
 * 
 */

public class CipherTools {

	/*
	 * インスタンスを生成しないようコンストラクタをprivateで宣言する
	 */
	private CipherTools() {}

	// ///////////////////////////////////////////////////////////
	// 復号に関係する部分
	// ///////////////////////////////////////////////////////////

	/*
	 * 実際に復号をおこなう関数 エラーのときにはどうするべき？ ひとまず、エラー時にはnullを返している
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
	 * 第一引数のバイト配列を第二引数の秘密鍵を使って復号する パディングはなし
	 * 
	 * @param contents
	 *            暗号化されたバイト列
	 * @param secKey
	 *            復号に使う秘密鍵
	 * @return 復号された後のバイト列
	 * @throws Exception 
	 */
	static public byte[] decryptDataNoPadding(byte[] contents, SecretKey secKey) throws Exception {
		byte[] ret = decrypt(secKey, "AES/ECB/NoPadding", contents);
		return ret;
	}

	/**
	 * 第一引数のバイト配列を第二引数の秘密鍵を使って復号する パディングはあり
	 * 
	 * @param contents
	 *            暗号化されたバイト列
	 * @param secKey
	 *            復号に使う秘密鍵
	 * @return 復号化された後のバイト列
	 * @throws Exception 
	 */
	static public byte[] decryptDataPadding(byte[] contents, SecretKey secKey) throws Exception {
		byte[] ret = decrypt(secKey, "AES", contents);
		return ret;
	}

	// ////////////////////////////////////////////////////////////////////////
	// 暗号化に関係する部分
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
	 * 第一引数のbyte配列に第二引数の秘密鍵を用いて暗号をかける その際パディングしない
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
	 * 第一引数のbyte配列に第二引数の秘密鍵を用いて暗号をかける その際パディングを行う
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
	// IBEに関する部分
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

			// ここから下が何やってるのかさっぱり分からん
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
	 * 第一引数のバイト列に第二引数の文字列を用いて暗号化をおこなう
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
