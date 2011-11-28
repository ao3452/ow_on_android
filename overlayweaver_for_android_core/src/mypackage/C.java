package mypackage;


/**
 * ƿ̾�̿��ǻȤ���������Բ��Ѥ��ѿ����갷�����餹��
 * ���󥹥��󥹲��򤹤�ɬ�פϤʤ���
 * ɬ�פ�Ŭ���ˤ����졣
 *
 * @author nozomu
 *
 */

public class C
{

	/*
	 * ���󥹥��󥹤��餻�ʤ�����private�ǥ��󥹥ȥ饯�������
	 */
	private C(){}

	/*
	 * Overlay Weaver�Ǥϥ�å����������Ƥϥ��ꥢ�饤����������Ȥ��ƤޤȤ����
	 * �ʲ����ѿ����Ѥ��ƥ�å����������Ƥ�������İ�����
	 * 2011/10/01���ߤǤϡ���å������˰ʲ��λͤĤ����Ǥ�ä���ͽ��
	 * ���إå���
	 * 		ƿ̾ϩ���ۻ���ƿ̾ϩ�����������Ѥ����¿�ŰŹ��ܤ��줿�إå�����
	 * ���ܥǥ���
	 * 		ƿ̾�̿��������Ѥ����¿�ŰŹ��ܤ��줿�ܥǥ�����
	 * ���إå��Υϥå�����
	 * 		�Ρ��ɴ֤Ƿ�ϩ�ξ������ã�򤹤뤿��Υϥå�����
	 * ����å������μ���
	 * 		���ۡ��������̿���3����ζ���
	 */

	public static final int MESSAGE_HEADER = 0;
	public static final int MESSAGE_BODY = 1;
	public static final int MESSAGE_PRIMALKEY = 2;
	public static final int MESSAGE_TYPE = 3;
	public static final int MESSAGE_SIZE = 4;

	/*
	 * ƿ̾�̿����Ѥ������å������μ���
	 * �����ۥ�å�����
	 * ��������å�����
	 * ���̿���å�����
	 * ���̿���ˡ�ѹ���å����� //ע�����ɲ� 11/27
	 */
	public static final int TYPE_CONSTRUCTION = 0;
	public static final int TYPE_REPAIR = 1;
	public static final int TYPE_COMMUNICATION = 2;
	public static final int TYPE_COMMUNICATION_CHANGE = 3;//ע�����ɲ� 11/27

	/*
	 * ƿ̾�̿����ޤ���ƿ̾ϩ�����κݤ�����
	 * ������ä���å������˰Ź��ä��뤫�����椹�뤫��ɽ��
	 * �����������
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












