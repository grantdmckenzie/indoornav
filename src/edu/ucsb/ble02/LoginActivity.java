package edu.ucsb.ble02;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends Activity {
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View decorView = getWindow().getDecorView();
	     int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
	                   | View.SYSTEM_UI_FLAG_FULLSCREEN;
	     decorView.setSystemUiVisibility(uiOptions);
        Display display = getWindowManager().getDefaultDisplay();
        setContentView(R.layout.login);
        
        Button start = (Button) findViewById(R.id.button1);
        start.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	 EditText part = (EditText) findViewById(R.id.editText1);
                 String participant = part.getText().toString();
     	         Intent mapView = new Intent(LoginActivity.this, MainActivity.class);
     	         mapView.putExtra("pid", participant);
     	         startActivity(mapView);
            }
        });
	}
}
