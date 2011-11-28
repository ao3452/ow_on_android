package mypackage;


/**
 * 匿名通新で使われるもろもろの不可変な変数を取り扱うくらす。
 * インスタンス化をする必要はない。
 * 必要な適当にいじれ。
 *
 * @author nozomu
 *
 */

public class C
{

	/*
	 * インスタンスを作らせないためprivateでコンストラクタを宣言
	 */
	private C(){}

	/*
	 * Overlay Weaverではメッセージの内容はシリアライズされ配列としてまとめられる
	 * 以下の変数を用いてメッセージの内容を管理、把握する
	 * 2011/10/01現在では、メッセージに以下の四つの要素を加える予定
	 * ・ヘッダ部
	 * 		匿名路構築時、匿名路修復時に利用される多重暗号を施されたヘッダ情報
	 * ・ボディ部
	 * 		匿名通信時に利用される多重暗号を施されたボディ情報
	 * ・ヘッダのハッシュ値
	 * 		ノード間で経路の情報の伝達をするためのハッシュ値
	 * ・メッセージの種類
	 * 		構築、修復、通信の3種類の区別
	 */

	public static final int MESSAGE_HEADER = 0;
	public static final int MESSAGE_BODY = 1;
	public static final int MESSAGE_PRIMALKEY = 2;
	public static final int MESSAGE_TYPE = 3;
	public static final int MESSAGE_SIZE = 4;

	/*
	 * 匿名通信で用いられるメッセージの種類
	 * ・構築メッセージ
	 * ・修復メッセージ
	 * ・通信メッセージ
	 * ・通信方法変更メッセージ //廣瀬が追加 11/27
	 */
	public static final int TYPE_CONSTRUCTION = 0;
	public static final int TYPE_REPAIR = 1;
	public static final int TYPE_COMMUNICATION = 2;
	public static final int TYPE_COMMUNICATION_CHANGE = 3;//廣瀬が追加 11/27

	/*
	 * 匿名通信、または匿名路修復の際に利用
	 * 受け取ったメッセージに暗号を加えるか、復号するかを表す
	 * ただそれだけ
	 */
	public static final int DECRYPT = 0;
	public static final int ENCRYPT = 1;

	/**
	 * this is a directory which has public and private keys.
	 */
	//public static final String KEY_SERVER = "/home/tanaka/workspace2/Bifrost_Original2/bin/KeyServer4/";
	//public static final String KEY_SERVER = "/home/nitech_bifrost1/Bifrost_Original2/bin/KeyServer4/";
	//public static final String KEY_SERVER = "/home/21417/cig17582/Bifrost_Original2/bin/KeyServer4/";
	//public static final String KEY_SERVER = "/home/hirose/Bifrost_Original2/KeyServer4/";
	public static final String KEY_SERVER = "/data/data/ow.android/KeyServer4/";

	public static final String TEST_SERVICE_NAME = "TestService001";
	public static final int SENDER_KEY = 0;
	public static final int RECV_SEND_KEY = 2;
	public static final int SEND_SID = 4;

	//public static final String SCENARIO_DIR = "/home/tanaka/Documents/ScenarioFile/";
	//public static final String SCENARIO_DIR = "/home/nitech_bifrost1/ScenarioFile/";
	//public static final String SCENARIO_DIR = "/home/21417/cig17582/ScenarioFile/";
	public static final String SCENARIO_DIR = "/home/hirose/ScenarioFile/";


	public static final long DEFAULT_TTL = 2;

	public static final int CIPHER = 0;
	public static final int R = 1;

	/**
	 * bytes of string "ALL_CORRECT"
	 */
	public static final byte[] ALL_CORRECT = {0x41 & 0xFF, 0x4C & 0xFF, 0x4C & 0xFF, 0x5F & 0xFF, 0x43 & 0xFF, 0x4F & 0xFF, 0x52 & 0xFF, 0x52 & 0xFF, 0x45 & 0xFF, 0x43 & 0xFF, 0x54 & 0xFF};
	public static final int IBE_CHECK_LEN = 355;

	public static final int DEC_TIME = 0;
	public static final int LOOKUP_TIME = 1;
	public static final int SEND_CLOCK = 2;
	public static final int DEC_I_MIP = 3;
	public static final int LOOKUP_I_MIP = 4;
	public static final int RECV_CLOCK_MIP = 5;
	public static final int DEC_MIP_D = 6;
	public static final int LOOKUP_MIP_D = 7;
	public static final int RECV_CLOCK_D = 8;
	public static final int DEC_D_I = 9;
	public static final int LOOKUP_D_I = 10;
	public static final int RECV_CLOCK_I = 11;
	//public static final int SEND_CLOCK_I = 0;
	//public static final int SEND_CLOCK_MIP = 0;
	public static final int SIZE_OF_EVALTIME = 12;


	public static final int PORT_NIA = 4100;
	public static final int PORT_PKG = 4101;
	//public static final String ADDR_NIA = "localhost";
	//public static final String ADDR_PKG = "localhost";
	public static final String ADDR_NIA = "cs-d50.cs.nitech.ac.jp";
	public static final String ADDR_PKG = "cs-d50.cs.nitech.ac.jp";

	public static final int JOIN_NIA = 100;
	public static final int LEAVE_REPORT_NIA = 200;
	public static final int GET_NODE_PRIVATE_KEY = 100;
	public static final int GET_SERVICE_PRIVATE_KEY = 200;





}












