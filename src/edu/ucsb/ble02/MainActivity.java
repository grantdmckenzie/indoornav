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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.RangeNotifier;
import com.radiusnetworks.ibeacon.Region;

import android.app.Activity;
import android.content.Context;
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
import android.widget.FrameLayout;

public class MainActivity extends Activity implements IBeaconConsumer,TextureView.SurfaceTextureListener {
	private TextureView mTextureView;
    private MainActivity.RenderingThread mThread;
    protected static final String TAG = "RangingActivity";
    private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);
    private static int userx_prev;
    private static int usery_prev;
    private static float multiple = 4.0f;	// EDIT
    double localmult = 0; //multiple;   // EDIT
    private static int[] userloc;
    //private static int[][] ble_locs = {{(int)(4*multiple),0},{0, 3*multiple},{(int)(4*multiple),6*multiple},{0, 9*multiple},{(int)(4*multiple),12*multiple},{0, 15*multiple},{4*multiple, 18*multiple},{0, 21*multiple},{4*multiple, 24*multiple}};
    private static JSONArray ibeacon_locations;
    private static float[] ble_rad = new float[21];
    private static ArrayList<ble> ble_rad_prev = new ArrayList<ble>();
    private static Bitmap floorplan;
    public OutputStreamWriter myOutWriter;
    public FileOutputStream fOut;
    // private static int[][] ble_locs = {{4*multiple,0},{0,0},{2*multiple,(int)(4*multiple)}};

    @SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.v("iBeacon", "onCreate");
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View decorView = getWindow().getDecorView();
	     int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
	                   | View.SYSTEM_UI_FLAG_FULLSCREEN;
	     decorView.setSystemUiVisibility(uiOptions);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
		// setContentView(R.layout.main);
        iBeaconManager.bind(this);
        try {
        	File myFile = new File("/sdcard/stats.txt");
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
        setContentView(content);
        
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
        iBeaconManager.setRangeNotifier(new RangeNotifier() {
	        @Override 
	        public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons, Region region) {
	            if (iBeacons.size() > 0) {
	            	Log.v("iBeacon", "iBeacon Collection Size: "+iBeacons.size());
	            	ble_rad_prev = multitrilateration(iBeacons, ble_rad_prev);
	            }
	        }
        });

        try {
            iBeaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }
    
    private ArrayList<ble> multitrilateration(Collection<IBeacon> beacons, ArrayList<ble> ble_rad_prev2) {
    	// write loop for trilateration
    	ArrayList<IBeacon> aBeacons = new ArrayList<IBeacon>();
    	for (IBeacon iBeacon : beacons) { 
    		if (iBeacon.getMinor() > 0) {
    			aBeacons.add(iBeacon);
    		}
    	}
    	Log.v("iBeacon", "ArrayList Size : "+aBeacons.size());
    	userloc[0] = 0;
    	userloc[1] = 0;
    	ArrayList<Integer[]> existing = new ArrayList<Integer[]>();
    	List<Integer> xasdf = new ArrayList<Integer>();
    	List<Integer> yasdf = new ArrayList<Integer>();
    	int count = 0;
    	for(int i=0;i<aBeacons.size();i++) {
    		for(int j=0;j<aBeacons.size();j++) {
    			for(int k=0;k<aBeacons.size();k++) {
        			if (k != j && k != i && j != i) {
        				boolean match = false;
        				for(int g=0;g<existing.size();g++) {
        					if (Arrays.asList(existing.get(g)).contains(i) && Arrays.asList(existing.get(g)).contains(j) && Arrays.asList(existing.get(g)).contains(k)) {
        						match = true;
        					}
        				}
        				if (!match) {
	        				Integer[] d = new Integer[3];
	        				d[0] = i;
	        				d[1] = j;
	        				d[2] = k;
	        				existing.add(d);
	        				double proxa = aBeacons.get(i).getAccuracy()*multiple;
	        		    	double proxb = aBeacons.get(j).getAccuracy()*multiple;
	        		    	double proxc = aBeacons.get(k).getAccuracy()*multiple;
	        		    	for(int z=0;z<ble_rad_prev.size();z++) {
	        		    		if (ble_rad_prev.get(i).id == aBeacons.get(i).getMinor()) {
	        		    			if (ble_rad_prev.get(i).accuracy - aBeacons.get(i).getAccuracy()*multiple > 3)
	        		    				proxa = aBeacons.get(i).getAccuracy()*multiple + 3;
	        		    			else if (ble_rad_prev.get(i).accuracy - aBeacons.get(i).getAccuracy()*multiple < -3)
	        		    				proxa = aBeacons.get(i).getAccuracy()*multiple - 3;
	        		    			else
	        		    				proxa = aBeacons.get(i).getAccuracy()*multiple;
	        		    			
	        		    			ble_rad_prev.get(i).accuracy = proxa;
	        		    			//Log.v("iBeacon", "proxa: "+iBeacon.getAccuracy() + " | " + aBeacons.get(i).getAccuracy() + " | " + proxa);
	        		    		}
	        		    		
	        		    		if (ble_rad_prev.get(j).accuracy == aBeacons.get(j).getMinor()) {
	        		    			if (ble_rad_prev.get(j).accuracy - aBeacons.get(j).getAccuracy()*multiple > 3)
	        		    				proxb = aBeacons.get(j).getAccuracy()*multiple + 3;
	        		    			else if (ble_rad_prev.get(j).accuracy - aBeacons.get(j).getAccuracy()*multiple < -3)
	        		    				proxb = aBeacons.get(j).getAccuracy()*multiple - 3;
	        		    			else
	        		    				proxb = aBeacons.get(j).getAccuracy()*multiple;
	        		    			
	        		    			ble_rad_prev.get(j).accuracy = proxb;
	        		    		}
	        		    		if (ble_rad_prev.get(k).accuracy == aBeacons.get(k).getMinor()) {
	        		    			if (ble_rad_prev.get(k).accuracy - aBeacons.get(k).getAccuracy()*multiple > 3)
	        		    				proxc = aBeacons.get(k).getAccuracy()*multiple + 3;
	        		    			else if (ble_rad_prev.get(k).accuracy - aBeacons.get(k).getAccuracy()*multiple < -3)
	        		    				proxc = aBeacons.get(k).getAccuracy()*multiple - 3;
	        		    			else
	        		    				proxc = aBeacons.get(k).getAccuracy()*multiple;
	        		    			
	        		    			ble_rad_prev.get(k).accuracy = proxc;
	        		    		}
	        		    	}
	        		    	// Log.v("iBeacon","Prox ("+aBeacons.get(i).getMinor()+"): "+proxa);
	        				int[] xy;
							try {
								if (proxa < 30*multiple && proxb < 30*multiple && proxc < 30*multiple) {
									xy = trilateration(
											ibeacon_locations.getJSONArray(aBeacons.get(i).getMinor()-1).getInt(0), 
											ibeacon_locations.getJSONArray(aBeacons.get(i).getMinor()-1).getInt(1),
											ibeacon_locations.getJSONArray(aBeacons.get(j).getMinor()-1).getInt(0),
											ibeacon_locations.getJSONArray(aBeacons.get(j).getMinor()-1).getInt(1),
											ibeacon_locations.getJSONArray(aBeacons.get(k).getMinor()-1).getInt(0),
											ibeacon_locations.getJSONArray(aBeacons.get(k).getMinor()-1).getInt(1),
											proxa,
											proxb,
											proxc
										);
									if (xy[0] > 0 && xy[1] > 0) {
										xasdf.add(xy[0]);
										yasdf.add(xy[1]);
			    						//userloc[0] += xy[0];
			    						//userloc[1] += xy[1];
			    						Log.v("iBeacon", "xy "+aBeacons.get(i).getMinor()+"/"+aBeacons.get(j).getMinor()+"/"+aBeacons.get(k).getMinor()+": "+xy[0] + ", " + xy[1]);
										// Log.v("iBeacon", "x ("+count+"): "+x[count]);
			    						count++;
			    						
			    					}
								}
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
        				}
        			}
        		}
    		}
    	}
    	if (count > 0) {
    		double medianx = 0;
    		Collections.sort(xasdf);
    		int middle = ((xasdf.size()) / 2);
    		if(xasdf.size() % 2 == 0){
    		 int medianA = xasdf.get(middle);
    		 int medianB = xasdf.get(middle-1);
    		 medianx = (medianA + medianB) / 2d;
    		} else{
    		 medianx = xasdf.get(middle);
    		}
    		
    		double mediany = 0;
    		Collections.sort(yasdf);
    		middle = ((yasdf.size()) / 2);
    		if(yasdf.size() % 2 == 0){
    		 int medianA = yasdf.get(middle);
    		 int medianB = yasdf.get(middle-1);
    		 mediany = (medianA + medianB) / 2d;
    		} else{
    		 mediany = yasdf.get(middle);
    		}
	    	userloc[0] = (int) medianx;
	    	userloc[1] = (int) mediany;
    		//userloc[0] = userloc[0] / count;
    		//userloc[1] = userloc[1] / count;
	    	Log.v("iBeacon", "user location ("+count+"): "+userloc[0] + ", " + userloc[1]);
    	}
    	return ble_rad_prev;
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
    
    private int[] getLowestThree(float[] fullarray) {
    	float min1 = 100;
    	float min2 = 100;
    	float min3 = 100;
    	int i1 = 1;
    	int i2 = 1;
    	int i3 = 1;
    	// Log.v("iBeacon","Array Size: "+fullarray.length);
    	for (int i=0; i < fullarray.length; i++) {
    		if (fullarray[i] > 0.0) {
	    		if (fullarray[i] < min3 && fullarray[i] >= min2) {
	    			min3 = fullarray[i];
	    			i3 = i;
	    		}
	    		if (fullarray[i] < min2 && fullarray[i] >= min1) {
	    			min3 = min2;
	    			i3 = i2;
	    			min2 = fullarray[i];
	    			i2 = i;
	    		}
	    		if (fullarray[i] < min1) {
	    			min3 = min2;
	    			i3 = i2;
	    			min2 = min1;
	    			i2 = i1;
	    			min1 = fullarray[i];
	    			i1 = i;
	    		}
    		}
    	}
    	int[] d = {i1,i2,i3};
    	return d;
    }
    
    public int[] trilateration(int xa, int ya, int xb, int yb, int xc, int yc, double ra, double rb, double rc) {
    	
    	double S = (Math.pow(xc, 2.) - Math.pow(xb, 2.) + Math.pow(yc, 2.) - Math.pow(yb, 2.) + Math.pow(rb, 2.) - Math.pow(rc, 2.)) / 2.0;
    	double T = (Math.pow(xa, 2.) - Math.pow(xb, 2.) + Math.pow(ya, 2.) - Math.pow(yb, 2.) + Math.pow(rb, 2.) - Math.pow(ra, 2.)) / 2.0;
    	double y = ((T * (xb - xc)) - (S * (xb - xa))) / (((ya - yb) * (xb - xc)) - ((yc - yb) * (xb - xa)));
    	double x = ((y * (ya - yb)) - T) / (xb - xa);
    	
    	int[] a = {(int)x,(int)y};
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
	                    	c.drawCircle(ibeacon_locations.getJSONArray(i).getInt(0),ibeacon_locations.getJSONArray(i).getInt(1), ble_rad[i], p);
	                    	// c.drawCircle(ble_locs[i][0], ble_locs[i][1], 20, ble);
	                    	 canvas.drawCircle(ibeacon_locations.getJSONArray(i).getInt(0),ibeacon_locations.getJSONArray(i).getInt(1), ble_rad[i], p);
                    	}
	                    canvas.drawCircle(ibeacon_locations.getJSONArray(i).getInt(0),ibeacon_locations.getJSONArray(i).getInt(1), 5, ble); // still show iBeacon location even if no radius
                    	
                    	//canvas.drawCircle(ble_locs[i][0], ble_locs[i][1], ble_rad[i], blerad);
                        // canvas.drawCircle(ble_locs[i][0], ble_locs[i][1], 20, ble);
                        // Log.v("iBeacon", "BLE "+i+": "+ble_rad[i]);
                        
/*                        if (ble_rad[i] < minrad) {
                        	minrad = ble_rad[i];
                        	min = ble_locs[i];
                        	//Log.v("iBeacon", "BLE "+i+": "+ble_rad[i]);
                        }*/
                        
                    }
                    
                    
/*                    for(int i=0;i<c.getWidth();i++){
                    	for(int j=0;j<c.getHeight();j++){
                    		if ((double)(b.getPixel(i,j)+16777216) / 16777216 < minval) {
                    			minval = (double) (b.getPixel(i,j)+16777216) / 16777216;
                    		}
                        }
                    }
                    for(int i=0;i<c.getWidth();i++){
                    	for(int j=0;j<c.getHeight();j++){
                    		if ((double)(b.getPixel(i,j)+16777216) / 16777216 == minval) {
                    			if (bounds[0] < i)
                    				bounds[0] = i;
                    			if (bounds[1] < j)
                    				bounds[1] = j;
                    			if (bounds[2] > i)
                    				bounds[2] = i;
                    			if (bounds[3] > j)
                    				bounds[3] = j;
                    		}
                        }
                    } 
                    userx = (bounds[0] + bounds[2]) / 2;
                    usery = (bounds[1] + bounds[3]) / 2;*/
                    // Log.v("iBeacon", "Difference "+(userx-userx_prev)+", "+(double)(usery-usery_prev)/multiple);
                    
                    // Make sure marker is inside hallway bounds
                    
                    // double dist = Math.sqrt(Math.pow(userloc[1]-usery_prev,2)+Math.pow(userloc[0]-userx_prev,2));
                    if (userloc[1] - usery_prev > 5.24*multiple) {
                    	userloc[1] = usery_prev + (int) Math.round(5.24*multiple);
                    } else if (userloc[1] - usery_prev < - 5.24*multiple) {
                    	userloc[1] = usery_prev - (int) Math.round(5.24*multiple);
                    }
                    if (userloc[0] - userx_prev > 5.24*multiple) {
                    	userloc[0] = userx_prev + (int) Math.round(5.24*multiple);
                    } else if (userloc[0] - userx_prev < - 5.24*multiple) {
                    	userloc[0] = userx_prev - (int) Math.round(5.24*multiple);
                    }
                   /* if ((userloc[0]) < 600 && userloc[0] > 0 && userloc[1] < 450) {
                    	userloc[1] = 395;
                    }*/
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
