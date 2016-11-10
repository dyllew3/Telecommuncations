package cs.tcd.ie;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

public class Basket extends TimerTask {
	DatagramSocket socket;
	private Timer timer;
	DatagramPacket packet;
	
	public Basket(DatagramPacket packet, Timer time) {
		timer = time;
		this.packet = packet;
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void resend() {
		try {
			socket.send(this.packet);
			timer.cancel();
			System.out.println("Resent");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		resend();
		return;
	}

	
	
	
	

}
