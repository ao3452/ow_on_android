package ow.bootstrap;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class BootstrapServer {
	
	private int BS_PORT = 3997;
	

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BootstrapServer bss = new BootstrapServer();
		bss.start();
	}
	
	
	private void start() {
		BootstrapWorker bsw = new BootstrapWorker();
		bsw.start();
	}
	
	
	
	private class BootstrapWorker extends Thread {
		
		String lastHost = null;
		
		public BootstrapWorker() {
			super();
		}
	
		@Override
		public void run() {
			ServerSocket ss; 
			try {
				ss = new ServerSocket(BS_PORT);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			while(true) {
				
				Socket socket = null;
				try {
					System.out.println("BootstrapWorker: accepting...");
					socket = ss.accept();
					System.out.println("BootstrapWorker: accepted!!!!");
					new Thread() {
						Socket sock = null;
						public Thread setSocket(Socket socket) {
							sock = socket;
							return this;
						}
						public void run() {
							try {
								InputStream in = sock.getInputStream();
								ObjectInputStream oin = new ObjectInputStream(in);
								String hostIP = (String)oin.readObject();
								System.out.println("Socket: read!!!!" + hostIP);

								OutputStream out = sock.getOutputStream();
								ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(out));
								oout.writeObject(lastHost);
								System.out.println("Socket: write!!!!" + lastHost);
								oout.flush();
								oout.close();
								
								sock.close();
								sock = null;
								
								lastHost = hostIP;
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								if (sock != null)
									try {
										sock.close();
									} catch (IOException e) {
										/* ignore */
									}
							}
						}
					}.setSocket(socket).start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}
	}
	

}
