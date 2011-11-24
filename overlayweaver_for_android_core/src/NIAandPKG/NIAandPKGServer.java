package NIAandPKG;

public class NIAandPKGServer
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		//int test = 1;
		NodeIDAllocator NIA = new NodeIDAllocator();
		PrivateKeyGenerator PKG = new PrivateKeyGenerator();
		//test = NIA.reverseBitOrder(test);
		
		//System.out.println(test);
		//System.out.println(Integer.MIN_VALUE);
		
		NIA.start();
		PKG.start();
		//System.out.println("parent thread");
		
		//joinRequest();
	}

}
