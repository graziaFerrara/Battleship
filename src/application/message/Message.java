package application.message;

import java.net.InetAddress;

public class Message {
	
	private String text;
	private int port = -1;
	private InetAddress address = null;
	
	public Message(String text) {
		this.text = text;
	}
	
	public Message(String text, int port, InetAddress address) {
		this(text);
		this.port = port;
		this.address = address;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}
	
}
