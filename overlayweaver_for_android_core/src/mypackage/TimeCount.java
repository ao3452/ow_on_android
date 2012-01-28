package mypackage;

public class TimeCount{
	public long startRelayTime;
	public long endRelayTime;
	
	public TimeCount(){
		this.startRelayTime=0;
		this.endRelayTime=0;
	}
	public TimeCount(long startRT,long endRT){
		this.startRelayTime=startRT;
		this.endRelayTime=endRT;
	}
}
