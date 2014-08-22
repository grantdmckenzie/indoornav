package edu.ucsb.ble02;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

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
    private static float multiple = 1.0f;	// EDIT
    double localmult = 0; //multiple;   // EDIT
    private static int[] userloc;
    //private static int[][] ble_locs = {{(int)(4*multiple),0},{0, 3*multiple},{(int)(4*multiple),6*multiple},{0, 9*multiple},{(int)(4*multiple),12*multiple},{0, 15*multiple},{4*multiple, 18*multiple},{0, 21*multiple},{4*multiple, 24*multiple}};
    private static JSONArray ibeacon_locations;
    private static float[] ble_rad = new float[21];
    private static float[] ble_rad_prev = new float[21];
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
        	//Log.v("iBeacon", "didRangeBeaconsInRegion");
        	//Log.v("iBeacon", "iBeacon Array Size: "+iBeacons.size());
        	int ble_closestx = 500;
        	int ble_closesty = 500;
        	int ble_closest_id = 0;
        	int[] lowest3 = new int[3];
            if (iBeacons.size() > 0) {
            	//Log.v("iBeacon", ""+iBeacons.size());
            	
            	double minval = 300.0;
	    	    for (IBeacon iBeacon : iBeacons) {  
	    	    	if (iBeacon.getMinor() > 0) {
	    	    		if (iBeacon.getAccuracy()*multiple > ble_rad_prev[iBeacon.getMinor() - 1] + 1)
	    	    			ble_rad[iBeacon.getMinor() - 1] = (float)(iBeacon.getAccuracy()*multiple + 1);
	    	    		else if (iBeacon.getAccuracy()*multiple < ble_rad_prev[iBeacon.getMinor() - 1] - 1)
	    	    			ble_rad[iBeacon.getMinor() - 1] = (float)(iBeacon.getAccuracy()*multiple - 1);
	    	    		else
	    	    			ble_rad[iBeacon.getMinor() - 1] = (float)(iBeacon.getAccuracy()*multiple); //*2/3 + (float)(ble_rad_prev[iBeacon.getMinor() - 1]*1/3);
	    	    		
	    	    		try {
							write2File(iBeacon.getMinor()+","+iBeacon.getAccuracy()+","+iBeacon.getProximity()+","+iBeacon.getRssi());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	    	    		/*if (iBeacon.getAccuracy() < minval) {
	    	    			minval = iBeacon.getAccuracy();
	    	    			try {
								ble_closestx = ibeacon_locations.getJSONArray(iBeacon.getMinor() - 1).getInt(0);
								ble_closesty = ibeacon_locations.getJSONArray(iBeacon.getMinor() - 1).getInt(1);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
	    	    			ble_closest_id = iBeacon.getMinor();
	    	    		}*/
	    	    	}
	    	    	
	    	    	//Log.v("iBeacon", "minor beacon: "+iBeacon.getMinor() + ": " + iBeacon.getAccuracy());
	    	    }
	    	    lowest3 = getLowestThree(ble_rad);
	    	    for(int i=0;i<lowest3.length;i++) {
	    	    	int r = lowest3[i] + 1;
	    	    	Log.v("iBeacon", "minor beacon: "+r + ": " + ble_rad[lowest3[i]]);
	    	    }
	    	    Log.v("iBeacon", "----------------");
	    	    ble_rad_prev = ble_rad;
    	    	try {
					userloc = trilateration(ibeacon_locations.getJSONArray(lowest3[0]).getInt(0),ibeacon_locations.getJSONArray(lowest3[0]).getInt(1),ibeacon_locations.getJSONArray(lowest3[1]).getInt(0),ibeacon_locations.getJSONArray(lowest3[1]).getInt(1),ibeacon_locations.getJSONArray(lowest3[2]).getInt(0),ibeacon_locations.getJSONArray(lowest3[2]).getInt(1), ble_rad[lowest3[0]],ble_rad[lowest3[1]],ble_rad[lowest3[2]]);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    	    	userloc = forceHallways(userloc);
				/*userloc[0] = ble_closestx;
				userloc[1] = ble_closesty;*/
	    		// Log.v("iBeacon", "X: "+userloc[0]+", Y: "+userloc[1]);
	    	    //a += "ID: "+iBeacon.getMinor()+" | Rssi: "+(float)Math.round(iBeacon.getAccuracy()*1000)/1000 + "\n"; // + " | Prox: " + iBeacon.getProximity() +"\n";
          	
            
            }}
            });

        try {
            iBeaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
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
/*    	mCamera = Camera.open();
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        mTextureView.setLayoutParams(new FrameLayout.LayoutParams(
        previewSize.width, previewSize.height, Gravity.CENTER));
        try {
           mCamera.setPreviewTexture(surface);
          } catch (IOException t) {
          }
        mCamera.startPreview();
        mTextureView.setAlpha(1.0f);
        mTextureView.setRotation(90.0f);*/
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
                    
                    int[] min = {-1,-1};
                    // maxx, maxy, minx, miny
                    int[] bounds = {0,0,2000,2000};
                    float minrad = 20000;
                    double minval = 1;
                    int userx = -10;
                    int usery = -10;
                    
                    for(int i=0;i<ibeacon_locations.length();i++) {
                    	if (ble_rad[i] > 0) {
	                    	Paint p = makeRadGrad(ibeacon_locations.getJSONArray(i).getInt(0),ibeacon_locations.getJSONArray(i).getInt(1),ble_rad[i]);
	                    	//c.drawCircle(ibeacon_locations.getJSONArray(i).getInt(0),ibeacon_locations.getJSONArray(i).getInt(1), ble_rad[i], p);
	                    	// c.drawCircle(ble_locs[i][0], ble_locs[i][1], 20, ble);
	                    	// canvas.drawCircle(ibeacon_locations.getJSONArray(i).getInt(0),ibeacon_locations.getJSONArray(i).getInt(1), ble_rad[i], p);
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
                    if (userloc[1] - usery_prev > 24) {
                    	userloc[1] = usery_prev + 24;
                    } else if (userloc[1] - usery_prev < - 24) {
                    	userloc[1] = usery_prev - 24;
                    }
                    if (userloc[0] - userx_prev > 24) {
                    	userloc[0] = userx_prev + 24;
                    } else if (userloc[0] - userx_prev < - 24) {
                    	userloc[0] = userx_prev - 24;
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
        Log.v("iBeacon", "test");
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
        Log.v("iBeacon", "Size: " + jArray.length());
        /*        for(int i=0;i<jArray.length();i++){
            Log.v("iBeacon",""+jArray.getJSONArray(i).getInt(0));
	    }*/
        return jArray;
    }
    private void write2File(String data) throws IOException {
    	myOutWriter.append(data+"\n");
    }
}
