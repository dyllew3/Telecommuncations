package cs.tcd.ie;

/**
 * 
 * @author Dylan
 * Selective Repeat class gets the window size, the
 * position of a frame in the window and checks is a window
 * is full or if all the acks have been received 
 * 
 */

class SelectiveRepeat {
	
	
	public static int getWindowSize(int numberOfFrames){
		
	 int pow = (int)((Math.log10(numberOfFrames -1)/Math.log10(2)));
	 int result = (int)Math.pow(2, (pow));
	 return (result);
		
	}
	
	public static int windowPos(int[] window, int frame ){
		for(int i = 0; i < window.length; i++){
			if(window[i] == frame){
				return i;
			}
		}
		
		return -1;
	}
	
	public static void setToEmpty(int [] array){
		for(int i = 0; i < array.length; i++){
			array[i] = -1;
		}
	}
	public static boolean isFull(int [] array){
		for(int i = 0; i <array.length; i++){
			if(array[i] == -1)return false;
		}
		return true;
	}
	public static boolean isFull(boolean [] array){
		for(int i = 0;i < array.length; i++){
			if(!array[i]){
				return false;
			}
		}
		
		return true;
	}
	
	
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
