package mypackage;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import edu.cityu.util.Point;

import ow.id.ID;

public class AnonymousRouteInfo {
	/*
	 * 受信者の情報を保持する変数
	 */
	private LocalRouteInfo receiverInfo = null;

	/*
	 * 中継ノードの情報を保持する変数配列
	 */
	private ArrayList<LocalRouteInfo> relayInfo = null;

	/*
	 * 多重暗号で用いる共通鍵を保持したリスト ArrayListの意味はない（なんとなくだ。もっといい形があるかも知れない）
	 */
	private ArrayList<SecretKey> keysForAnonymity;

	/*
	 * 隣接する送信先ノード（本質的な送信先では無い＝中継ノード）へ送る主キーのバイト列
	 */
	private byte[] primaryKey = null;

	// 暗号化の方式（要らないかもしれん）
	public static final String DES_ALGORITHM = "DES";
	public static final String DESEDE_ALGORITHM = "DESede";
	public static final String AES_ALGORITHM = "AES";
	public static final String DEFAULT_ALGORITHM = AES_ALGORITHM; // デフォルト

	/**
	 * なにもしないコンストラクタ 本来の使いかたでなく、意図的なヘッダの作成のために利用する（＝テスト、デバッグ用）
	 */
	public AnonymousRouteInfo() {
	}

