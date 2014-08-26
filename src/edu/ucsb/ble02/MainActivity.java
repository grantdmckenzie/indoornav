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
import java.util.Random;

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
    private static float multiple = 3.8f;	// EDIT
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
        /*try {
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
		} */
        
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
	        	Log.v("iBeacon", "didRangeBeaconsInRegion");
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
    
    private ArrayList<ble> multitrilateration(Collection<IBeacon> beacons, ArrayList<ble> prevBeacons) {
    	// write loop for trilateration

    	Random rand = new Random();
    	int r = rand.nextInt();
    	ArrayList<IBeacon> aBeacons = new ArrayList<IBeacon>();
    	if (prevBeacons.size() == 0) {
    		for(int i=0;i<ibeacon_locations.length();i++) {
    			prevBeacons.add(new ble(i+1,10, r));
    		}
    	}
    	
    	
    	for (IBeacon iBeacon : beacons) { 
    		if (iBeacon.getMinor() > 0) {
    			if(iBeacon.getAccuracy()*multiple < 100) {
    				for(int i=0;i<prevBeacons.size();i++) {
    					if (prevBeacons.get(i).id == iBeacon.getMinor()) {
    						/*double a = prevBeacons.get(i).accuracy - iBeacon.getAccuracy()*multiple;
    						// Log.v("iBeacon", "Accuracy Diff: "+ iBeacon.getAccuracy()*multiple + " | " + prevBeacons.get(i).accuracy + " | " +a);
    						
    						if (a > multiple * 5)
    							prevBeacons.get(i).accuracy = iBeacon.getAccuracy()*multiple + 5*multiple;
    						else if (a < multiple * -5)
    							prevBeacons.get(i).accuracy = iBeacon.getAccuracy()*multiple - 5*multiple;
    						else*/
    							prevBeacons.get(i).accuracy = iBeacon.getAccuracy()*multiple;
    						
    						prevBeacons.get(i).ts = r;
    						
    					}
    				}
    				
    			}
    		}
    	}
    	
    	// Log.v("iBeacon", "ArrayList Size : "+aBeacons.size());
    	// Log.v("iBeacon","PrevList Size: "+prevBeacons.size());*/
    	double xloc = 0;
    	double yloc = 0;
    	ArrayList<Integer[]> existing = new ArrayList<Integer[]>();
    	List<Integer> xasdf = new ArrayList<Integer>();
    	List<Integer> yasdf = new ArrayList<Integer>();
    	int count = 0;
    	for(int i=0;i<prevBeacons.size();i++) {
    		if (prevBeacons.get(i).ts == r) {
    		for(int j=0;j<prevBeacons.size();j++) {
    			if (prevBeacons.get(j).ts == r) {
    			for(int k=0;k<prevBeacons.size();k++) {
    				if (prevBeacons.get(k).ts == r) {
    			
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
	  
	        		    	int[] xy;
	        				try {
								// if (prevBeacons.get(i).accuracy < 30*multiple && prevBeacons.get(j).accuracy < 30*multiple && prevBeacons.get(k).accuracy < 30*multiple) {
									xy = trilateration(
											ibeacon_locations.getJSONArray(prevBeacons.get(i).id-1).getInt(0), 
											ibeacon_locations.getJSONArray(prevBeacons.get(i).id-1).getInt(1),
											ibeacon_locations.getJSONArray(prevBeacons.get(j).id-1).getInt(0),
											ibeacon_locations.getJSONArray(prevBeacons.get(j).id-1).getInt(1),
											ibeacon_locations.getJSONArray(prevBeacons.get(k).id-1).getInt(0),
											ibeacon_locations.getJSONArray(prevBeacons.get(k).id-1).getInt(1),
											prevBeacons.get(i).accuracy,
											prevBeacons.get(j).accuracy,
											prevBeacons.get(k).accuracy
										);
									if (xy[0] > 0 && xy[1] > 0 && xy[0] < 1920 && xy[1] < 1920) {
										// xasdf.add(xy[0]);
										// yasdf.add(xy[1]);
			    						xloc += xy[0];
			    						yloc += xy[1];
			    						// Log.v("iBeacon", "xy "+prevBeacons.get(i).id+"/"+prevBeacons.get(j).id+"/"+prevBeacons.get(k).id+": "+xy[0] + ", " + xy[1]);
										// Log.v("iBeacon", "x ("+count+"): "+x[count]);
			    						count++;
			    						
			    					}
								// }
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
        				}
        			}
    				}
        			}
        		}
    		}
    		}
    	}
    	Log.v("iBeacon", "Count: "+count);
    	if (count > 0) {
/*    		double medianx = 0;
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
    		}*/
    		double medianx = xloc / count;
    		double mediany = yloc / count;
    		
    		if (mediany - userloc[1] > 4*multiple) {
            	userloc[1] = (int) (userloc[1] + 4*multiple);
            } else if (mediany - userloc[1] < - 4*multiple) {
            	userloc[1] = (int) (userloc[1] - 4*multiple);
            } else {
            	userloc[1] = (int) mediany;
            }
            	
    		if (medianx - userloc[0] > 4*multiple) {
            	userloc[0] = (int) (userloc[0] + 4*multiple);
            } else if (medianx - userloc[0] < - 4*multiple) {
            	userloc[0] = (int) (userloc[0] - 4*multiple);
            } else {
            	userloc[0] = (int) medianx;
            }
    		// userloc = forceHallways(userloc);
	    	Log.v("iBeacon", "user location: "+userloc[0] + ", " + userloc[1]);
    	}
    	
    	return prevBeacons;
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
