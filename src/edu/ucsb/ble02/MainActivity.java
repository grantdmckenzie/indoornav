package edu.ucsb.ble02;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.JSONArray;
import org.json.JSONException;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.RangeNotifier;
import com.radiusnetworks.ibeacon.Region;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.view.Display;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class MainActivity extends Activity implements IBeaconConsumer,TextureView.SurfaceTextureListener {
	private TextureView mTextureView;
    private MainActivity.RenderingThread mThread;
    protected static final String TAG = "RangingActivity";
    private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);
    private static int userx_prev;
    private static int usery_prev;
    private static float multiple = 3.8f;	// EDIT
    double localmult = 0; //multiple;   // EDIT
    private static int[] userloc;
    //private static int[][] ble_locs = {{(int)(4*multiple),0},{0, 3*multiple},{(int)(4*multiple),6*multiple},{0, 9*multiple},{(int)(4*multiple),12*multiple},{0, 15*multiple},{4*multiple, 18*multiple},{0, 21*multiple},{4*multiple, 24*multiple}};
    private static JSONArray ibeacon_locations;
    private static float[] ble_rad = new float[24];
    private static ArrayList<ble> ble_rad_prev = new ArrayList<ble>();
    private static Bitmap floorplan;
    public OutputStreamWriter myOutWriter;
    public FileOutputStream fOut;
    private int prevx;
    private int prevy;
    private ble[] bleclusters;
    private String participantID;
    // private static int[][] ble_locs = {{4*multiple,0},{0,0},{2*multiple,(int)(4*multiple)}};

    @SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.v("iBeacon", "onCreate");
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        /*View decorView = getWindow().getDecorView();
	     int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
	                   | View.SYSTEM_UI_FLAG_FULLSCREEN;
	     decorView.setSystemUiVisibility(uiOptions); */
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            	participantID = extras.getString("pid");
				// setContentView(R.layout.main);
		        iBeaconManager.bind(this);
		        try {
		        	File myFile = new File("/sdcard/IndoorNav/P"+participantID+".txt");
		        	myFile.createNewFile();
				    fOut = new FileOutputStream(myFile);
				    myOutWriter = new OutputStreamWriter(fOut);
			    	
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		        
		        try {
		        	ibeacon_locations = loadJSONFromAsset();
		        	bleclusters = new ble[ibeacon_locations.length()];
		        	for(int i=0;i<ibeacon_locations.length();i++) {
		        		bleclusters[i] = new ble(i, ibeacon_locations.getJSONArray(i).getInt(0),ibeacon_locations.getJSONArray(i).getInt(1));
		        	}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		        
		        //Log.v("iBeacon", "onCreate");
		        FrameLayout content = new FrameLayout(this);
		
		        mTextureView = new TextureView(this);
		        // mTextureView.setOpaque(false);
		        //mTextureView.setBackgroundResource(R.drawable.floorplan_small);
		        // mTextureView.setAlpha(0.5f);
		        mTextureView.setSurfaceTextureListener(this);
		        
		        userloc = new int[]{500,500};
		        Bitmap floorplan1 = BitmapFactory.decodeResource(getResources(), R.drawable.floorplan_small);
		        floorplan = Bitmap.createScaledBitmap(floorplan1, 1200, 1920, true);
		        //mfloorplan = floorplan.copy(Bitmap.Config.ARGB_8888, true);
		        content.addView(mTextureView, new FrameLayout.LayoutParams(width,height, Gravity.TOP));
		        
		        // ADD FINISH BUTTON
		        RelativeLayout rl = new RelativeLayout(this);
		        RelativeLayout.LayoutParams lay = new RelativeLayout.LayoutParams(
		            RelativeLayout.LayoutParams.MATCH_PARENT, 
		            RelativeLayout.LayoutParams.WRAP_CONTENT);
		        lay.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		        Button finishButton = new Button(this);
		        finishButton.setGravity(Gravity.CENTER);
		        finishButton.setText("FINISH");
		        finishButton.setWidth(300);
		        rl.addView(finishButton, lay);
		        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		        content.addView(rl, lp);
		        // END add FINISH button
		        
		        setContentView(content);
		        
		        
		        finishButton.setOnClickListener(new OnClickListener() {
		            public void onClick(View v) {
		            	 
		     	         Intent mapView = new Intent(MainActivity.this, FinishActivity.class);
		     	         mapView.putExtra("pid", participantID);
		     	         startActivity(mapView);
		            }
		        });
        }
    }
    @Override 
    protected void onDestroy() {
        super.onDestroy();
		try {
			myOutWriter.close();
			fOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        iBeaconManager.unBind(this);
    }
    @Override 
    protected void onPause() {
    	super.onPause();
    	try {
			myOutWriter.close();
			fOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	if (iBeaconManager.isBound(this)) iBeaconManager.setBackgroundMode(this, true);    		
    }
    @Override 
    protected void onResume() {
    	super.onResume();
    	if (iBeaconManager.isBound(this)) iBeaconManager.setBackgroundMode(this, false);    		
    }

    @Override
    public void onIBeaconServiceConnect() {
    	Log.v("iBeacon", "onIBeaconServiceConnect");
    	userloc = new int[]{500,500};
        iBeaconManager.setRangeNotifier(new RangeNotifier() {
	        @Override 
	        public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons, Region region) {
	        	//Log.v("iBeacon", "didRangeBeaconsInRegion");
	            if (iBeacons.size() > 0) {
	            	// Log.v("iBeacon", "iBeacon Collection Size: "+iBeacons.size());
	            	multitrilateration(iBeacons);
	            }
	        }
        });

        try {
            iBeaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }
    
    private void multitrilateration(Collection<IBeacon> beacons) {
 
    	for (IBeacon iBeacon : beacons) { 
    		if (iBeacon.getMinor() > 0) {
    				//stats.addValue(iBeacon.getAccuracy());
    				//map.put(iBeacon.getMinor(), new ble(iBeacon.getMinor(), iBeacon.getAccuracy(), iBeacon.getProximity()));
    			if (iBeacon.getMinor() < 5) {
    				bleclusters[0].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() < 10 && iBeacon.getMinor() != 8) {
    				bleclusters[1].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 10 && iBeacon.getMinor() < 14) {
    				bleclusters[2].addDistance((iBeacon.getAccuracy() - 1)+2);
    			} else if (iBeacon.getMinor() >= 14 && iBeacon.getMinor() < 18) {
    				bleclusters[3].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 19 && iBeacon.getMinor() < 23) {
    				bleclusters[4].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 23 && iBeacon.getMinor() < 27) {
    				bleclusters[5].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 27 && iBeacon.getMinor() < 31) {
    				bleclusters[6].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 31 && iBeacon.getMinor() < 35) {
    				bleclusters[7].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 35 && iBeacon.getMinor() < 39) {
    				bleclusters[8].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 39 && iBeacon.getMinor() < 43) {
    				bleclusters[9].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 43 && iBeacon.getMinor() < 47) {
    				bleclusters[10].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 47 && iBeacon.getMinor() < 50) {
    				bleclusters[11].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 51 && iBeacon.getMinor() < 55) {
    				bleclusters[12].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 55 && iBeacon.getMinor() < 59) {
    				bleclusters[13].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 59 && iBeacon.getMinor() < 63) {
    				bleclusters[14].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 63 && iBeacon.getMinor() < 67) {
    				bleclusters[15].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 67 && iBeacon.getMinor() < 71) {
    				bleclusters[16].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 71 && iBeacon.getMinor() < 75) {
    				bleclusters[17].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 75 && iBeacon.getMinor() < 79) {
    				bleclusters[18].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 79 && iBeacon.getMinor() < 83) {
    				bleclusters[19].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 83 && iBeacon.getMinor() < 87) {
    				bleclusters[20].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 87 && iBeacon.getMinor() < 91) {
    				bleclusters[21].addDistance(iBeacon.getAccuracy() - 1);
    			} else if (iBeacon.getMinor() >= 91 && iBeacon.getMinor() < 95) {
    				bleclusters[22].addDistance(iBeacon.getAccuracy() - 1);
    			}
    			// Log.v("iBeacon", "Found: "+iBeacon.getMinor() + ", " + iBeacon.getAccuracy() + ", " + iBeacon.getProximity());
    		}
    	}

    	int minid1 = 0;
    	int minid2 = 0;
    	double mind1 = 4000;
    	double mind2 = 4000;
    	
        for (int i=0;i<bleclusters.length;i++) {
        	if (bleclusters[i].getStableDistance() > 0.0) {
	            if(bleclusters[i].getStableDistance() < mind2 && bleclusters[i].getStableDistance() >= mind1 && bleclusters[i].getStableDistance() != 0.0) {
	                minid2 = bleclusters[i].id;
	                mind2 = bleclusters[i].getStableDistance();
	            }
	            if (bleclusters[i].getStableDistance()<mind1){
	                mind2 = mind1;
	                minid2 = minid1;
	                minid1 = bleclusters[i].id;
	                mind1 = bleclusters[i].getStableDistance();
	            }
	            // Log.v("iBeacon", "BLE Cluster: "+bleclusters[i].id + ", " + bleclusters[i].getStableDistance());
        	}
        }
        Integer[] lowestValues = {minid1, minid2};
		Log.v("iBeacon", "MIN IDS: " + Arrays.toString(lowestValues));
		Log.v("iBeacon", "MIN VALS: " + mind1 + ", " + mind2);

    	if (!Arrays.asList(lowestValues).contains(2147483647)) {
		    	
		    	int locx = 550;
		    	int locy = 550;
		    	if (Arrays.asList(lowestValues).contains(0) && Arrays.asList(lowestValues).contains(1)) {
		    		double diff = bleclusters[0].getStableDistance() / (bleclusters[0].getStableDistance() + bleclusters[1].getStableDistance()) * 170;
			    	locx = bleclusters[0].x;
			    	locy = bleclusters[0].y - (int) diff;
		    	} else if (Arrays.asList(lowestValues).contains(0) && Arrays.asList(lowestValues).contains(2)) {
		    		double diff = bleclusters[0].getStableDistance() / (bleclusters[0].getStableDistance() + bleclusters[2].getStableDistance()) * 170;
			    	locy = bleclusters[0].y+5;
			    	locx = bleclusters[0].x - (int) diff;
		    	} else if (Arrays.asList(lowestValues).contains(0) && Arrays.asList(lowestValues).contains(3)) {
		    		double diff = bleclusters[0].getStableDistance() / (bleclusters[0].getStableDistance() + bleclusters[3].getStableDistance()) * 170;
			    	locy = bleclusters[0].y;
			    	locx = bleclusters[0].x + (int) diff;
		    	} else if (Arrays.asList(lowestValues).contains(3) && Arrays.asList(lowestValues).contains(4)) {
		    		double diff = bleclusters[3].getStableDistance() / (bleclusters[3].getStableDistance() + bleclusters[4].getStableDistance()) * 170;
			    	locy = bleclusters[3].y;
			    	locx = bleclusters[3].x + (int) diff;
		    	} else if (Arrays.asList(lowestValues).contains(4) && Arrays.asList(lowestValues).contains(5)) {
		    		double diff = bleclusters[4].getStableDistance() / (bleclusters[4].getStableDistance() + bleclusters[5].getStableDistance()) * 170;
			    	locy = bleclusters[4].y;
			    	locx = bleclusters[4].x + (int) diff;
		    	} else if (Arrays.asList(lowestValues).contains(5) && Arrays.asList(lowestValues).contains(6)) {
		    		double diff = bleclusters[5].getStableDistance() / (bleclusters[5].getStableDistance() + bleclusters[6].getStableDistance()) * 170;
			    	locy = bleclusters[5].y - (int) diff;
			    	locx = bleclusters[5].x - 5;
		    	} else if (Arrays.asList(lowestValues).contains(6) && Arrays.asList(lowestValues).contains(7)) {
		    		double diff = bleclusters[6].getStableDistance() / (bleclusters[6].getStableDistance() + bleclusters[7].getStableDistance()) * 170;
			    	locy = bleclusters[6].y;
			    	locx = bleclusters[6].x - (int) diff;
		    	} else if (Arrays.asList(lowestValues).contains(7) && Arrays.asList(lowestValues).contains(8)) {
		    		double diff = bleclusters[7].getStableDistance() / (bleclusters[7].getStableDistance() + bleclusters[8].getStableDistance()) * 170;
			    	locy = bleclusters[7].y;
			    	locx = bleclusters[7].x - (int) diff;
		    	} else if (Arrays.asList(lowestValues).contains(8) && Arrays.asList(lowestValues).contains(1)) {
		    		double diff = bleclusters[8].getStableDistance() / (bleclusters[8].getStableDistance() + bleclusters[1].getStableDistance()) * 170;
			    	locy = bleclusters[8].y;
			    	locx = bleclusters[8].x - (int) diff;
		    	} else if (Arrays.asList(lowestValues).contains(9) && Arrays.asList(lowestValues).contains(1)) {
		    		double diff = bleclusters[9].getStableDistance() / (bleclusters[9].getStableDistance() + bleclusters[1].getStableDistance()) * 170;
			    	locy = bleclusters[9].y;
			    	locx = bleclusters[9].x + (int) diff;
		    	} else if (Arrays.asList(lowestValues).contains(10) && Arrays.asList(lowestValues).contains(9)) {
		    		double diff = bleclusters[10].getStableDistance() / (bleclusters[10].getStableDistance() + bleclusters[9].getStableDistance()) * 170;
			    	locy = bleclusters[10].y;
			    	locx = bleclusters[10].x + (int) diff;
		    	} else if (Arrays.asList(lowestValues).contains(11) && Arrays.asList(lowestValues).contains(10)) {
		    		double diff = bleclusters[11].getStableDistance() / (bleclusters[11].getStableDistance() + bleclusters[10].getStableDistance()) * 170;
			    	locy = bleclusters[11].y- (int) diff;
			    	locx = bleclusters[11].x; 
		    	} else if (Arrays.asList(lowestValues).contains(11) && Arrays.asList(lowestValues).contains(2)) {
		    		double diff = bleclusters[11].getStableDistance() / (bleclusters[11].getStableDistance() + bleclusters[2].getStableDistance()) * 170;
			    	locy = bleclusters[11].y;
			    	locx = bleclusters[11].x + (int) diff;
		    	} else if (Arrays.asList(lowestValues).contains(11) && Arrays.asList(lowestValues).contains(12)) {
		    		double diff = bleclusters[11].getStableDistance() / (bleclusters[11].getStableDistance() + bleclusters[12].getStableDistance()) * 170;
			    	locy = bleclusters[11].y;
			    	locx = bleclusters[11].x - (int) diff;
		    	} else if (Arrays.asList(lowestValues).contains(12) && Arrays.asList(lowestValues).contains(13)) {
		    		double diff = bleclusters[12].getStableDistance() / (bleclusters[12].getStableDistance() + bleclusters[13].getStableDistance()) * 180;
			    	locy = bleclusters[12].y  + (int) diff;
			    	locx = bleclusters[12].x;
		    	} else if (Arrays.asList(lowestValues).contains(14) && Arrays.asList(lowestValues).contains(13)) {
		    		double diff = bleclusters[13].getStableDistance() / (bleclusters[14].getStableDistance() + bleclusters[13].getStableDistance()) * 180;
			    	locy = bleclusters[13].y  + (int) diff;
			    	locx = bleclusters[13].x;
		    	} else if (Arrays.asList(lowestValues).contains(15) && Arrays.asList(lowestValues).contains(14)) {
		    		double diff = bleclusters[14].getStableDistance() / (bleclusters[14].getStableDistance() + bleclusters[15].getStableDistance()) * 180;
			    	locy = bleclusters[14].y  + (int) diff;
			    	locx = bleclusters[14].x;
		    	} else if (Arrays.asList(lowestValues).contains(16) && Arrays.asList(lowestValues).contains(15)) {
		    		double diff = bleclusters[15].getStableDistance() / (bleclusters[15].getStableDistance() + bleclusters[16].getStableDistance()) * 180;
			    	locy = bleclusters[15].y  + (int) diff;
			    	locx = bleclusters[15].x;
		    	} else if (Arrays.asList(lowestValues).contains(17) && Arrays.asList(lowestValues).contains(16)) {
		    		double diff = bleclusters[16].getStableDistance() / (bleclusters[17].getStableDistance() + bleclusters[16].getStableDistance()) * 180;
			    	locy = bleclusters[16].y  + (int) diff;
			    	locx = bleclusters[16].x;
		    	} else if (Arrays.asList(lowestValues).contains(18) && Arrays.asList(lowestValues).contains(17)) {
		    		double diff = bleclusters[17].getStableDistance() / (bleclusters[18].getStableDistance() + bleclusters[17].getStableDistance()) * 170;
			    	locy = bleclusters[17].y;
			    	locx = bleclusters[17].x  + (int) diff;
		    	} else if (Arrays.asList(lowestValues).contains(19) && Arrays.asList(lowestValues).contains(19)) {
		    		double diff = bleclusters[18].getStableDistance() / (bleclusters[19].getStableDistance() + bleclusters[18].getStableDistance()) * 180;
			    	locy = bleclusters[18].y - (int) diff;
			    	locx = bleclusters[18].x;
		    	} else if (Arrays.asList(lowestValues).contains(20) && Arrays.asList(lowestValues).contains(19)) {
		    		double diff = bleclusters[19].getStableDistance() / (bleclusters[20].getStableDistance() + bleclusters[19].getStableDistance()) * 180;
			    	locy = bleclusters[19].y - (int) diff;
			    	locx = bleclusters[19].x;
		    	} else if (Arrays.asList(lowestValues).contains(21) && Arrays.asList(lowestValues).contains(20)) {
		    		double diff = bleclusters[20].getStableDistance() / (bleclusters[20].getStableDistance() + bleclusters[21].getStableDistance()) * 180;
			    	locy = bleclusters[20].y - (int) diff;
			    	locx = bleclusters[20].x;
		    	} else if (Arrays.asList(lowestValues).contains(22) && Arrays.asList(lowestValues).contains(21)) {
		    		double diff = bleclusters[21].getStableDistance() / (bleclusters[22].getStableDistance() + bleclusters[21].getStableDistance()) * 180;
			    	locy = bleclusters[21].y - (int) diff;
			    	locx = bleclusters[21].x;
		    	} else if (Arrays.asList(lowestValues).contains(22) && Arrays.asList(lowestValues).contains(11)) {
		    		double diff = bleclusters[11].getStableDistance() / (bleclusters[22].getStableDistance() + bleclusters[22].getStableDistance()) * 180;
			    	locy = bleclusters[11].y + (int) diff;
			    	locx = bleclusters[11].x;
		    	} else {
		    		locx = userloc[0];
		    		locy = userloc[1];
		    	}
		    	
		    	// max movement
		    	/* if (locx - prevx > 25) userloc[0] = prevx + 25;
		    	else if (locx - prevx < -25) userloc[0] = prevx - 25;
		    	else userloc[0] = locx;
		    	if (locy - prevy > 25) userloc[1] = prevy + 25;
		    	else if (locy - prevy < -25) userloc[1] = prevy - 25;
		    	else userloc[1] = locy; */
		    	
		    	userloc[0] = locx;
		    	userloc[1] = locy;
		    	Calendar c = Calendar.getInstance(); 
		    	int seconds = c.get(Calendar.SECOND);
		    	try {
					write2File(seconds+","+userloc[0]+","+userloc[1]);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    	prevx = userloc[0];
		    	prevy = userloc[1];
    	}
     }

    
    private int[] forceHallways(int[] userloc) {
    	int[] newuserloc = userloc;
    	
    	// force inside east side of building
    	if (userloc[0] > 1300)
    		newuserloc[0] = 1300;
    	// force inside of west side of building
    	if (userloc[0] < 100)
    		newuserloc[0] = 100;
    	// force inside north side of building
    	if (userloc[1] < 386)
    		newuserloc[1] = 395;
    	
    	// force y axis in south east side of building
    	if (userloc[0] > 285 && userloc[1] > 590)
    		newuserloc[1] = 580;
    	// force y axis in southwest side of building
    	if (userloc[0] <= 285 && userloc[1] > 1800)
    		newuserloc[1] = 1800;
    	
    	// BLOCK 1 Y AXIS
    	if (userloc[0] > 310 && userloc[0] < 606) {
    		if (userloc[1] > 487)
    			newuserloc[1] = 580;
    		if (userloc[1] < 487)
    			newuserloc[1] = 395;
    	}
    	// BLOCK 2 Y AXIS
    	if (userloc[0] > 632 && userloc[0] < 1000) {
    		if (userloc[1] > 487)
    			newuserloc[1] = 580;
    		if (userloc[1] < 487)
    			newuserloc[1] = 395;
    	}
    	// BLOCK 1 X AXIS
    	if (userloc[0] > 606 && userloc[1] < 632) {
    		userloc[0] = 620;
    	}
    	
    	Log.v("iBeacon", "old user location: "+userloc[0] + ", " + userloc[1]);
    	Log.v("iBeacon", "new user location: "+newuserloc[0] + ", " + newuserloc[1]);
    	return newuserloc;
    }
    
    public int[] trilateration(int i1, int j1, int i2, int j2, int i3, int j3, double d1, double d2, double d3) {
    //public int[] trilateration(int xa, int ya, int xb, int yb, int xc, int yc, double ra, double rb, double rc) {public int[] trilateration(int xa, int ya, int xb, int yb, int xc, int yc, double ra, double rb, double rc) {
    	
    /*	double S = (Math.pow(xc, 2.) - Math.pow(xb, 2.) + Math.pow(yc, 2.) - Math.pow(yb, 2.) + Math.pow(rb, 2.) - Math.pow(rc, 2.)) / 2.0;
    	double T = (Math.pow(xa, 2.) - Math.pow(xb, 2.) + Math.pow(ya, 2.) - Math.pow(yb, 2.) + Math.pow(rb, 2.) - Math.pow(ra, 2.)) / 2.0;
    	double y = ((T * (xb - xc)) - (S * (xb - xa))) / (((ya - yb) * (xb - xc)) - ((yc - yb) * (xb - xa)));
    	double x = ((y * (ya - yb)) - T) / (xb - xa);
    	
    	int[] a = {(int)x,(int)y};*/
    	
    	
    	double x = ((((Math.pow(d1,2.0)-Math.pow(d2,2.0)) + (Math.pow(i2,2.0)-Math.pow(i1,2.0)) + (Math.pow(j2,2.0)-Math.pow(j1,2.0)) ) * (2*j3-2*j2) - ( (Math.pow(d2,2.0)-Math.pow(d3,2.0)) + (Math.pow(i3,2.0)-Math.pow(i2,2.0)) + (Math.pow(j3,2.0)-Math.pow(j2,2.0)) ) *(2*j2-2*j1) ) / ( (2*i2-2*i3)*(2*j2-2*j1)-(2*i1-2*i2)*(2*j3-2*j2 ) ));
    	double y = ( (Math.pow(d1,2.0)-Math.pow(d2,2.0)) + (Math.pow(i2,2.0)-Math.pow(i1,2.0)) + (Math.pow(j2,2.0)-Math.pow(j1,2.0)) + x*(2*i1-2*i2)) / (2*j2-2*j1);
    	int[] a = {(int)x, (int)y};
    	return a;
    }
    
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        mThread = new RenderingThread(mTextureView);
        mThread.start();
    	
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mThread != null) mThread.stopRendering();
        try {
			myOutWriter.close();
			fOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Ignored
    }

    private static class RenderingThread extends Thread {
        private final TextureView mSurface;
        private volatile boolean mRunning = true;

        public RenderingThread(TextureView surface) {
            mSurface = surface;
        }
   
        @Override
        public void run() {
            
            Paint paint = new Paint();
            paint.setColor(0xff0066ff);
            Paint ble = new Paint();
            ble.setColor(0xff990000);
            Paint blerad = new Paint();
            blerad.setStyle(Paint.Style.STROKE);
            blerad.setStrokeWidth(1);
            blerad.setColor(0x110066ff);
            
            Paint canvasrec = new Paint();
            // canvasrec.setStyle(Paint.Style.STROKE);
            //canvasrec.setStrokeWidth(10);
            //canvasrec.setColor(0xffffffff);
            

            while (mRunning && !Thread.interrupted()) {
                
                View v = mSurface;
                Bitmap b = Bitmap.createBitmap( v.getLayoutParams().width, v.getLayoutParams().height, Bitmap.Config.ARGB_8888);  
                
                final Canvas canvas = mSurface.lockCanvas(null);
                Canvas c = new Canvas(b);
                
                

                //mSurface.layout(0, 0, v.getLayoutParams().width, v.getLayoutParams().height);
                //mSurface.draw(c);
                
                try {
                	
                    //c.drawColor(0xffffffff);
                	//c.drawRect(0, 0, (int)Math.round(3*multiple), 30*multiple, canvasrec);
                	
                	//canvas.drawColor(0xffffff33);
                	Paint paint2 = new Paint();
                	paint2.setAntiAlias(false);
                	paint2.setFilterBitmap(false);
                	paint2.setDither(false);
                
                	canvas.drawBitmap(floorplan, 0, 0, paint2);
                	
                	// canvas.drawRect(0, 0, (int)Math.round(4*multiple), 26*multiple, canvasrec);
                	
                    // canvas.drawRect(x, y, x + 20.0f, y + 20.0f, paint);
                    

                    for(int i=0;i<ibeacon_locations.length();i++) {
                    	if (ble_rad[i] > 0) {
	                    	Paint p = makeRadGrad(ibeacon_locations.getJSONArray(i).getInt(0),ibeacon_locations.getJSONArray(i).getInt(1),ble_rad[i]);
	                    	//c.drawCircle(ibeacon_locations.getJSONArray(i).getInt(0),ibeacon_locations.getJSONArray(i).getInt(1), ble_rad[i], p);
	                    	// c.drawCircle(ble_locs[i][0], ble_locs[i][1], 20, ble);
	                    	 //canvas.drawCircle(ibeacon_locations.getJSONArray(i).getInt(0),ibeacon_locations.getJSONArray(i).getInt(1), ble_rad[i], p);
                    	}
	                    canvas.drawCircle(ibeacon_locations.getJSONArray(i).getInt(0),ibeacon_locations.getJSONArray(i).getInt(1), 5, ble); // still show iBeacon location even if no radius
                        
                    }
                    
                   
                    
                   /* if ((userloc[0]) < 600 && userloc[0] > 0 && userloc[1] < 450) {
                    	userloc[1] = 395;
                    } */
                    //Log.v("iBeacon", "user location draw: "+userloc[0] + ", " + userloc[1]);
                    canvas.drawCircle(userloc[0], userloc[1], 15, paint);
                    userx_prev = userloc[0];
                    usery_prev = userloc[1];
                } catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
                	
                    mSurface.unlockCanvasAndPost(canvas);
                }

                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    // Interrupted
                }
            }
        }
        private Paint makeRadGrad(int x, int y, float radius) {
        	double r = (double) radius/400;
        	int col = 0;
        	if (radius > 400) {
        		r = 1;
        	}
        	col = (int) (255*(1-r));
        	//Log.v("iBeacon", ""+col);
        	
            /* RadialGradient gradient = new RadialGradient(x, y, radius, Color.argb(col, 0, 0, 0), 0x00000000, android.graphics.Shader.TileMode.CLAMP);
            Paint p = new Paint();
            p.setDither(true);
            p.setShader(gradient); */
        	Paint p = new Paint();
        	p.setColor(Color.argb(col, 0, 0, 0));
            return p;
        }
        
        
        void stopRendering() {
            interrupt();
            mRunning = false;
        }
    }
    public JSONArray loadJSONFromAsset() throws JSONException {
        String json = null;
        try {
        	
            InputStream is = getAssets().open("beacon_locations.json");

            int size = is.available();
            
            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            // return null;
        }
        JSONArray jArray = new JSONArray(json);
        // Log.v("iBeacon", "Size: " + jArray.length());
    
        return jArray;
    }
    private void write2File(String data) throws IOException {
    	myOutWriter.append(data+"\n");
    }
}