	/**
	 * 匿名路情報を作成する 具体的には受信（目的）ノードと、最新群の値、中継ノードの数を取得し 入力値に応じた経路の情報を蓄える
	 * 中継ノードは最新群以下の群に所属するノードの中からランダムに選択する
	 * 
	 * @param targetID
	 *            目的とする送信ノードのID
	 * @oaran latestIDLevel 自身が知っている最新群のレベル
	 * @param relayCount
	 *            指定する中継ノードの個数
	 * 
	 * @throws NoSuchAlgorithmException
	 */
	public AnonymousRouteInfo(final ID targetID, final int latestIDLevel, final int relayCount) {
		try {
			this.keysForAnonymity = new ArrayList<SecretKey>();
			this.relayInfo = new ArrayList<LocalRouteInfo>();

			KeyGenerator kg;
			kg = KeyGenerator.getInstance(DEFAULT_ALGORITHM);
			kg.init(128);
			SecretKey skey;

			/* 目的ノードの情報を保存 */
			skey = kg.generateKey();
			this.receiverInfo = LocalRouteInfo.generateInfoAsReceiver(targetID, skey);
			this.keysForAnonymity.add(0, skey);

			/* 中継ノードの情報を保存 */
			long seed = System.currentTimeMillis();
			ID nextID = targetID;

			for (int i = 0; i < relayCount; i++) {
				skey = kg.generateKey();

				// System.currentTimeMillisだとseedの値が変化しないのでこうした
				seed += 10;
				ID relayID = ID.getLatestLevelID(latestIDLevel, seed);

				// 同じノードが連続することを回避
				if (relayID.equals(nextID)) {
					i--;
					continue;
				}
				nextID = relayID;

				this.relayInfo.add(LocalRouteInfo.generateInfoAsRelay(relayID, skey));
				this.keysForAnonymity.add(i, skey);
			}
		}
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 特め色情報を作成する。 具体的には受信（目的）ノードと、中継ノードのIDを取得し保存する。 主にデバッグ用の関数である
	 * 
	 * @param targetID
	 *            目的とする送信ノードのID
	 * @param relayID
	 *            中継ノードのID
	 */
	public AnonymousRouteInfo(final ID targetID, final ID[] relayID) {
		try {
			this.keysForAnonymity = new ArrayList<SecretKey>();
			this.relayInfo = new ArrayList<LocalRouteInfo>();
			
			KeyGenerator kg;
			kg = KeyGenerator.getInstance(DEFAULT_ALGORITHM);
			kg.init(128);
			SecretKey skey;

			/* 目的ノードの情報を保存 */
			skey = kg.generateKey();
			this.receiverInfo = LocalRouteInfo.generateInfoAsReceiver(targetID, skey);
			this.keysForAnonymity.add(0, skey);

			/* 中継ノードの情報を保存 */
			for (int i = 0; i < relayID.length; i++) {
				skey = kg.generateKey();
				ID id = relayID[i];
				this.relayInfo.add(LocalRouteInfo.generateInfoAsRelay(id, skey));
				this.keysForAnonymity.add(i, skey);
			}
		}
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * このクラスが保持する情報を表現する文字列をかえす 具体的には ・送信者に近い中継ノードのID、共通鍵、かバックアップノードか否か
	 * ・中継ノードのID、共通鍵、中継ノードだという主張 を順に表示する（はず）
	 * 
	 * まぁ確認用ですよ。
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("relay node info \n");
		for (LocalRouteInfo lrci : this.relayInfo) {
			sb.append(lrci.toString());
			sb.append("\n");
		}
		sb.append("receiver node info \n");
		sb.append(this.receiverInfo.toString());
		return sb.toString();
	}

	/**
	 * このクラスが保持する情報のID部分のみを表示
	 * 
	 * @return
	 */
	public String toPrintRoute() {
		StringBuffer sb = new StringBuffer();
		sb.append("relay node ID \n");
		for (LocalRouteInfo lrci : this.relayInfo) {
			sb.append(lrci.toStringID());
			sb.append("\n");
		}
		sb.append("receiver node ID \n");
		sb.append(this.receiverInfo.toStringID());
		return sb.toString();
	}

	/**
	 * 匿名路の全体像を表現する文字列を表示 送信者に近い中継ノードからはじまり、受信ノードまでの経路を表示する （いまの所は）
	 */
	public void printRouteInfo() {
		//System.out.println("匿名路情報表示");
		//System.out.println(this.toString() + "\n"); // 最後に改行をいれたかった
	}

	/**
	 * このクラスが保持している情報から匿名路構築情報（修復ではない）を作成する
	 * 
	 * @return
	 */
	public AnonymousMessage getConstructMessage(Point masterKey) {
		try {
			// 受信者が受け取る情報を作成
			SecretKey secKey = this.receiverInfo.getSharedKey();
			ID nextID = null;
			byte[] nextHeader = null;
			AnonymousHeader header = new AnonymousHeader(nextID, nextHeader, secKey);

			// 受信者が復号できるよう、受信者のIDで暗号化
			byte[] byteHeader = MyUtility.object2Bytes(header);
			ID receiverID = this.receiverInfo.getNodeID();
			nextHeader = CipherTools.encryptByIBEPadding(byteHeader, receiverID.toString(), masterKey);

			// 中継者が受け取る情報を作成
			nextID = receiverID;
			for (int index = this.relayInfo.size() - 1; index >= 0; index--) {
				LocalRouteInfo localInfo = relayInfo.get(index);
				secKey = localInfo.getSharedKey();

				// 中継者が受け取るヘッダ情報を作成
				header = new AnonymousHeader(nextID, nextHeader, secKey);
				byteHeader = MyUtility.object2Bytes(header);

				// 中継者が復号できる形で暗号化
				nextID = localInfo.getNodeID();
				nextHeader = CipherTools.encryptByIBEPadding(byteHeader, nextID.toString(), masterKey);
			}
			AnonymousMessage ret = new AnonymousMessage();
			ret.setHeader(nextHeader);

			// 主キーを保存
			this.primaryKey = nextHeader;

			return ret;
		}
		// ユーザの操作でエラーが起きることは無いからここでエラーを止めるべき？
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	// 通常はランダムで中継ノードを生成するがテストでは扱いにくいので
	// 指定した中継のノードを設定する
	// setReceiverInfoとsetRelayNodeInfoはそのためのクラス
	/**
	 * テストのためだけに存在 多重暗号化されたヘッダ部を受け取り設定する 他のクラスでヘッダを作る必要がある（このクラスで作れるように変更すべきかも？）
	 * 
	 * @param receiverInfo
	 */
	public void setReceiverInfo(LocalRouteInfo receiverInfo) {
		this.receiverInfo = receiverInfo;
	}

	/**
	 * テストのためだけに存在
	 * 
	 * @param relayInfo
	 */
	public void setRelayNodeInfo(ArrayList<LocalRouteInfo> relayInfo) {
		this.relayInfo = relayInfo;
	}

	/**
	 * 多段中継における"直接"の送信先のIDを取得するための関数
	 * 
	 * @return
	 */
	public ID getLocalTargetID() {
		ID localTargetID = this.relayInfo.get(0).getNodeID();
		return localTargetID;
	}

	/**
	 * 多段中継における"実際"の送信先のIDを取得するための関数
	 * 
	 * @return
	 */
	public ID getGlobalTargetID() {
		ID globalTargetID = this.receiverInfo.getNodeID();
		return globalTargetID;
	}

	/**
	 * 匿名路で利用されるキーの集合を取得するための関数 index=0から送信者に近いノードとの共有鍵であり
	 * index=size-1が受信者との共有鍵となる
	 * 
	 * @return
	 */
	public ArrayList<SecretKey> getKeyList() {
		return this.keysForAnonymity;
	}

	/**
	 * 次ノードに送信する主キーを取得するための関数
	 * 
	 * @return
	 */
	public byte[] getPrimaryKey() {
		return this.primaryKey;
	}
}
