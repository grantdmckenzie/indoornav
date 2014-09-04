package edu.ucsb.ble02;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class FinishActivity extends Activity {
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.finish);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String pid = extras.getString("pid");
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.setText("Thank you, Participant #"+pid+" finished the task.  Please click the button below.");
        }
       
        Button done = (Button) findViewById(R.id.button1);
        done.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	 finish();
            }
        });
	}

}
