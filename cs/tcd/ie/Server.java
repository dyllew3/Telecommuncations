package cs.tcd.ie;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Timer;

import tcdIO.Terminal;

public class Server extends Node {
	static final int DEFAULT_PORT = 50001;
	static final int HEADER_LENGTH = 2;
	private int receivedFrame;
	private int frameToReceive;
	Terminal terminal;
	private byte[] receivedData;
	
	/*
	 * 
	 */
	Server(Terminal terminal, int port) {
		try {
			this.terminal= terminal;
			socket= new DatagramSocket(port);
			listener.go();
			receivedFrame = 0;
			frameToReceive = 0;
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/**
	 * Assume that incoming packets contain a String and print the string.
	 */
	public void onReceipt(DatagramPacket packet) {
		try {
			receivedData= packet.getData();
			if(receivedData[0] == 1){
				
				byte [] stringData = new byte[receivedData.length-2];
				System.arraycopy(receivedData, 2, stringData, 0, receivedData.length-2);
				String content = new String(stringData);
				receivedFrame = (receivedData[1] >> 4);
				
				
				if(frameToReceive == receivedFrame){
					frameToReceive = 1 - frameToReceive;
					DatagramPacket response;
					terminal.println(content);
					byte[] respond;
					byte[] data = (new String("ACK " + frameToReceive + "\n")).getBytes();
					respond = new byte[HEADER_LENGTH + data.length];
					respond[0] = 0;
					respond[1] += (byte) (frameToReceive << 4);
					respond[1] += (byte) (1 -frameToReceive );
					System.arraycopy(data, 0, respond, 2, data.length);
					response = new DatagramPacket(respond, respond.length);
					response.setSocketAddress(packet.getSocketAddress());
					socket.send(response);
				}
				
				
			}
			else{
				
			}
			
			
		}
		catch(Exception e) {e.printStackTrace();}
	}

	
	public synchronized void start() throws Exception {
		terminal.println("Waiting for contact");
		this.wait();
	}
	
	/*
	 * 
	 */
	public static void main(String[] args) {
		try {					
			Terminal terminal= new Terminal("Server");
			(new Server(terminal, DEFAULT_PORT)).start();
			terminal.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}