package ow;

import java.io.Serializable;
import java.util.LinkedList;

public class MessageObject extends LinkedList<Object> implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8377081823983156567L;

	public MessageObject(Object obj) {
		this.add(obj);
	}

}
