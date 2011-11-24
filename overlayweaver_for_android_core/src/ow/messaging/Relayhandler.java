package ow.messaging;

import java.nio.channels.SocketChannel;

public interface Relayhandler {
	//コミュニケートメッセージをもらったらそのメッセージに従いリレー
	//その後，中継先のソケットを返す
	SocketChannel process(Message msg,SocketChannel sock) throws Exception;
	
	
}
