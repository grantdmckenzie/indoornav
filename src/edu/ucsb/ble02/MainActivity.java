package edu.ucsb.ble02;

import java.util.Collection;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.RangeNotifier;
import com.radiusnetworks.ibeacon.Region;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.view.Display;
import android.view.Gravity;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends Activity implements IBeaconConsumer,TextureView.SurfaceTextureListener {
	private TextureView mTextureView;
    private MainActivity.RenderingThread mThread;
    protected static final String TAG = "RangingActivity";
    private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);
    private double prevble1rad, prevble2rad, prevble3rad, prevble1rad2, prevble2rad2, prevble3rad2 = 0.1;
    private static double ble1rad, ble2rad, ble3rad = 0.1;
    private static int multiple = 150;	// EDIT
    double localmult = multiple - 80;   // EDIT
    private static int[] userloc = {500,500};
    private static int[][] ble_locs = {{4*multiple,0},{0, 9*multiple/2},{4*multiple,9*multiple}};
    // private static int[][] ble_locs = {{4*multiple,0},{0,0},{2*multiple,(int)(4*multiple)}};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
		// setContentView(R.layout.main);
        iBeaconManager.bind(this);
        //Log.v("iBeacon", "onCreate");
        FrameLayout content = new FrameLayout(this);

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.setOpaque(true);

        content.addView(mTextureView, new FrameLayout.LayoutParams(4*multiple, 9*multiple, Gravity.CENTER));
        setContentView(content);
    }
    @Override 
    protected void onDestroy() {
        super.onDestroy();
        iBeaconManager.unBind(this);
    }
    @Override 
    protected void onPause() {
    	super.onPause();
    	if (iBeaconManager.isBound(this)) iBeaconManager.setBackgroundMode(this, true);    		
    }
    @Override 
    protected void onResume() {
    	super.onResume();
    	if (iBeaconManager.isBound(this)) iBeaconManager.setBackgroundMode(this, false);    		
    }

    @Override
    public void onIBeaconServiceConnect() {
        iBeaconManager.setRangeNotifier(new RangeNotifier() {
        @Override 
        public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons, Region region) {
            if (iBeacons.size() > 0) {
            	/*EditText editText = (EditText)MainActivity.this
            			.findViewById(R.id.rangingText);
            	//editText.setText("");
            	String a = ""; */
            	double b1 = -9, b2 = -9, b3 = -9, b4 = -9, b5 = -9, b6 = -9;
            	
	    	    for (IBeacon iBeacon : iBeacons) {  
	    	    	if (iBeacon.getMinor() > 0) {
	    	    		// Log.v("iBeacon", "ID: "+iBeacon.getMinor()+" | Accuracy: "+(float)Math.round(iBeacon.getAccuracy()*1000)/1000);
	    	    		
	    	    		if (iBeacon.getMinor() == 1) {
	    	    			b1 = iBeacon.getAccuracy()*localmult;
	    	    		} else if (iBeacon.getMinor() == 2) {
	    	    			b2 = iBeacon.getAccuracy()*localmult;
	    	    			//b2 = (iBeacon.getProximity()*localmult*1.5 + iBeacon.getAccuracy()*localmult)/2;
	    	    		} else if (iBeacon.getMinor() == 3) {
	    	    			b3 = iBeacon.getAccuracy()*localmult;
	    	    		} else if (iBeacon.getMinor() == 4) {
	    	    			b4 = iBeacon.getAccuracy()*localmult;
	    	    		} else if (iBeacon.getMinor() == 5) {
	    	    			b5 = iBeacon.getAccuracy()*localmult;
	    	    		} else if (iBeacon.getMinor() == 6) {
	    	    			b6 = iBeacon.getAccuracy()*localmult;
	    	    		}
	    	    		
	    	    	}
	    	    }
	    		ble1rad = (((b1 + b4) / 2) + prevble1rad2 + prevble1rad) /3;
	    		ble2rad = (((b2 + b5) / 2) + prevble2rad2 + prevble2rad) /3;
	    		ble3rad = (((b3 + b6) / 2) + prevble3rad2 + prevble3rad) /3;
    	    	prevble1rad2 = prevble1rad;
    	    	prevble2rad2 = prevble2rad;
    	    	prevble3rad2 = prevble3rad;
    	    	prevble1rad = ble1rad;
    	    	prevble2rad = ble2rad;
    	    	prevble3rad = ble3rad;
    	    	Log.v("iBeacon", "Bcomb: "+ble3rad+", B6: "+b6);
    	    	userloc = trilateration(ble_locs[0][0], ble_locs[0][1], ble_locs[1][0], ble_locs[1][1], ble_locs[2][0], ble_locs[2][1], ble1rad, ble2rad, ble3rad);
	    		// Log.v("iBeacon", "X: "+userloc[0]+", Y: "+userloc[1]);
	    	    		//a += "ID: "+iBeacon.getMinor()+" | Rssi: "+(float)Math.round(iBeacon.getAccuracy()*1000)/1000 + "\n"; // + " | Prox: " + iBeacon.getProximity() +"\n";
	    	    
	    	    //logToDisplay(a); 
            	//logToDisplay("The first iBeacon I see is about "+iBeacons.iterator().next().getProximityUuid()+" meters away.");            	
            
            }}
            });

        try {
            iBeaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
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
            float x = 0.0f;
            float y = 0.0f;
            float speedX = 5.0f;
            float speedY = 3.0f;
            
            Paint paint = new Paint();
            paint.setColor(0xff0066ff);
            Paint ble = new Paint();
            ble.setColor(0xff009900);
            Paint blerad = new Paint();
            blerad.setStyle(Paint.Style.STROKE);
            blerad.setStrokeWidth(1);
            blerad.setColor(0xff009900);
            
            Paint canvasrec = new Paint();
            canvasrec.setStyle(Paint.Style.STROKE);
            canvasrec.setStrokeWidth(10);
            canvasrec.setColor(0xff000000);

            while (mRunning && !Thread.interrupted()) {
                final Canvas canvas = mSurface.lockCanvas(null);
                try {
                	
                    canvas.drawColor(0xffffffff);
                	canvas.drawRect(0, 0, 4*multiple, 9*multiple, canvasrec);
                    // canvas.drawRect(x, y, x + 20.0f, y + 20.0f, paint);
                    canvas.drawCircle(userloc[0], userloc[1], 30, paint);
                    // STKO BLE 01
                    canvas.drawCircle(ble_locs[0][0], ble_locs[0][1], (int)ble1rad, blerad);
                    canvas.drawCircle(ble_locs[0][0], ble_locs[0][1], 30, ble);
                    // STKO BLE 02
                    canvas.drawCircle(ble_locs[1][0], ble_locs[1][1], (int)ble2rad, blerad);
                    canvas.drawCircle(ble_locs[1][0], ble_locs[1][1], 30, ble);
                    // STKO BLE 03
                    canvas.drawCircle(ble_locs[2][0], ble_locs[2][1], (int)ble3rad, blerad);
                    canvas.drawCircle(ble_locs[2][0], ble_locs[2][1], 30, ble);
                } finally {
                    mSurface.unlockCanvasAndPost(canvas);
                }

              /*  if (x + 20.0f + speedX >= mSurface.getWidth() || x + speedX <= 0.0f) {
                    speedX = -speedX;
                }
                if (y + 20.0f + speedY >= mSurface.getHeight() || y + speedY <= 0.0f) {
                    speedY = -speedY;
                }

                x += speedX;
                y += speedY; */

                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    // Interrupted
                }
            }
        }
        
        void stopRendering() {
            interrupt();
            mRunning = false;
        }
    }
}
