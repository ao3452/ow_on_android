package mypackage;

import java.io.Serializable;

import ow.id.ID;
import ow.id.IDAddressPair;

/**
 * ���Υ��饹��ƿ̾�̿���Ԥ��ݤ����Ѥ�����å�������ɽ���ޤ���
 * ƿ̾�̿��Ǥϰʲ��μ�³����Ф��̿���Ԥ��ޤ�
 * ��ƿ̾ϩ�ι���
 * ��ƿ̾ϩ�ν���
 * ��ƿ̾���Τ����̿�
 * ����Ū�ˤϾ廰�Ĥ����Ƥ򤳤Υ��饹�Ǽ���������
 * 
 * �ޤ��ޤ���¤��Ǻ����
 * 
 * @author nozomu
 *
 */

public class AnonymousMessage implements Serializable{

	
	private static final long serialVersionUID = 1L;
	
	private IDAddressPair dest;
	private IDAddressPair src;
	private byte[] constructInfo;
	private byte[] body;
	private byte[] primeKey;
	
	public AnonymousMessage(){}
	
	public AnonymousMessage getConstructMessage(ID nextID, byte[] header){
		AnonymousMessage am = new AnonymousMessage();
		return am;
	}
	
	/**
	 * ���Υ�å������Υإå������ꤹ��
	 * @param nextID
	 */
	public void setHeader(byte[] header){
		this.constructInfo = header;
	}
	
	/**
	 * ���Υ�å������Υإå����֤��ʤ��δؿ����פ�ʤ�����͡���
	 * @return
	 */
	public byte[] getHeader(){
		return this.constructInfo;
	}
	
	/**
	 * ���Υ�å��������������Ƥ����ꤹ��
	 * @param body
	 */
	public void setBody(byte[] body){
		this.body = body;
	}
	
	/**
	 * ���Υ�å�������body���֤�
	 * @return
	 */
	public byte[] getBody(){
		return this.body;
	}
	
	
}
