package cs.tcd.ie;

/**
 * @author Dylan Lewis
 * based off server class
 */

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.Paths;
import java.util.Timer;

import tcdIO.Terminal;

public class SelecRepServer extends Node {
	static final int DEFAULT_PORT = 50001;
	static final int HEADER_LENGTH = 2;
	static final int NUM_OF_FRAMES = 16;
	static final int DATA_LENGTH = 1250;
	private int receivedFrame;
	private int frameToReceive;
	Terminal terminal;
	private byte[] totalData;
	private byte [] insert;
	private boolean lastFrame;
	private int [] window; 
	private boolean [] acks; 
	private boolean nack;
	private int count;
	/*
	 * 
	 */
	SelecRepServer(Terminal terminal, int port) {
		try {
			this.terminal = terminal;
			socket = new DatagramSocket(port);
			listener.go();
			count = 0;
			receivedFrame = 0;
			frameToReceive = 0;
			nack = false;
			lastFrame = false;
			window = new int[SelectiveRepeat.getWindowSize(NUM_OF_FRAMES)];
			getWindow();
			acks = new boolean[SelectiveRepeat.getWindowSize(NUM_OF_FRAMES)];
			
			
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Assume that incoming packets contain a String and print the string.
	 */
	public synchronized  void onReceipt(DatagramPacket packet) {
		try {
			byte[] receivedData;
			packet.getAddress();
			receivedData = packet.getData();
			int address = receivedData[0] >> 4;
			if (address == 1) {

				byte[] temp;
				
				
				
				receivedFrame = (receivedData[1] >> 4);
				//accounts for 2s complements
				if(receivedFrame < 0)receivedFrame = NUM_OF_FRAMES + receivedFrame;
				int windowPos = SelectiveRepeat.windowPos(window, receivedFrame);
				//accounts for reset
				if(receivedFrame == window[0]){
					
					reset();
					frameToReceive = receivedFrame;
					
				}
				//checks if valid frame
				if(receivedFrame == frameToReceive){
					//checks if code is valid
					if((receivedData[0] & 0b1100) == 0b1000){
						
						
						//create byte array to be stored in totalData
						//accounts for nacks
						if (insert == null) {
							temp = new byte[receivedData.length];
							System.arraycopy(receivedData, 2, temp, 0, receivedData.length - 2);
						}
						else {
							temp = new byte[insert.length + receivedData.length - 2];
							System.arraycopy(insert, 0, temp, 0, insert.length);
							System.arraycopy(receivedData, 2, temp, (DATA_LENGTH * windowPos), receivedData.length - 2);
						}

						insert = temp;

						DatagramPacket response;
						terminal.println("Received Packet");
						byte[] respond;
						byte[] data = (new String("ACK " + ((frameToReceive + 1) % NUM_OF_FRAMES) + "\n")).getBytes();
						respond = new byte[HEADER_LENGTH + data.length];
						respond[0] = 0b1000;
						if ((receivedData[1] & 8) == 8) {
							
							boolean [] newAck = new boolean[(windowPos + 1)];
							
							int index = 0;
							while(index < newAck.length){
								
								newAck[index] = acks[index];
								index++;
							}
							acks = newAck;
							respond[1] += 8;
							lastFrame = true;
						}
						
						//Inserts data into total data array
						frameToReceive = (frameToReceive + 1) % NUM_OF_FRAMES;
						acks[windowPos] = true;
						if(SelectiveRepeat.isFull(acks)){
							getWindow();
							if(totalData == null){
								totalData = insert;
							}
							else{
								byte [] newTemp = new byte[totalData.length + insert.length];
								System.arraycopy(totalData, 0, newTemp, 0, totalData.length);
								System.arraycopy(insert, 0, newTemp, totalData.length, insert.length);
							}
							
							
							reset();
						}
						respond[1] += (byte) (frameToReceive << 4);
						
						System.arraycopy(data, 0, respond, 2, data.length);
						response = new DatagramPacket(respond, respond.length);
						response.setSocketAddress(packet.getSocketAddress());
						socket.send(response);
						if (lastFrame) {
							
							FileOutputStream stream = new FileOutputStream(("Result.txt"));
							try {
								stream.write(totalData);
							} finally {
								stream.close();
								socket.close();
							}

						}
						
					}
					//program started
									
					else {
						terminal.println("Invalid code");
					}
					
				}
				else if(!nack) {
						nack = true;
						DatagramPacket response;
						terminal.println("Received out of order Packet");
						if (insert == null) {
							temp = new byte[receivedData.length];
							System.arraycopy(receivedData, 2, temp, 0, receivedData.length - 2);
						}
						else {
							temp = new byte[insert.length + receivedData.length - 2];
							System.arraycopy(insert, 0, temp, 0, insert.length);
							System.arraycopy(receivedData, 2, temp, (DATA_LENGTH * windowPos), receivedData.length - 2);
						}

						insert = temp;
						byte[] respond;
						byte[] data = (new String("NACK " + ((frameToReceive)) + "\n")).getBytes();
						respond = new byte[HEADER_LENGTH + data.length];
						respond[0] = 0b1011;
						respond[1] += (byte) (frameToReceive << 4);
						System.arraycopy(data, 0, respond, 2, data.length);
						response = new DatagramPacket(respond, respond.length);
						response.setSocketAddress(packet.getSocketAddress());
						socket.send(response);
						
						
					
					
				}
			}
			else {
				terminal.println("Invalid address");
			}

		} catch (Exception e) {
			e.printStackTrace();
			
		}
	}
	
	public void getWindow(){
		int frame = frameToReceive;
		for(int i = 0 ; i < window.length; i++){
			window[i] = frame;
			frame = (frame + 1)%NUM_OF_FRAMES;
			
		}
		
	}
	public void reset(){
		for(int i = 0; i< acks.length; i++){
			acks[i] = false;
		}
		
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
			Terminal terminal = new Terminal("Server");
			(new SelecRepServer(terminal, DEFAULT_PORT)).start();
			terminal.println("Program completed");
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}
}