package cs.tcd.ie;

public class GoBack {
	
	static final long RESET_TIME = 10000;
	private int startFrame;
	private int endFrame;
	private boolean [] acksRecieved;
	private int windowSize;
	private byte[] frame;
	private long now;
	
	public GoBack(int windowLength){
		
		this.windowSize = windowLength;
		acksRecieved = new boolean[windowLength];
		startFrame = 0;
		endFrame = windowSize - 1;
		
	}
	
	
	
	public boolean received(int ack ){
		acksRecieved[(ack -1) % windowSize] = true;
		if((ack - 1)% this.windowSize == startFrame){
		
			startFrame = (startFrame + 1)% windowSize;
			now  = System.currentTimeMillis();
			slide();
			return true;
		}
		else {
			if(System.currentTimeMillis() - now > RESET_TIME ){
				reset();
				now = System.currentTimeMillis();
				return false;
			}
			else{
				return true;
			}
		}
		
				
	}
	
	public void reset(){
		int current = endFrame;
		while(current != startFrame){
			acksRecieved[current] = false;
			current = (current -1)% windowSize;
		}
		
	}
	
	public void slide(){
		while(acksRecieved[startFrame]){
			acksRecieved[startFrame] = false;
			startFrame = (startFrame + 1)% windowSize;
			endFrame = (endFrame + 1)% windowSize;
		}
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
