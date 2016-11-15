package cs.tcd.ie;


/**
 * @author Dylan Lewis
 * @author Stefan Weber
 * Receives data from client class
 */
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import tcdIO.Terminal;

public class Server extends Node {
	static final int DEFAULT_PORT = 50001;
	static final int HEADER_LENGTH = 2;
	static final int NUM_OF_FRAMES = 2;
	private int receivedFrame;
	private int frameToReceive;
	Terminal terminal;
	private byte[] totalData;
	private boolean lastFrame;
	
	/*
	 * Initialises server class and starts thread
	 */
	Server(Terminal terminal, int port) {
		try {
			this.terminal= terminal;
			socket= new DatagramSocket(port);
			listener.go();
			receivedFrame = 0;
			frameToReceive = 0;
			lastFrame = false;
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/**
	 * Assume that incoming packets contain a String and print the string.
	 */
	public void onReceipt(DatagramPacket packet) {
		try {
			byte [] receivedData;
			receivedData= packet.getData();
			if(receivedData[0] == 1){
				
				byte [] temp;
						
				receivedFrame = (receivedData[1] >> 4);
				
				
				if (frameToReceive == receivedFrame) {
					if(totalData == null){
						temp = new byte[receivedData.length];
						System.arraycopy(receivedData, 2, temp, 0, receivedData.length-2);
					}
					else{
						temp = new byte[totalData.length + receivedData.length -2];
						System.arraycopy(totalData, 0, temp, 0, totalData.length);
						System.arraycopy(receivedData, 2, temp, totalData.length, receivedData.length-2);
					}
					
					totalData = temp;	
					
					DatagramPacket response;
//create a string from the incoming stream;					
					String outString= new String(totalData);
//and display it					
					terminal.println("Received Packet :"+outString);
					byte[] respond;
					byte[] data = (new String("ACK " + ((frameToReceive +1)%NUM_OF_FRAMES ) + "\n")).getBytes();
					respond = new byte[HEADER_LENGTH + data.length];
					respond[0] = 0;
					if((receivedData[1] & 8) == 8){
						respond[1] += 8;
						lastFrame = true;
					}
					respond[1] += (byte) (frameToReceive )% 4;
					frameToReceive = (frameToReceive + 1)% NUM_OF_FRAMES;
					respond[1] += (byte) (frameToReceive << 4);
					
					
					System.arraycopy(data, 0, respond, 2, data.length);
					response = new DatagramPacket(respond, respond.length);
					response.setSocketAddress(packet.getSocketAddress());
					socket.send(response);
					if(lastFrame){
						FileOutputStream stream = new FileOutputStream(("Result.txt"));
						try {
						    stream.write(totalData);
						} finally {
						    stream.close();
						}
						
					}
				}
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
		
		System.out.println("In here");
		try {					
			Terminal terminal= new Terminal("Server");
			(new Server(terminal, DEFAULT_PORT)).start();
			terminal.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}