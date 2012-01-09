package com.adakoda.android.spinnersample;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
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
import ow.id.IDAddressPair;
import ow.messaging.util.MessagingUtility;
import ow.routing.RoutingException;
import ow.routing.RoutingHop;
import ow.routing.RoutingResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class OverlayWeaver_on_androidActivity extends Activity 
   implements OnItemSelectedListener{
	
	private final static short APPLICATION_ID = 0x01;
	private final static short APPLICATION_VERSION = 2;

	private final static int OW_PORT = DHTConfiguration.DEFAULT_CONTACT_PORT;
	private final static String JoinHOST = //"10.192.41.113";
	                                            //"133.68.187.100";//CSE
			                                      "133.68.15.197";


	private DHT<MessageObject> dht = null;
	private DHTConfiguration dhtConfig = null;
	
	private commFlag setCommFlag = null;
	private int changeConstructNumber = 0; 
	private ID constructTargetID = null;
	
    private Handler mainHandler = new MainHandler();
    private Handler mHandler = new Handler();
    private Handler constructHandler = new Handler();
    private Handler constructChooseHandler = new Handler();
    private Runnable mUpdateCpu;
    private Runnable mUpdateConstructNumber;
    private Runnable mUpdateConstructChoose;
	
	private ViewFlipper viewflipper;
	
	private Button putFinishButton = null;
	private Button getFinishButton = null;
	private Button statusFinishButton = null;
	private Button relayFinishButton = null;
	private Button constructFinishButton = null;
	private Button putButton = null;
	private Button getButton = null;
	private Button getCpuButton = null;
	private Button statusButton = null;
	private Button relayChangeButton = null;
	private Button constructButton = null;
	
	private RadioButton radioButton = null;
	private RadioGroup radioGroup = null;
	
	private TextView txt1 = null;
	private TextView txt3 = null;
	private TextView txt4 = null;
	private TextView txt5 = null;
	
	private EditText putKey = null;
	private EditText putValue = null;
	private EditText getKey = null;
	private EditText relayChangeNumber = null;
	private EditText logView = null;
	
	private OnClickListener putListerner = new OnClickListener() {

		@Override
		public void onClick(View v) {
			String key = putKey.getText().toString();
			String value = putValue.getText().toString();
			try {
				dht.put(ID.getSHA1BasedID(key.getBytes()), new MessageObject(value));
				logView.append("PUT command : Key=" + key + "  Value=" + value + "\n");
			} catch (Exception e) {
				logView.append("PUT command : Key=" + key + "  Value=" + value + " のPUTに失敗しました。\n");
			}
			viewflipper.setDisplayedChild(0);
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
				logView.append("GET command : Key=" + key + " のGETに失敗しました。\n");
			}
			viewflipper.setDisplayedChild(0);
		}

	};
	private OnClickListener finishListerner = new OnClickListener() {
		@Override
		public void onClick(View v) {
			viewflipper.setDisplayedChild(0);
		}
	};
	private OnClickListener statusListerner = new OnClickListener() {

		@Override
		public void onClick(View v) {
			getStatus();
			viewflipper.setDisplayedChild(0);
		}
	};
	private OnClickListener relayChangeListerner = new OnClickListener() {

		@Override
		public void onClick(View v) {
			changeConstructNumber = Integer.parseInt(relayChangeNumber.getText().toString());
			dhtConfig.setCommunicateMethodFlag(setCommFlag, changeConstructNumber);
			viewflipper.setDisplayedChild(0);
		}
	};
	private OnClickListener constructListerner = new OnClickListener() {

		@Override
		public void onClick(View v) {
			dht.construct(constructTargetID, 2);
			viewflipper.setDisplayedChild(0);
		}
	};
	private OnItemSelectedListener setConstructChooseListerner = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			// TODO 自動生成されたメソッド・スタブ
			constructTargetID=ID.getID(arg0.getSelectedItem().toString(), 20);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO 自動生成されたメソッド・スタブ
			
		}
	};
	
	private OnClickListener getCpuListerner = new OnClickListener() {
		@Override
		public void onClick(View v) {

			//double dw =cpuUsed();
			float dw = readUsage();
			txt1.setText(dw+"%");
		}
	};
	
	//バッテリー量の取得用変数
	private int scale;
	private int level;

	@Override
	protected void onResume() {
		super.onResume();
		
		//バッテリー情報の受信開始
		IntentFilter filter=new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(myReceiver,filter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		 //バッテリー情報の受信停止
		unregisterReceiver(myReceiver);
	}

	//バッテリ情報を受信するブロードキャストレシーバー
	public BroadcastReceiver myReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
				// バッテリー量の取得
				scale = intent.getIntExtra("scale", 0);
				level = intent.getIntExtra("level", 0);
			}

			//バッテリー量の表示用
			txt3 = (TextView) findViewById(R.id.txt3);
			txt3.setText(((float)level/(float)scale * 100)+" (%)");

		}

	};
	

	private Spinner stateSpinner;
	private Spinner constructSpinner;
	private String[] constructStates;
	private String[] states;
	private ArrayAdapter<String> adapter;
	private ArrayAdapter<String> adapterConstruct;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
    	Window win = getWindow();
    	win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	
	    viewflipper = (ViewFlipper)this.findViewById(R.id.flipper);
	    
	    stateSpinner = (Spinner)findViewById(R.id.spinner);
	    constructSpinner = (Spinner)findViewById(R.id.construct_spinner);
	    
	    putFinishButton = (Button)findViewById(R.id.put_finish);
	    getFinishButton = (Button)findViewById(R.id.get_finish);
	    statusFinishButton = (Button)findViewById(R.id.status_finish);
	    relayFinishButton = (Button)findViewById(R.id.relay_finish);
	    constructFinishButton = (Button)findViewById(R.id.construct_finish);
	    putFinishButton.setOnClickListener(finishListerner);
	    getFinishButton.setOnClickListener(finishListerner);
	    statusFinishButton.setOnClickListener(finishListerner);
	    relayFinishButton.setOnClickListener(finishListerner);
	    constructFinishButton.setOnClickListener(finishListerner);
	    
	    states = getResources().getStringArray(R.array.command_state);

	    adapter = new ArrayAdapter<String>(
    			this,
    			android.R.layout.simple_spinner_item,
    			states);
    	adapter.setDropDownViewResource(
    			android.R.layout.simple_spinner_dropdown_item);
    	
    	putKey = (EditText)findViewById(R.id.put_key_edit_view);
        putValue = (EditText)findViewById(R.id.put_value_edit_view);
        getKey = (EditText)findViewById(R.id.get_key_edit_view);
        relayChangeNumber = (EditText)findViewById(R.id.get_changeConstructNumber);
        logView = (EditText)findViewById(R.id.log_view);
        
        txt1 = (TextView) findViewById(R.id.txt1);
        txt4 = (TextView) findViewById(R.id.txt4);
        //CPU自動更新用
        mUpdateCpu = new Runnable() {
     	   public void run() {
     		//   float dw =cpuUsed();
     		   float dw = readUsage();
     		   txt1.setText(dw+"%");
     		   mHandler.removeCallbacks(mUpdateCpu);
     		   mHandler.postDelayed(mUpdateCpu, 10000);
     	   }
        };
        mHandler.postDelayed(mUpdateCpu, 1000);
        txt5 = (TextView) findViewById(R.id.txt5);
        //匿名路構築数自動更新用
        mUpdateConstructNumber = new Runnable() {
        	public void run() {
        		int constructNumber = dhtConfig.getConstructNumber();
        		txt5.setText(constructNumber + "個");
        		constructHandler.removeCallbacks(mUpdateConstructNumber);
        		constructHandler.postDelayed(mUpdateConstructNumber, 3500);
        	}
        };
        constructHandler.postDelayed(mUpdateConstructNumber, 3000);
        
        //表示用Fingertable自動更新用
        mUpdateConstructChoose = new Runnable() {
        	public void run() {
        		IDAddressPair[] tmpIDAddressPair= dht.getSuccessorlist();
        		int tmpIDlength = 0;
        		try{
        			tmpIDlength= tmpIDAddressPair.length;
        			constructStates = new String[tmpIDlength];
        			for(int i = 0;i<tmpIDlength;i++){
        				constructStates[i]=tmpIDAddressPair[i].getID().toString();
        			}
        			setSpinner(constructSpinner,constructStates);
        		} catch (NullPointerException e){
        			logView.append(tmpIDlength + "\n");
        		}
 
        		constructHandler.removeCallbacks(mUpdateConstructChoose);
        		constructHandler.postDelayed(mUpdateConstructChoose, 10000);
        	}
        };
        constructHandler.postDelayed(mUpdateConstructChoose, 5000);
        
        //中継方法変更用のラジオボタンの設定
        radioGroup = (RadioGroup)findViewById(R.id.radiogroup);
        // 指定した ID のラジオボタンをチェックします
        radioGroup.check(R.id.radiobutton_permit);
        // チェックされているラジオボタンの ID を取得します
        radioButton = (RadioButton)findViewById(radioGroup.getCheckedRadioButtonId());
        // ラジオグループのチェック状態が変更された時に呼び出されるコールバックリスナーを登録します
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            // ラジオグループのチェック状態が変更された時に呼び出されます
            // チェック状態が変更されたラジオボタンのIDが渡されます
            public void onCheckedChanged(RadioGroup group, int checkedId) { 
            	if(group == radioGroup){
            		switch(checkedId){
            		case R.id.radiobutton_permit :
            			setCommFlag = commFlag.Permit;
            			break;
            		case R.id.radiobutton_relay :
            			setCommFlag = commFlag.Relay;
            			break;
            		case R.id.radiobutton_reject :
            			setCommFlag = commFlag.Reject;
            			break;
            		default :
            			break;
            		}
            	}

            }
        });

        putButton = (Button)findViewById(R.id.put_button);
        getButton = (Button)findViewById(R.id.get_button);
        getCpuButton = (Button)findViewById(R.id.cpu_button);
        statusButton = (Button)findViewById(R.id.status_get);
        relayChangeButton = (Button)findViewById(R.id.relay_change);
        constructButton = (Button)findViewById(R.id.construct);
        
        putButton.setOnClickListener(putListerner);
        getButton.setOnClickListener(getListerner);
        getCpuButton.setOnClickListener(getCpuListerner);
        statusButton.setOnClickListener(statusListerner);
        relayChangeButton.setOnClickListener(relayChangeListerner);
        constructButton.setOnClickListener(constructListerner);
        
    	stateSpinner.setAdapter(adapter);
    	stateSpinner.setOnItemSelectedListener(this);
	}
	
	private void setSpinner(Spinner spinner,String[] arr){
		ArrayAdapter<String> adapter1 =
				new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arr);
		adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter1);
		spinner.setOnItemSelectedListener(setConstructChooseListerner);
	}

	  @Override
	  public void onItemSelected(
	    AdapterView<?> arg0, 
	    View arg1, 
	    int arg2,
	    long arg3) {
	    
	    int i = arg0.getSelectedItemPosition();
	    viewflipper.setDisplayedChild(i);


//	    stateSpinner[old].setAdapter(null);
//	    stateSpinner[old].setOnItemSelectedListener(null);
//	    old = i;
//	    stateSpinner[i].setAdapter(adapter);
//	    stateSpinner[i].setOnItemSelectedListener(this);

	    
	  }

	  @Override
	  public void onNothingSelected(AdapterView<?> arg0) {}
	  
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
//			  logView.append(oldTime+" "+oldUse+"\n");
			  // アドレスの取得
			  //InetAddress ipAddr = getLocalAddress();
			  //InetAddress ipAddr = getIpAddress();
			  InetAddress ipAddr = getWifiAddress();
			  Log.d("DHT", ipAddr.getHostAddress());
			  //logView.append("1 \n");
			  // コンフィグ設定。変える場合は、自分でブートストラップを用意してください。
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
			  in = socket.getInputStream();
			  ObjectInputStream oin = new ObjectInputStream(in);
			  String bsIP = (String)oin.readObject();
			  if(bsIP.equals(hostIP.getHostName())) {
//				  if(bsIP.equals("10.192.41.113")) {
				  //if(bsIP.equals("hakkoudasan.matlab.nitech.ac.jp")) {
				  return null;
			  }
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
	  
	  private InetAddress getWifiAddress(){
		  InetAddress ip=null;
		  WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		  WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		  
		  int ipAddress = wifiInfo.getIpAddress();
		  String strIPAddress =
				  ((ipAddress >> 0) & 0xFF) + "." +
						  ((ipAddress >> 8) & 0xFF) + "." +
						  ((ipAddress >> 16) & 0xFF) + "." +
						  ((ipAddress >> 24) & 0xFF);
		  if(!(strIPAddress.equals("0.0.0.0"))){
			  byte[] byteIPAddress =new byte[] {(byte)((ipAddress >> 0) & 0xFF),(byte)((ipAddress >> 8) & 0xFF),(byte)((ipAddress >> 16) & 0xFF),(byte)((ipAddress >> 24) & 0xFF)};
			  try {
				  ip = InetAddress.getByAddress(byteIPAddress);
				  logView.append(ip+"\n");
			  } catch (UnknownHostException e) {
				  // TODO 自動生成されたcatchブロック
				  e.printStackTrace();
			  }
		  }
		  else{
			  byte[] byteIPAddress =new byte[] {(byte)40,(byte)15,(byte)68,(byte)133};
			  try {
				  ip = InetAddress.getByAddress(byteIPAddress);
				  logView.append(ip+"\n");
			  } catch (UnknownHostException e) {
				  // TODO 自動生成されたcatchブロック
				  e.printStackTrace();
			  }
		  }
		  return ip;
	  }
	  
	  
	  private float readUsage() {
		  try {
			  RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
			  String load = reader.readLine();
			  String[] toks = load.split(" ");
			  long idle1 = Long.parseLong(toks[5]);
			  long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
					  + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
			  try {
				  Thread.sleep(360);
			  } catch (Exception e) {}
			  reader.seek(0);
			  load = reader.readLine();
			  reader.close();
			  toks = load.split(" ");
			  long idle2 = Long.parseLong(toks[5]);
			  long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
					  + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
			  return (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));
		  } catch (IOException ex) {
			  ex.printStackTrace();
		  }
		  return 0;
	  }
	  
	  private void getStatus(){
		  // show self address
		  logView.append("ID and address: " + dht.getSelfIDAddressPair() + "\n");
		  
		  // show routing table
		  logView.append("Routing table:" + "\n");
		  String routingTableString = dht.getRoutingTableString().replaceAll("\n", "\n");
		  logView.append(routingTableString + "\n");
		  
		  // show last key and route
		  logView.append("Last key: ");
		  logView.append(dht.getLastKeyString() + "\n");
		  
		  RoutingResult routingRes = dht.getLastRoutingResult();
		  if (routingRes != null) {
			  long timeBase = -1L;
			  
			  logView.append("Last route: ");
			  logView.append("[");
			  for (RoutingHop hop: routingRes.getRoute()) {
				  if (timeBase < 0L) {
					  timeBase = hop.getTime();
				  }
				  
				  logView.append("\n" + " ");
				  logView.append(hop.getIDAddressPair() + " (");
				  long time = hop.getTime() - timeBase;
				  logView.append(time + ")");
			  }
			  logView.append("\n" + "]" + "\n");
			  
			  logView.append("Last root candidates: ");
			  logView.append("[");
			  for (IDAddressPair r: routingRes.getRootCandidates()) {
				  logView.append("\n");
				  logView.append(" " + r);
			  }
			  logView.append("\n" + "]" + "\n");
		  }
		  
	  }
}