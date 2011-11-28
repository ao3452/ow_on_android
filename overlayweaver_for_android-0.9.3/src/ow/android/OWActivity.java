package ow.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import ow.MessageObject;
import ow.dht.DHT;
import ow.dht.DHTConfiguration;
import ow.dht.DHTConfiguration.commFlag;
import ow.dht.DHTFactory;
import ow.dht.ValueInfo;
import ow.id.ID;
import ow.messaging.util.MessagingUtility;
import ow.routing.RoutingException;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class OWActivity extends Activity {

	private int oldTime;
	private int oldUse;


	private final static short APPLICATION_ID = 0x01;
	private final static short APPLICATION_VERSION = 2;

	private final static int OW_PORT = DHTConfiguration.DEFAULT_CONTACT_PORT;
	private final static String JoinHOST = //"10.192.41.113";
	                                            //"133.68.187.100";//CSE
			                                      "133.68.15.197";


	private DHT<MessageObject> dht = null;
	private DHTConfiguration dhtConfig = null;

    private Handler mainHandler = new MainHandler();
    private Handler mHandler = new Handler();
    private Runnable mUpdateCpu;


	private EditText putKey = null;
	private EditText putValue = null;
	private EditText getKey = null;
	private EditText logView = null;

	private TextView txt1 = null;
	private TextView txt3 = null;
	private TextView txt4 = null;

	private CheckBox checkBox = null;

	private Button putButton = null;
	private Button getButton = null;
	private Button getCpuButton = null;

	private OnClickListener putListerner = new OnClickListener() {

		@Override
		public void onClick(View v) {
			String key = putKey.getText().toString();
			String value = putValue.getText().toString();
			try {
				dht.put(ID.getSHA1BasedID(key.getBytes()), new MessageObject(value));
				logView.append("PUT command : Key=" + key + "  Value=" + value + "\n");
			} catch (Exception e) {
				logView.append("PUT command : Key=" + key + "  Value=" + value + "  ��UT��け����障����\n");

			}
		}
	};
	private OnClickListener getListerner = new OnClickListener() {

		@Override
		public void onClick(View v) {
			String key = getKey.getText().toString();

			Set<ValueInfo<MessageObject>> values = null;
			try {
				values = dht.get(ID.getSHA1BasedID(key.getBytes()));
				for(ValueInfo<MessageObject> mo : values) {
					logView.append("GET command : Key=" + key + "  Value=" + mo.getValue().getFirst() + "\n");
				}
			} catch (RoutingException e) {
				logView.append("GET command : Key=" + key + " ��et��け����障����\n");
			}

		}

	};

	private OnClickListener checkBoxListerner = new OnClickListener() {
		@Override
		public void onClick(View v){

			if(checkBox.isChecked() == true){
				dhtConfig.setCommunicateMethodFlag(commFlag.Permit);
				txt4.setText("��拭�ゅ������ON");
			}else{
				dhtConfig.setCommunicateMethodFlag(commFlag.Reject);
				txt4.setText("��拭�ゅ������OFF");
			}
		}

	};

	private OnClickListener getCpuListerner = new OnClickListener() {

		@Override
		public void onClick(View v) {

			double dw =cpuUsed();
			txt1.setText(dw+"%");
		}

	};

	private int scale;
	private int level;

	@Override
	protected void onResume() {
		super.onResume();
		//��拭���紮�
		IntentFilter filter=new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(myReceiver,filter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		//��拭���罩�
		unregisterReceiver(myReceiver);
	}

	//��拭罘�
	public BroadcastReceiver myReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
				// �紙�罧�����紊у�
				scale = intent.getIntExtra("scale", 0);
				// �紙�罧��
				level = intent.getIntExtra("level", 0);
			}

			//腟������
			txt3 = (TextView) findViewById(R.id.txt3);
			txt3.setText(((float)level/(float)scale * 100)+" (%)");

		}

	};

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);
    	Window win = getWindow();
    	win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

       putKey = (EditText)findViewById(R.id.put_key_edit_view);
       putValue = (EditText)findViewById(R.id.put_value_edit_view);
       getKey = (EditText)findViewById(R.id.get_key_edit_view);
       logView = (EditText)findViewById(R.id.log_view);

       txt1 = (TextView) findViewById(R.id.txt1);
       txt4 = (TextView) findViewById(R.id.txt4);
       mUpdateCpu = new Runnable() {
    	   public void run() {
    		   float dw =cpuUsed();
    		   txt1.setText(dw+"%");
    		   mHandler.removeCallbacks(mUpdateCpu);
    		   mHandler.postDelayed(mUpdateCpu, 10000);
    	   }
       };
       mHandler.postDelayed(mUpdateCpu, 100);

       checkBox = (CheckBox)findViewById(R.id.checkBox1);

       putButton = (Button)findViewById(R.id.put_button);
       getButton = (Button)findViewById(R.id.get_button);
       getCpuButton = (Button)findViewById(R.id.cpu_button);

       putButton.setOnClickListener(putListerner);
       getButton.setOnClickListener(getListerner);
       getCpuButton.setOnClickListener(getCpuListerner);
       checkBox.setOnClickListener(checkBoxListerner);
    }


	@Override
	protected void onStart() {
		super.onStart();
        if(dht != null) {
        	return;
        }

		try {
			if (mainHandler != null) {
				mainHandler.sendMessage
					(mainHandler.obtainMessage
					 (SHOW_LOADING));
			}
	    	oldTime=(int) System.currentTimeMillis();
	    	oldUse=checkCpuUse();
//	    	logView.append(oldTime+" "+oldUse+"\n");
			// �≪��������
			//InetAddress ipAddr = getLocalAddress();
			//InetAddress ipAddr = getIpAddress();
			InetAddress ipAddr = getWifiAddress();
			Log.d("DHT", ipAddr.getHostAddress());
			//logView.append("1 \n");
			// �潟�����域┃絎��紊����������������������������������������
			dhtConfig = DHTFactory.getDefaultConfiguration();
			dhtConfig.setImplementationName("ChurnTolerantDHT");
			dhtConfig.setMessagingTransport("TCP");
			dhtConfig.setContactPort(OW_PORT);
			dhtConfig.setSelfPortRange(4);
			dhtConfig.setDoUPnPNATTraversal(false);
			dhtConfig.setRoutingAlgorithm("Chord");
			dhtConfig.setRoutingStyle("Iterative");
			dhtConfig.setDirectoryType("VolatileMap");
			dhtConfig.setWorkingDirectory("./hash");
			//logView.append("2 \n");
			MessagingUtility.HostAndPort hostAndPort =
				MessagingUtility.parseHostnameAndPort(ipAddr.getHostAddress(), OW_PORT);
		//			MessagingUtility.parseHostnameAndPort("133.68.15.197", OW_PORT);
		//			MessagingUtility.parseHostnameAndPort("10.192.41.113", OW_PORT);
			//logView.append(ipAddr.getHostAddress()+"3 \n");
			dhtConfig.setSelfAddress(hostAndPort.getHostName());
			//logView.append(hostAndPort.getHostName()+"1 \n");
		//	dhtConfig.setSelfAddress("hakkoudasan.matlab.nitech.ac.jp");
			dhtConfig.setSelfPort(hostAndPort.getPort());
			//logView.append(hostAndPort.getPort()+"4 \n");

			dht = DHTFactory.getDHT(APPLICATION_ID, APPLICATION_VERSION, dhtConfig, null);

			logView.append(ipAddr+"\n");
			InetAddress bsIP = getBootstrapServer(ipAddr);
			if(bsIP == null) {
				logView.append("Only mode... \n"+ ipAddr + "\n" + ipAddr.getHostAddress() + " " +ipAddr.getHostName()+ "\n");
			} else {
				logView.append(bsIP + "\n" + bsIP.getHostAddress() + " " +bsIP.getHostName()+ "\n");
				dht.joinOverlay(bsIP.getHostAddress(), OW_PORT);
				logView.append("end\n");
			}

		} catch (Exception e) {
			logView.append("Don't join DHT network... now local mode... \n");
		} finally {
			if (mainHandler != null) {
				mainHandler.sendMessage
					(mainHandler.obtainMessage
					 (HIDE_LOADING));
			}
		}
	}





	private static final int DIALOG_LOADING = 0;
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_LOADING: {
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage("Loading ...");
			// dialog.setIndeterminate(true);
			dialog.setCancelable(false);
			dialog.getWindow().setFlags
				(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
				 WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			return dialog;
		}
		default:
			return super.onCreateDialog(id);
		}
	}


	public static final int SHOW_LOADING = 5;
	public static final int HIDE_LOADING = 6;
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
    			case SHOW_LOADING: {
    				showDialog(DIALOG_LOADING);
    				break;
    			}
    			case HIDE_LOADING: {
    				try {
    					dismissDialog(DIALOG_LOADING);
    					removeDialog(DIALOG_LOADING);
    				} catch (IllegalArgumentException e) {
    				}
    				break;
    			}
            }
        }
    }

    private InetAddress getBootstrapServer(InetAddress hostIP) {
		logView.append("aaa \n");
    	//InetSocketAddress iSock = new InetSocketAddress(JoinHOST, OW_PORT);
		Socket socket = null;
		InputStream in = null;
		OutputStream out = null;
		try {
			//socket = new Socket();
			socket = new Socket(JoinHOST,OW_PORT);
			//socket.connect(iSock, 3000);
			out = socket.getOutputStream();
			ObjectOutputStream oout = new ObjectOutputStream(out);
			oout.writeObject(hostIP.getHostName());
			//oout.writeObject(hostIP.getHostAddress());
			//oout.writeObject("hakkoudasan.matlab.nitech.ac.jp");
			logView.append("aaa \n");
			in = socket.getInputStream();
		    ObjectInputStream oin = new ObjectInputStream(in);
		    String bsIP = (String)oin.readObject();
		    if(bsIP.equals(hostIP.getHostName())) {
//		    if(bsIP.equals("10.192.41.113")) {
		    //if(bsIP.equals("hakkoudasan.matlab.nitech.ac.jp")) {
		    	return null;
		    }
			logView.append("aaa \n");
		    InetAddress iNetAddr = InetAddress.getByName(bsIP);
		    return iNetAddr;

		} catch (UnknownHostException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (OptionalDataException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} finally {
			try {
				if (socket != null) {
					socket.close();
					socket = null;
				}
			} catch (IOException e) {
				/* ignore */
			}
		}
		return null;

    }
    private InetAddress getIpAddress() {
    	logView.append("if1\n");
        Enumeration<NetworkInterface> netIFs;
        try {
            netIFs = NetworkInterface.getNetworkInterfaces();
            while( netIFs.hasMoreElements() ) {
            	logView.append("if\n");
                NetworkInterface netIF = netIFs.nextElement();
                Enumeration<InetAddress> ipAddrs = netIF.getInetAddresses();
                while( ipAddrs.hasMoreElements() ) {
                    InetAddress ip = ipAddrs.nextElement();
                    if( ! ip.isLoopbackAddress() ) {
                    	logView.append("if1234\n"+ip+"\n");
                    	return ip;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    	logView.append("ifueui\n");
        return getWifiAddress();
    }

    private InetAddress getWifiAddress(){
    	InetAddress ip=null;
    	logView.append("if2\n");
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		String strIPAddress =
				((ipAddress >> 0) & 0xFF) + "." +
						((ipAddress >> 8) & 0xFF) + "." +
						((ipAddress >> 16) & 0xFF) + "." +
						((ipAddress >> 24) & 0xFF);
		logView.append(strIPAddress+"\n");
		if(!(strIPAddress.equals("0.0.0.0"))){
			byte[] byteIPAddress =new byte[] {(byte)((ipAddress >> 0) & 0xFF),(byte)((ipAddress >> 8) & 0xFF),(byte)((ipAddress >> 16) & 0xFF),(byte)((ipAddress >> 24) & 0xFF)};
			try {
				ip = InetAddress.getByAddress(byteIPAddress);
				logView.append(ip+"\n");
			} catch (UnknownHostException e) {
				// TODO �����������catch ������
				e.printStackTrace();
			}
		}
		else{
			byte[] byteIPAddress =new byte[] {(byte)40,(byte)15,(byte)68,(byte)133};
			try {
				ip = InetAddress.getByAddress(byteIPAddress);
				logView.append(ip+"\n");
			} catch (UnknownHostException e) {
				// TODO �����������catch ������
				e.printStackTrace();
			}
		}
    	return ip;
    }

    private InetAddress getLocalAddress() {
		Enumeration enuIfs = null;
		try {
			enuIfs = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		InetAddress ipAddr = null;
		InetAddress tmpIpAddr = null;
		if(null != enuIfs) {
		    while(enuIfs.hasMoreElements()) {
		    	Log.d("DHT", "INTERFECE FOUND");
		        NetworkInterface nic = (NetworkInterface)enuIfs.nextElement();
		        Log.d("DHT", "getDisplayName:\t" + nic.getDisplayName());
		        Log.d("DHT", "getName:\t" + nic.getName());
		        Enumeration enuAddrs = nic.getInetAddresses();
		        while (enuAddrs.hasMoreElements()) {
		            ipAddr = (InetAddress)enuAddrs.nextElement();
		            Log.d("DHT", "getHostAddress:\t" + ipAddr.getHostAddress());
		            if(!ipAddr.getHostAddress().equals("127.0.0.1")) {
		            	if(!ipAddr.getHostAddress().equals("0.0.0.0"))
		            		tmpIpAddr=ipAddr;
		            	break;
		            }
		        }
		    }
		    ipAddr=tmpIpAddr;
		}
		if(ipAddr.getHostAddress().equals("127.0.0.1")){
			if(ipAddr.getHostAddress().equals("0.0.0.0")){
				logView.append("if\n");
				WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
				WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				int ipAddress = wifiInfo.getIpAddress();
				String strIPAddress =
						((ipAddress >> 0) & 0xFF) + "." +
								((ipAddress >> 8) & 0xFF) + "." +
								((ipAddress >> 16) & 0xFF) + "." +
								((ipAddress >> 24) & 0xFF);
				logView.append(strIPAddress+"\n");
				if(!(strIPAddress.equals("0.0.0.0"))){
					byte[] byteIPAddress =new byte[] {(byte)((ipAddress >> 0) & 0xFF),(byte)((ipAddress >> 8) & 0xFF),(byte)((ipAddress >> 16) & 0xFF),(byte)((ipAddress >> 24) & 0xFF)};
					try {
						InetAddress ip = InetAddress.getByAddress(byteIPAddress);
						logView.append(ip+"\n");
						ipAddr=ip;
					} catch (UnknownHostException e) {
						// TODO �����������catch ������
						e.printStackTrace();
					}
				}
			}
		}
		return ipAddr;
	}
    private ProcessBuilder cmd=null;
    //cpu篏睡����茯帥���
    private Integer checkCpuUse(){
    	// �����������輝荐���宴�茵�ず���

    	String [] cmdArgs = {"/system/bin/cat","/proc/stat"};

    	String cpuLine    = "";
    	StringBuffer cpuBuffer    = new StringBuffer();

    	cmd = new ProcessBuilder(cmdArgs);

    	try {
    	Process process = cmd.start();

    	InputStream in  = process.getInputStream();

    	// 腟沿�������1024����������粋昭��
    	// cpu user/nice/system/idle/iowait/irq/softirq/steal/����宴�������

    	byte[] lineBytes = new byte[1024];

    	while(in.read(lineBytes) != -1 ) {

    	cpuBuffer.append(new String(lineBytes));
    	}

    	in.close();

    	}catch (IOException e) {

    	}

    	cpuLine = cpuBuffer.toString();

    	// 1024���������cpu鐔�pu0����с��������遵�
    	int start = cpuLine.indexOf("cpu");
    	int end = cpuLine.indexOf("cpu0");
    	cpuLine = cpuLine.substring(start, end);
    	Log.i("CPU_VALUES_LINE",cpuLine);

    	String[] cpuUse = cpuLine.split(" ");

    	Integer usr = new Integer(cpuUse[2]);

    	Integer nice = new Integer(cpuUse[3]);

    	Integer sys= new Integer(cpuUse[4]);

    	return usr+nice+sys;

    }

    private float cpuUsed(){
      	 int nowTime=(int) System.currentTimeMillis();
      	 int nowUse=checkCpuUse();
      // CPU������膊��
     	float wd = ( (float)( nowUse - oldUse ) /(float) (( nowTime - oldTime )/10));
     	oldTime=nowTime;
     	oldUse=nowUse;
     	return wd;
    }


}