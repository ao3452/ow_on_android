package ow.messaging;

import java.nio.channels.SocketChannel;

public interface Relayhandler {
	//���ߥ�˥����ȥ�å���������ä��餽�Υ�å������˽�����졼
	//���θ塤�����Υ����åȤ��֤�
	SocketChannel process(Message msg,SocketChannel sock) throws Exception;
	
	
}
