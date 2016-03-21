package com.android.wifi.test;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class WifiActivity extends Activity implements IWifiConnectionInfoListener{
    private PinGuoWifiSettings mWifiSettings=PinGuoWifiSettings.getInstance();
    private Button mConnectBtn;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifi);
		mConnectBtn=(Button) findViewById(R.id.connect);
		mConnectBtn.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				mWifiSettings.setStartPINGUOWifi(true);		
			}
		});
		mWifiSettings.init(this);
		mWifiSettings.setListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_wifi, menu);
		return true;
	}

	@Override
	public void isWifiConnectionSuccess(boolean isWifiConnSuccess) {
	    if(isWifiConnSuccess){
	    	Toast.makeText(this, "连接指定的Wifi成功", Toast.LENGTH_LONG).show();
	    }else{
	    	showStartScanWifiDialog();
	    }
		
	}

	@Override
	protected void onResume() {
		mWifiSettings.onResume();
		super.onResume();
	}

	@Override
	protected void onPause() {
		mWifiSettings.onPause();
		super.onPause();
	}
    
	@Override
	protected void onDestroy() {
		mWifiSettings.onDestroy();
		super.onDestroy();
	}

	AlertDialog mRetryAlertDialog;

	private void showStartScanWifiDialog() {
		if(mRetryAlertDialog!=null && mRetryAlertDialog.isShowing()){
			return;
		}
		android.content.DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mRetryAlertDialog.cancel();
				mWifiSettings.reset(true);
				if (mWifiSettings.getListener() == null) {
					mWifiSettings.setListener(WifiActivity.this);
				}
			}
		};
		android.content.DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				arg0.dismiss();
				mWifiSettings.clear();
			}

		};
		android.content.DialogInterface.OnKeyListener keyListener=new DialogInterface.OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if(keyCode==KeyEvent.KEYCODE_BACK || keyCode==KeyEvent.KEYCODE_SEARCH){
					dialog.dismiss();
					mWifiSettings.clear();
					return true;
				}
				return false;
			}
		};
		AlertDialog.Builder builder=new AlertDialog.Builder(this);
		builder.setTitle("温馨提示");
		builder.setMessage("连接指定的wifi失败是否需要从新连接");
		builder.setPositiveButton("从试", positiveListener);
		builder.setNegativeButton("取消", negativeListener);
		mRetryAlertDialog=builder.create();
		mRetryAlertDialog.setOnKeyListener(keyListener);
		mRetryAlertDialog.setCanceledOnTouchOutside(false);
		mRetryAlertDialog.setCancelable(false);
		mRetryAlertDialog.show();
	}
}
