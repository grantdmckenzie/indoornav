package edu.ucsb.ble02;

import java.util.Arrays;

public class ble {
	private int[] ids;
	public int id;
	private double[] accuracy;
	public int x;
	public int y;
	private int counter;
	private double prevmed;
	public ble(int id, int x, int y){
		this.id = id;
		this.accuracy = new double[4];
		this.ids = new int[4];
		this.x = x;
		this.y = y;
		this.counter = 0;
		this.prevmed = 0.0;
	}
	public void updateDistances(double[] accuracy) {
		this.counter = 0;
		this.accuracy = accuracy;
		
	}
	public void addDistance(double accuracy) {
		
		if (this.counter == 0)
			this.prevmed = this.getStableDistance();
		
		this.accuracy[this.counter] = accuracy;
		this.counter++;
		if (this.counter > 3)
			this.counter = 0;
		
	}
	public double getStableDistance() {
			
	    	Arrays.sort(this.accuracy);
	    	
	    	if (prevmed > 0.0 && prevmed < 600) {
		    	// If all transmitters are 0
		    	if (this.accuracy[0] == 0 && this.accuracy[1] == 0 && this.accuracy[2] == 0 && this.accuracy[3] == 0)
		    		return prevmed;
		    	
		    	// If the first two transmitters are not zero
		    	else if (this.accuracy[0] != 0.0 && this.accuracy[1] != 00)
		    		return ((this.accuracy[0] + this.accuracy[1]) / 2 * 0.75) + (prevmed * 0.25);
		    	
		    	// If the first transmitter is zero
		    	else if (this.accuracy[0] == 0.0 && this.accuracy[1] != 0.0 && this.accuracy[2] != 0.0)
		    		return ((this.accuracy[1] + this.accuracy[2]) / 2 * 0.75) + (prevmed * 0.25);
		    	
		    	// If the first two transmitters are zero
		    	else if (this.accuracy[0] == 0.0 && this.accuracy[1] == 0.0 && this.accuracy[2] != 0.0 && this.accuracy[3] != 0.0)
		    		return ((this.accuracy[2] + this.accuracy[3]) / 2 * 0.75) + (prevmed * 0.25);
		    	
		    	// If the first three transmitters are zero
		    	else 
		    		return (this.accuracy[3] * 0.75) + (prevmed + 0.25);
	    	} else {
	    		if (this.accuracy[0] == 0 && this.accuracy[1] == 0 && this.accuracy[2] == 0 && this.accuracy[3] == 0)
		    		return 0.0;
		    	
		    	// If the first two transmitters are not zero
		    	else if (this.accuracy[0] != 0.0 && this.accuracy[1] != 00)
		    		return ((this.accuracy[0] + this.accuracy[1]) / 2);
		    	
		    	// If the first transmitter is zero
		    	else if (this.accuracy[0] == 0.0 && this.accuracy[1] != 0.0 && this.accuracy[2] != 0.0)
		    		return ((this.accuracy[1] + this.accuracy[2]) / 2);
		    	
		    	// If the first two transmitters are zero
		    	else if (this.accuracy[0] == 0.0 && this.accuracy[1] == 0.0 && this.accuracy[2] != 0.0 && this.accuracy[3] != 0.0)
		    		return ((this.accuracy[2] + this.accuracy[3]) / 2);
		    	
		    	// If the first three transmitters are zero
		    	else 
		    		return (this.accuracy[3]);
	    		
	    	}
	}
}
