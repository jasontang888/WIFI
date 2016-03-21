package com.android.wifi.test;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import android.util.Log;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PinGuoWifiSettings {
	// Debug标签
	private static final String TAG = "PinGuoWifiSettings";
	// 在PINGUO镜头开启时wifi是否已经打开
	private boolean mWifiIsOpen = false;
	// 变量表示wifi是否可用
	private boolean mWifiEnable = false;
	private Context mContext;
	// wifi管理类
	private WifiManager mWifiManager = null;
	// wifi相关的锁
	private WifiLock mWifiLock;
	private MulticastLock mwifiMulticastLock;
	private WakeLock mWakeLock = null;
	// wifi信息
	private WifiInfo mConnectedWifiInfo = null;
	private WifiInfo mPINGUOWifiInfo;
	private WifiInfo mLastInfo;
	// 扫描类
	private Scanner mScanner = null;
	private AtomicBoolean mConnected = new AtomicBoolean(false);
	private IntentFilter mFilter;
	private BroadcastReceiver mReceiver;
	// 是否显示wifi链接对话框，false表示需要
	// private boolean mShowWifiDialog = false;
	// 单个wifi连接对话框
	private AlertDialog mWifiDialog;
	// list列表对话框
	private AlertDialog mWifiDialogs;
	// 单个wifi对话框的id
	private static final int WIFI_DIALOG_ID = 1;
	// wifi list对话框的id
	private static final int WIFI_DIALOG_IDS = 2;
	// 无线AP
	private AccessPoint mDlgAccessPoint;
	// 无线ap列表
	private List<AccessPoint> mPINGUOAccessPoints = new ArrayList<AccessPoint>();
	private Handler mHandler;
	// 回调接口
	private IWifiConnectionInfoListener mListener;
	// wifi已经连接上
	public static final int PINGUO_WIFI_CONNECTED = 5;
	// wifi断开连接
	public static final int PINGUO_WIFI_DISCONNECTED = 0;
	// wifi正在连接
	public static final int PINGUO_WIFI_CONNECTING = 3;
	// wifi正在扫描
	public static final int PINGUO_WIFI_SCANNER = 1;
	// wif链接错误
	public static final int PINGUO_WIFI_CONNECTED_FAIL = 4;
	// 没有wifi设备
	public static final int PINGUO_WIFI_DEVICE_NOT_FOUND = 6;
	// 显示对话框或者对话框列表
	public static final int PINGUO_WIFI_SHOW_DIAOG_OR_DIALOGS = 2;
	// 当前wifi状态
	private int mWifiState = PINGUO_WIFI_DISCONNECTED;
	// 扫描PINGUO wifi的对话框
	private ProgressDialog mScannerDialog;
	// 链接PINGUO wifi的对话框
	private ProgressDialog mProcessDialog;
	// 没有搜索到PINGUO设备
	private AlertDialog mNoPINGUODevice;
	// 是否开启链接PINGUO wifi
	private boolean mStartPINGUOWifi = false;
	// wifi链接开始的时间
	private long mWifiStartConnectTime = 0L;
	// 当前acitivty的状态
	private boolean mActivityState = false;
	// 变量值控制第一次记录连接的到底是什么什么wifi
	private boolean mFirstConnectWifi = false;
	// 变量值控制是否显示密码输入错误的提示
	private boolean mWifiPasswordErrorPrompt = false;
	// 防止用户点击search键
	private android.content.DialogInterface.OnKeyListener mSearchKeyListener = new DialogInterface.OnKeyListener() {
		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_SEARCH) {
				return true;
			}
			return false;
		}
	};

	public boolean isStartPINGUOWifi() {
		return mStartPINGUOWifi;
	}

	@SuppressLint("DefaultLocale")
	public void setStartPINGUOWifi(boolean startPINGUOWifi) {
		if (mStartPINGUOWifi != startPINGUOWifi) {
			mStartPINGUOWifi = startPINGUOWifi;
			if (startPINGUOWifi) {
				Log.i(TAG, "开启PINGUO wifi HHHHHHHHHHH mWifiEnable="+ mWifiEnable + ",mFirstConnectWifi="+ mFirstConnectWifi);
				// wifi没有打开或者wifi已经坏掉
				if (!mWifiEnable) {
					mHandler.sendEmptyMessage(CONNECT_PINGUO_WIFI_FAIL);
					return;
				}
				// 记录wifi打开状态下已经链接到的wifi
				WifiInfo info = mWifiManager.getConnectionInfo();
				if (!mFirstConnectWifi) {
					mFirstConnectWifi = true;
					mConnectedWifiInfo = info;
				} else {
					if (info != null) {
						String ssid = info.getSSID();
						if (ssid != null && !ssid.equalsIgnoreCase("")&& !(ssid.toLowerCase().replaceAll("\"", "").startsWith("direct"))) {
							if (mConnectedWifiInfo != info) {
								mConnectedWifiInfo = info;
								Log.i(TAG, "更新一次以前连接的wifi: "+ mConnectedWifiInfo);
							}
						}
					}
				}
				// 断开当前链接的wifi网络，不管是否是不是PINGUO wifi
				Log.i(TAG, "在链接PINGUO wifi前已经连接的wifi=" + info);
				if (info != null && info.getNetworkId() != -1) {
					boolean disable = mWifiManager.disableNetwork(info.getNetworkId());
					boolean disconnect = mWifiManager.disconnect();
					Log.i(TAG, "断掉以前wifi disable=" + disable+ ",disconnect=" + disconnect);
				}
				//显示扫描的进度对话框
				mHandler.sendEmptyMessage(SHOW_DIALOG_MESSAGE);
				//10秒扫描不成功我们处理失败消息
				mHandler.postDelayed(mWifiSannerMessage,WIFI_SCANNER_DISMISS_DELAY);
				mScanner.forceScan();
			}
		}
	}

	public int getmWifiState() {
		return mWifiState;
	}

	public IWifiConnectionInfoListener getListener() {
		return mListener;
	}

	public void setListener(IWifiConnectionInfoListener listener) {
		this.mListener = listener;
	}

	private static PinGuoWifiSettings mWifiSettingInstance = new PinGuoWifiSettings();

	public static PinGuoWifiSettings getInstance() {
		return mWifiSettingInstance;
	}

	@SuppressLint("HandlerLeak")
	public void init(Context context) {
		Log.i(TAG, "init ############ PinGuoWifiSettings");
		reset(false);
		mFirstConnectWifi = false;
		mContext = context;
		mWifiIsOpen = false;
		mWifiEnable = false;
		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		mScanner = new Scanner(mContext.getMainLooper());
		mHandler = new Handler(mContext.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				try {
					switch (msg.what) {
					case CONNECT_PINGUO_DEVICE:
						sendEmptyMessage(CONNECT_PINGUO_WIFI_SUCCESS);
						break;
					case CACEL_DIALOG_MESSAGE:
						removeCallbacks(mWifiSannerMessage);
						if (mScannerDialog != null&& mScannerDialog.isShowing()) {
							mScannerDialog.cancel();
						}
						mScannerDialog = null;
						break;
					case SHOW_DIALOG_MESSAGE:
						if (mScannerDialog != null&& mScannerDialog.isShowing()) {
							return;
						}
						mScannerDialog=new ProgressDialog(mContext);
						mScannerDialog.setMessage("正在扫描连接的wifi设备");
						mScannerDialog.setCancelable(false);
						mScannerDialog.setCanceledOnTouchOutside(false);
						mScannerDialog.setOnKeyListener(mSearchKeyListener);
						mScannerDialog.show();
						break;
					case CONNECT_PINGUO_WIFI_FAIL:
						//非暂停状态我们处理连接成功与失败的消息
						if (!mActivityState) {
							// 移除超时的runable
							removeCallbacks(mWifiConnectionMessage);
							// 移除超时的runable
							sendEmptyMessage(CANCEL_DIALOG_CONNECT_WIFI);
							mWifiState = PINGUO_WIFI_CONNECTED_FAIL;
							mListener.isWifiConnectionSuccess(false);
						}
						break;
					case CONNECT_PINGUO_WIFI_SUCCESS:
						//非暂停状态我们处理连接成功与失败的消息
						if (!mActivityState) {
							Log.i(TAG, "连接PINGUO成功时我们取消runnable消息发送");
							removeCallbacks(mWifiConnectionMessage);
							sendEmptyMessage(CANCEL_DIALOG_CONNECT_WIFI);
							mWifiState = PINGUO_WIFI_CONNECTED;
							mListener.isWifiConnectionSuccess(true);
						}
						break;
					case SHOW_DIALOG_CONNECT_WIFI:
						if (mProcessDialog != null&& mProcessDialog.isShowing()) {
							return;
						}
						mProcessDialog=new ProgressDialog(mContext);
						mProcessDialog.setMessage("正在连接指定的wifi网络");
						mProcessDialog.setCancelable(false);
						mProcessDialog.setCanceledOnTouchOutside(false);
						mProcessDialog.setOnKeyListener(mSearchKeyListener);
						mProcessDialog.show();
						break;
					case CANCEL_DIALOG_CONNECT_WIFI:
						if (mProcessDialog != null && mProcessDialog.isShowing()) {
							mProcessDialog.cancel();
						}
						mProcessDialog = null;
						break;
					case SHOW_DIALOG_NO_DEVICE:
						// 显示没有搜索到PINGUO设备的对话框
						mWifiState = PINGUO_WIFI_DEVICE_NOT_FOUND;
						if (mNoPINGUODevice != null && mNoPINGUODevice.isShowing()) {
							return;
						}
						AlertDialog.Builder builder=new AlertDialog.Builder(mContext);
						builder.setMessage("没有搜索到要连接的wifi设备");
						builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,int which) {
										mNoPINGUODevice.cancel();
										clear();
									}
								});						
						mNoPINGUODevice=builder.create();
						mNoPINGUODevice.setCanceledOnTouchOutside(false);
						mNoPINGUODevice.setCancelable(false);
						mNoPINGUODevice.show();
						break;
					case SCAN_PINGUO_WIF_FAIL:
						sendEmptyMessage(CONNECT_PINGUO_WIFI_FAIL);
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
					Log.w(TAG, "exception=" + e.toString());
				}
			}

		};
		mWifiLock = mWifiManager.createWifiLock("pinguo_wifi");
		mwifiMulticastLock = mWifiManager.createMulticastLock("pinguo_multicastWifi");
		PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "pinguo_power");
		mFilter = new IntentFilter();
		mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
		mReceiver = new BroadcastReceiver() {
			@SuppressLint("DefaultLocale")
			@Override
			public void onReceive(Context context, Intent intent) {
				//处理wifi认证失败的信息
				if(mDlgAccessPoint!=null){
					//判断是否是我们连接的wifi,我们连接的wifi的ssid是以“direct”开头的
					String ssid=mDlgAccessPoint.ssid;
					if(ssid!=null && ssid.toLowerCase().replaceAll("\"", "").startsWith("direct")){
						if (intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1) == WifiManager.ERROR_AUTHENTICATING) {
							//认证失败之后我们只需要保存一次记录即可
							if(AuthParametersSave.getAutchCode(mContext, ssid.replaceAll("\"", ""))!=AuthParametersSave.ERROR_AUTHENTICATING){
								Log.e(TAG, "认证失败。。。。。。。ssid=" + ssid);
								AuthParametersSave.saveAutchCode(mContext,ssid.replaceAll("\"", ""),AuthParametersSave.ERROR_AUTHENTICATING);
							    mHandler.sendEmptyMessage(CONNECT_PINGUO_WIFI_FAIL);
							}
							return;
						}
					}
				}
				handleEvent(context, intent);
			}
		};
	}

	public void reset(boolean isForce) {
		mStartPINGUOWifi = isForce;
		// mListener = null;
		mWifiState = PINGUO_WIFI_DISCONNECTED;
		mConnected.set(false);
		if (isForce) {
			WifiInfo info = mWifiManager.getConnectionInfo();
			Log.i(TAG, "reset 在链接PINGUO wifi前已经连接的wifi=" + mConnectedWifiInfo);
			if (info != null && info.getNetworkId() != -1) {
				boolean disable = mWifiManager.disableNetwork(info.getNetworkId());
				boolean disconnect = mWifiManager.disconnect();
				Log.i(TAG, "断掉以前wifi disable=" + disable + ",disconnect="+ disconnect);
			}
			mHandler.sendEmptyMessage(SHOW_DIALOG_MESSAGE);
			mHandler.postDelayed(mWifiSannerMessage, WIFI_SCANNER_DISMISS_DELAY);
			mScanner.forceScan();
		}
	}

	// 清除一些变量的值，恢复为默认值
	public void clear() {
		mStartPINGUOWifi = false;
		mWifiState = PINGUO_WIFI_DISCONNECTED;
		mConnected.set(false);
	}

	public void onResume() {
		Log.i(TAG, "onResumeonResume");
		try {
			mActivityState = false;
			dissMissAllDialog();
			if (!mWifiManager.isWifiEnabled()) {
				if (mWifiManager.setWifiEnabled(true)) {
					mWifiEnable = true;
					Log.i(TAG, "打开wfi成功");
				} else {
					mWifiEnable = false;
					Log.i(TAG, "打开wfi失败");
				}
			} else {
				// wifi已经打开
				Log.i(TAG, "wifi在程序启动的时候已经是打开的了");
				mWifiIsOpen = true;
				mWifiEnable = true;
			}
			mContext.registerReceiver(mReceiver, mFilter);
			acquireLock();
			mScanner.resume();
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "" + e.toString());
		}
	}

	public void onPause() {
		Log.i(TAG, "onPauseonPause");
		try {
			mActivityState = true;
			mContext.unregisterReceiver(mReceiver);
			releaseLock();
			mScanner.pause();
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "" + e.toString());
		}
	}

	public void onDestroy() {
		long start = System.currentTimeMillis();
		Log.v(TAG, "onDestroy");
		try {
			// 修改一次变量的值
			mFirstConnectWifi = false;
			// 断掉PINGUO wifi的链接
			Log.v(TAG, "mPINGUOWifiInfo=" + mPINGUOWifiInfo);
			if (mPINGUOWifiInfo != null && mWifiManager.isWifiEnabled() && mPINGUOWifiInfo.getNetworkId() != -1) {
				boolean disable = mWifiManager.disableNetwork(mPINGUOWifiInfo.getNetworkId());
				boolean disconnect = mWifiManager.disconnect();
				mWifiState = PINGUO_WIFI_DISCONNECTED;
				Log.v(TAG, "销毁的时候断开连接 disable=" + disable + ",disconnect="+ disconnect);
			}
			// mPINGUOWifiInfo=null;
			// 如果以前连接过其他wifi我们需要打开它
			Log.v(TAG, "mConnectedWifiInfo=" + mConnectedWifiInfo);
			if (mConnectedWifiInfo != null && mWifiManager.isWifiEnabled()) {
				if (mConnectedWifiInfo.getNetworkId() != -1) {
					boolean enable = mWifiManager.enableNetwork(mConnectedWifiInfo.getNetworkId(), true);
					boolean reassociate=mWifiManager.reassociate();
					Log.i(TAG, "销毁的时候恢复以前链接的wifi enable=" + enable+ ",reassociate=" + reassociate);
				}
			}
			// mConnectedWifiInfo=null;
			if (!mWifiIsOpen) {
				if (mWifiManager.setWifiEnabled(false)) {
					Log.i(TAG, "关闭wifi成功");
				} else {
					Log.i(TAG, "关闭wifi失败");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "" + e.toString());
		}
		Log.v(TAG, "调用onDestroy共花费了" + (System.currentTimeMillis() - start)+ "毫秒");
	}

	private void acquireLock() {
		try {
			mWifiLock.acquire();
			mwifiMulticastLock.acquire();
			mWakeLock.acquire();
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "exception=" + e.toString());
		}
	}

	private void releaseLock() {
		try {
			mWifiLock.release();
			mwifiMulticastLock.release();
			mWakeLock.release();
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "exception=" + e.toString());
		}
	}

	@SuppressLint("DefaultLocale")
	private void updateAccessPoints() {
		Log.i(TAG, "updateAccessPoints 自定义mWifiState=" + mWifiState);
		// wifi不处于为连接状态
		if (mWifiState != PINGUO_WIFI_DISCONNECTED) {
			return;
		}
		// 控制不要重复弹出对话框
		if (mWifiDialog != null && mWifiDialog.isShowing()) {
			Log.i(TAG, "wifi dialog 已经显示了");
			return;
		}
		// 控制不要重复弹出对话框
		if (mWifiDialogs != null && mWifiDialogs.isShowing()) {
			Log.i(TAG, "wifi 列表dialog 已经显示了");
			return;
		}

		// 改变当前wifi状态
		mWifiState = PINGUO_WIFI_SCANNER;
		try {
			final int wifiState = mWifiManager.getWifiState();
			switch (wifiState) {
			case WifiManager.WIFI_STATE_ENABLED:
				final Collection<AccessPoint> accessPoints = constructAccessPoints();
				ArrayList<AccessPoint> PINGUOAccessPoints = new ArrayList<AccessPoint>();
				// 清空列表
				PINGUOAccessPoints.clear();
				for (AccessPoint accessPoint : accessPoints) {
					if (accessPoint.ssid.toLowerCase().replaceAll("\"", "")
							.startsWith("direct")) {
						if (accessPoint.getRssi() != Integer.MAX_VALUE) {
							Log.d(TAG, "扫描时 ssid=" + accessPoint.ssid+ ",信号=" + accessPoint.getRssi());
							PINGUOAccessPoints.add(accessPoint);
						} else {
							Log.d(TAG, "扫描时 ssid=" + accessPoint.ssid+ ",wifi不在范围内");
						}
					}
				}
				// 关闭正在扫描的对话框
				mHandler.sendEmptyMessage(CACEL_DIALOG_MESSAGE);
				// 搜索到PINGUO wifi的数量
				int size = PINGUOAccessPoints.size();
				Log.i(TAG, "搜索到的PINGUO wifi数量 size=" + size);
				// 改变当前wifi状态
				mWifiState = PINGUO_WIFI_SHOW_DIAOG_OR_DIALOGS;
				if (size > 0) {
					if (size == 1) {
						AccessPoint accessPoint = PINGUOAccessPoints.get(0);
						connectPINGUOWifiAccessPoint(accessPoint);
					} else {
						// 扫描到多个以上的PINGUO wifi设备需要做处理
						showDialog(PINGUOAccessPoints);
					}
				} else {
					// 不在扫描
					mHandler.sendEmptyMessage(SHOW_DIALOG_NO_DEVICE);
				}
				break;
			case WifiManager.WIFI_STATE_ENABLING:
			case WifiManager.WIFI_STATE_DISABLING:
			case WifiManager.WIFI_STATE_DISABLED:
			default:
				Log.i(TAG, "当前wifi状态 wifiState=" + wifiState);
				mWifiState = PINGUO_WIFI_DISCONNECTED;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "exception=" + e.toString());
		}
	}

	private List<AccessPoint> constructAccessPoints() {
		ArrayList<AccessPoint> accessPoints = new ArrayList<AccessPoint>();
		Multimap<String, AccessPoint> apMap = new Multimap<String, AccessPoint>();
		final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
		if (configs != null) {
			for (WifiConfiguration config : configs) {
				AccessPoint accessPoint = new AccessPoint(mContext, config);
				accessPoints.add(accessPoint);
				apMap.put(accessPoint.ssid, accessPoint);
			}
		}

		final List<ScanResult> results = mWifiManager.getScanResults();
		if (results != null) {
			for (ScanResult result : results) {
				if (result.SSID == null || result.SSID.length() == 0 || result.capabilities.contains("[IBSS]")) {
					continue;
				}
				boolean found = false;
				for (AccessPoint accessPoint : apMap.getAll(result.SSID)) {
					if (accessPoint.update(result))
						found = true;
				}
				if (!found) {
					AccessPoint accessPoint = new AccessPoint(mContext, result);
					accessPoints.add(accessPoint);
					apMap.put(accessPoint.ssid, accessPoint);
				}
			}
		}
		return accessPoints;
	}

	private class Multimap<K, V> {
		private HashMap<K, List<V>> store = new HashMap<K, List<V>>();
		List<V> getAll(K key) {
			List<V> values = store.get(key);
			return values != null ? values : Collections.<V> emptyList();
		}
		void put(K key, V val) {
			List<V> curVals = store.get(key);
			if (curVals == null) {
				curVals = new ArrayList<V>(3);
				store.put(key, curVals);
			}
			curVals.add(val);
		}
	}

	private void handleEvent(Context context, Intent intent) {
		// 没有开启PINGUOwifi不处理事件或者wifi没打开
		if (!mStartPINGUOWifi || !mWifiEnable) {
			return;
		}
		try {
			String action = intent.getAction();
			if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
				updateWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,WifiManager.WIFI_STATE_UNKNOWN));
			} else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
				updateAccessPoints();
			} else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
				SupplicantState state = (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
				if (!mConnected.get()) {
					updateConnectionState(WifiInfo.getDetailedStateOf(state));
				}
			} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
				NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				mConnected.set(info.isConnected());
				updateAccessPoints();
				updateConnectionState(info.getDetailedState());
			} else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
				updateConnectionState(null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "exception=" + e.toString());
		}
	}

	@SuppressLint("DefaultLocale")
	private void updateWifiState(int state) {
		try {
			switch (state) {
			case WifiManager.WIFI_STATE_ENABLED:
				mScanner.resume();
				return;
			case WifiManager.WIFI_STATE_ENABLING:
				Log.i(TAG,"正在打开wifi");
				break;

			case WifiManager.WIFI_STATE_DISABLED:
				Log.i(TAG,"wifi没有打开");
				break;
			}
			mScanner.pause();
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "exception=" + e.toString());
		}
	}

	@SuppressLint("DefaultLocale")
	private void updateConnectionState(DetailedState state) {
		Log.i(TAG, "updateConnectionState 系统连接 state=" + state+ ",自定义 mWifiState=" + mWifiState);
		// PINGUO wifi还没有开始链接,连接失败,连接成功,没有找到设备,正在扫描，显示对话框
		if (mWifiState == PINGUO_WIFI_DISCONNECTED
				|| mWifiState == PINGUO_WIFI_CONNECTED_FAIL
				|| mWifiState == PINGUO_WIFI_SCANNER
				|| mWifiState == PINGUO_WIFI_CONNECTED
				|| mWifiState == PINGUO_WIFI_DEVICE_NOT_FOUND
				|| mWifiState == PINGUO_WIFI_SHOW_DIAOG_OR_DIALOGS) {
			return;
		}
		try {
			// wifi没有打开
			if (!mWifiManager.isWifiEnabled()) {
				mScanner.pause();
				return;
			}
			// 获取当前连接的wifi,我们连接的wifi是以"direct"或者"DIRECT"开头的
			mLastInfo = mWifiManager.getConnectionInfo();
			if (mLastInfo != null) {
				String ssid = mLastInfo.getSSID();
				int rssi = mLastInfo.getRssi();
				if (rssi == Integer.MAX_VALUE) {
					mHandler.sendEmptyMessage(CONNECT_PINGUO_WIFI_FAIL);
					return;
				}
				if (ssid != null && ssid.toLowerCase().replaceAll("\"", "").startsWith("direct")) {
					mPINGUOWifiInfo = mLastInfo;
					if (state == DetailedState.CONNECTED) {
						Log.i(TAG, "连接成功");
						// 这里我们必须提前重置一次状态,预防多次调用
						mWifiState = PINGUO_WIFI_CONNECTED;
						// 我们提前移除runnable
						mHandler.removeCallbacks(mWifiConnectionMessage);
						mHandler.sendEmptyMessage(CONNECT_PINGUO_DEVICE);
					} else if (state == DetailedState.DISCONNECTED) {
						if (System.currentTimeMillis() - mWifiStartConnectTime > DELAY_CONNECT_FAIL) {
							mHandler.sendEmptyMessage(CONNECT_PINGUO_WIFI_FAIL);
						}
					} else if (state == DetailedState.FAILED) {
						if (System.currentTimeMillis() - mWifiStartConnectTime > DELAY_CONNECT_FAIL) {
							mHandler.sendEmptyMessage(CONNECT_PINGUO_WIFI_FAIL);
						}
					}
				} else {
					//这里会有问题出现,修改连接最大的失败时间
					Log.i(TAG, "updateConnectionState ##########ssid="+ ssid);
					if (state == DetailedState.DISCONNECTED) {
						if (System.currentTimeMillis() - mWifiStartConnectTime > DELAY_CONNECT_FAIL_MAX) {
							mHandler.sendEmptyMessage(CONNECT_PINGUO_WIFI_FAIL);
						}
					}
				}
			} else {
				if (state == DetailedState.DISCONNECTED) {
					if (System.currentTimeMillis() - mWifiStartConnectTime > DELAY_CONNECT_FAIL_MAX) {
						mHandler.sendEmptyMessage(CONNECT_PINGUO_WIFI_FAIL);
					}
				}
			}
			if (state == DetailedState.OBTAINING_IPADDR) {
				// mScanner.pause();
			} else {
				mScanner.resume();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "exception=" + e.toString());
		}
	}

	private void showDialog(List<AccessPoint> list) {
		try {
			if (mWifiDialog != null && mWifiDialog.isShowing()) {
				Log.i(TAG, "PINGUO wifi lsitview已经显示了111");
				return;
			}
			if (mWifiDialogs != null && mWifiDialogs.isShowing()) {
				Log.i(TAG, "PINGUO wifi lsitview已经显示了2222222");
				return;
			}
			mPINGUOAccessPoints.clear();
			mPINGUOAccessPoints.addAll(list);
			onCreateDialog(WIFI_DIALOG_IDS);
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "exception=" + e.toString());
		}
	}

	private void showDialog(AccessPoint accessPoint, boolean showErrorpassword) {
		try {
			if (mWifiDialog != null && mWifiDialog.isShowing()) {
				Log.i(TAG, "PINGUO wifi dialog 已经显示了");
				return;
			}
			if (mWifiDialogs != null && mWifiDialogs.isShowing()) {
				return;
			}
			mDlgAccessPoint = accessPoint;
			mWifiPasswordErrorPrompt = showErrorpassword;
			onCreateDialog(WIFI_DIALOG_ID);
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "exception=" + e.toString());
		}
	}

	// 预防重复调用clear()
	boolean mBtnClick = false;

	protected Dialog onCreateDialog(int id) {
		try {
			AlertDialog.Builder builder=new AlertDialog.Builder(mContext);
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mBtnClick = false;
			switch (id) {
			case WIFI_DIALOG_ID:
				View view = inflater.inflate(R.layout.wifi_dialog, null);
				final TextView ssid = (TextView) view.findViewById(R.id.pinguoSSID);
				ssid.setText(mDlgAccessPoint.ssid);
				final EditText passwordView = (EditText) view.findViewById(R.id.pinguoPassword);
				final CheckBox showPassword = (CheckBox) view.findViewById(R.id.show_password);
				final TextView passwordError = (TextView) view.findViewById(R.id.pinguoPasswordErrorPrompt);
				if (mWifiPasswordErrorPrompt) {
					if (passwordError.getVisibility() != View.VISIBLE) {
						passwordError.setVisibility(View.VISIBLE);
					}
				}
				showPassword.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						passwordView.setInputType(InputType.TYPE_CLASS_TEXT
								| (showPassword.isChecked() ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
										: InputType.TYPE_TEXT_VARIATION_PASSWORD));
					}
				});
				DialogInterface.OnClickListener postiveButtonListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mBtnClick = true;
						// 新增一个网络;
						WifiConfiguration config = getConfig(mDlgAccessPoint,passwordView);
						Log.i(TAG, "新增一个网络: config=" + config.networkId);
						int netId = mWifiManager.addNetwork(config);
						Log.i(TAG, "新增一个网络: addNetwork=" + netId);
						if (netId == -1) {
							if (mWifiDialog != null && mWifiDialog.isShowing()) {
								mWifiDialog.dismiss();
							}
							mWifiDialog = null;
							mHandler.sendEmptyMessage(CONNECT_PINGUO_WIFI_FAIL);
							return;
						}
						// 改变当前wifi的状态
						mWifiState = PINGUO_WIFI_CONNECTING;
						mHandler.postDelayed(mWifiConnectionMessage,WIFI_CONNECT_TIME);
						// 显示正在连接PINGUO wifi的进度条对话框
						mHandler.sendEmptyMessage(SHOW_DIALOG_CONNECT_WIFI);
						// 开始连接PINGUO wifi
						boolean enalbe = mWifiManager.enableNetwork(netId, true);
						Log.i(TAG, "新增一个网络: enalbe=" + enalbe);
						boolean saveConfig = mWifiManager.saveConfiguration();
						Log.i(TAG, "新增一个网络: saveConfig=" + saveConfig);
						boolean reconnect = mWifiManager.reconnect();
						Log.i(TAG, "新增一个网络: reconnect=" + reconnect);
						// 记录wifi开始连接的时间
						Log.i(TAG, "新增一个PINGUO wifi时记录wifi开始连接的时间");
						mWifiStartConnectTime = System.currentTimeMillis();
						if (mWifiManager.isWifiEnabled()) {
							mScanner.resume();
						}
						if (mWifiDialog != null && mWifiDialog.isShowing()) {
							mWifiDialog.dismiss();
						}
						mWifiDialog = null;
					}
				};
				DialogInterface.OnClickListener negativeButtonListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mBtnClick = true;
						if (mWifiDialog != null && mWifiDialog.isShowing()) {
							mWifiDialog.dismiss();
						}
						mWifiDialog = null;
						clear();
					}
				};
				builder.setView(view);
				builder.setPositiveButton("连接", postiveButtonListener);
				builder.setNegativeButton("取消", negativeButtonListener);
				mWifiDialog=builder.create();
				mWifiDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialog) {
								Log.i(TAG, "按了返回键");
								if (!mBtnClick) {
									if (mWifiDialog != null&& mWifiDialog.isShowing()) {
										mWifiDialog.dismiss();
									}
									mWifiDialog = null;
									clear();
								}
							}
						});
				mWifiDialog.setCanceledOnTouchOutside(false);
				mWifiDialog.setInverseBackgroundForced(true);
				mWifiDialog.setOnKeyListener(mSearchKeyListener);
				mWifiDialog.show();
				//修改密码框的默认行为
				final ColorStateList colors = mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).getTextColors();
				mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
				mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(0xffc2c2c2);
				passwordView.addTextChangedListener(new TextWatcher() {
					//ColorStateList colors = mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).getTextColors();
					@Override
					public void onTextChanged(CharSequence s, int start,int before, int count) {
						int length = s.toString().length();
						Log.i(TAG, "onTextChanged onTextChanged length="+ length);
						if (length >= 8) {
							mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
							mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(colors);
						} else {
							mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
							mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(0xffc2c2c2);
						}
						if (passwordError.getVisibility() == View.VISIBLE) {
							passwordError.setVisibility(View.GONE);
						}
					}
					@Override
					public void beforeTextChanged(CharSequence s, int start,int count, int after) {
						int length = s.toString().length();
						Log.i(TAG,"beforeTextChanged beforeTextChanged length="+ length);
						if (length >= 8) {
							mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
							mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(colors);
						} else {
							mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
							mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(0xffc2c2c2);
						}
						if (passwordError.getVisibility() == View.VISIBLE) {
							passwordError.setVisibility(View.GONE);
						}
					}
					@Override
					public void afterTextChanged(Editable s) {
						int length = s.toString().length();
						Log.i(TAG,"afterTextChanged afterTextChanged length="+ length);
						if (length >= 8) {
							mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
							mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(colors);
						} else {
							mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
							mWifiDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(0xffc2c2c2);
						}
						if (passwordError.getVisibility() == View.VISIBLE) {
							passwordError.setVisibility(View.GONE);
						}
					}
				});
				return mWifiDialog;
			case WIFI_DIALOG_IDS:
				android.content.DialogInterface.OnClickListener clickListener = new android.content.DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Log.i(TAG, "点击了wifi 列表的某一项 which=" + which);
						mBtnClick = true;
						if (mWifiDialogs != null && mWifiDialogs.isShowing()) {
							mWifiDialogs.dismiss();
						}
						mWifiDialogs = null;
						AccessPoint accessPoint = (AccessPoint) mPINGUOAccessPoints.get(which);
						connectPINGUOWifiAccessPoint(accessPoint);
					}
				};
				android.content.DialogInterface.OnCancelListener cancelListener = new android.content.DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						Log.i(TAG, "点击了wifi 列表的的cancel mBtnClick="+ mBtnClick);
						if (!mBtnClick) {
							if (mWifiDialogs != null&& mWifiDialogs.isShowing()) {
								mWifiDialogs.dismiss();
							}
							mWifiDialogs = null;
							clear();
						}

					}
				};
				builder.setTitle("");
				String[] entries = new String[mPINGUOAccessPoints.size()];
				for (int i = 0; i < mPINGUOAccessPoints.size(); i++) {
					entries[i] = mPINGUOAccessPoints.get(i).ssid;
				}
				builder.setSingleChoiceItems(entries, 0, clickListener);
				builder.setOnCancelListener(cancelListener);
				mWifiDialogs = builder.create();
				mWifiDialogs.setCanceledOnTouchOutside(false);
				mWifiDialogs.setOnKeyListener(mSearchKeyListener);
				mWifiDialogs.show();
				return mWifiDialogs;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "exception=" + e.toString());
		}
		return null;
	}

	// 新增一个AP的wifi配置项
	public WifiConfiguration getConfig(AccessPoint accessPoint,
			EditText passwordView) {
		try {
			int accessPointSecurity = (accessPoint == null) ? AccessPoint.SECURITY_NONE : accessPoint.security;
			if (accessPoint != null && accessPoint.networkId != -1) {
				return null;
			}
			WifiConfiguration config = new WifiConfiguration();
			if (accessPoint == null) {
				return null;
			} else if (accessPoint.networkId == -1) {
				Log.i(TAG, "新增一个网络时 网络的netWorkID=-1");
				config.SSID = AccessPoint.convertToQuotedString(accessPoint.ssid);
			} else {
				config.networkId = accessPoint.networkId;
			}
			switch (accessPointSecurity) {
			case AccessPoint.SECURITY_NONE:
				config.allowedKeyManagement.set(KeyMgmt.NONE);
				break;

			case AccessPoint.SECURITY_WEP:
				config.allowedKeyManagement.set(KeyMgmt.NONE);
				config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
				config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
				if (passwordView.length() != 0) {
					int length = passwordView.length();
					String password = passwordView.getText().toString();
					if ((length == 10 || length == 26 || length == 58) && password.matches("[0-9A-Fa-f]*")) {
						config.wepKeys[0] = password;
					} else {
						config.wepKeys[0] = '"' + password + '"';
					}
				}
				break;
			case AccessPoint.SECURITY_PSK:
				Log.i(TAG, "新增一个网络时 网的安全是密码");
				config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
				if (passwordView.length() != 0) {
					String password = passwordView.getText().toString();
					if (password.matches("[0-9A-Fa-f]{64}")) {
						config.preSharedKey = password;
					} else {
						config.preSharedKey = '"' + password + '"';
					}
				}
				break;

			case AccessPoint.SECURITY_EAP:
				config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
				config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
				break;

			default:
				return null;
			}
			return config;
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "exception=" + e.toString());
		}
		return null;
	}

	// 连接单个的wifi网络
	private void connectPINGUOWifiAccessPoint(AccessPoint accessPoint) {
		// 单个wifi ap的网络id不为-1
		if (accessPoint.networkId != -1) {
			//记录一次当前的连接点
			mDlgAccessPoint=accessPoint;
			int authCode = AuthParametersSave.getAutchCode(mContext,accessPoint.ssid.replaceAll("\"", ""));
			Log.i(TAG, "认证的 code=" + authCode);
			if (authCode == AuthParametersSave.ERROR_AUTHENTICATING) {
				AuthParametersSave.saveAutchCode(mContext,accessPoint.ssid.replaceAll("\"", ""),AuthParametersSave.SUCCESS_AUTHENTICATING);
				Log.i(TAG, "PINGUO wifi 认证失败");
				// 认证失败，比如密码错误,我们需要移除wifi的配置项
				mWifiManager.removeNetwork(accessPoint.networkId);
				mWifiManager.saveConfiguration();
				if (mWifiManager.isWifiEnabled()) {
					mScanner.resume();
				}
				// 认证失败，我们需要修改网络id以及密码类型
				accessPoint.setConfig(null);
				accessPoint.networkId = -1;
				accessPoint.security = AccessPoint.SECURITY_PSK;
				// 弹出用户连接ap的对话框
				showDialog(accessPoint, true);
				return;
			}
			// 获取wifi信号
			int rssi = accessPoint.getRssi();
			if (rssi == Integer.MAX_VALUE) {
				mHandler.sendEmptyMessage(CONNECT_PINGUO_WIFI_FAIL);
				return;
			}
			// 改变当前wifi的状态
			mWifiState = PINGUO_WIFI_CONNECTING;
			mHandler.postDelayed(mWifiConnectionMessage, WIFI_CONNECT_TIME);
			// 显示正在连接PINGUO wifi的进度条对话框
			mHandler.sendEmptyMessage(SHOW_DIALOG_CONNECT_WIFI);
			// 开始连接PINGUO wifi
			boolean enable = mWifiManager.enableNetwork(accessPoint.networkId,
					true);
			Log.i(TAG, "自动链接PINGUO wifi时enable=" + enable);
			// boolean save=mWifiManager.saveConfiguration();
			// Log.i(TAG, "自动链接PINGUO wifi时save="+save);
			//boolean reconnect = mWifiManager.reconnect();
			//Log.i(TAG, "自动链接PINGUO wifi时reconnect=" + reconnect);
			boolean reassociate=mWifiManager.reassociate();
			Log.i(TAG, "自动链接PINGUO wifi时reassociate=" + reassociate);
			// 记录wifi开始连接的时间
			Log.i(TAG, "自动链接PINGUO wifi时记录wifi开始连接的时间");
			mWifiStartConnectTime = System.currentTimeMillis();
			if (mWifiManager.isWifiEnabled()) {
				mScanner.resume();
			}
		} else {
			showDialog(accessPoint, false);
		}
	}

	public static final int CONNECT_PINGUO_DEVICE = 6666;
	public static final int CACEL_DIALOG_MESSAGE = 6667;
	public static final int SHOW_DIALOG_MESSAGE = 6668;
	public static final int CONNECT_PINGUO_WIFI_FAIL = 6669;
	public static final int CONNECT_PINGUO_WIFI_SUCCESS = 7000;
	public static final int SHOW_DIALOG_CONNECT_WIFI = 7001;
	public static final int CANCEL_DIALOG_CONNECT_WIFI = 7002;
	public static final int SHOW_DIALOG_NO_DEVICE = 7003;
	public static final int SCAN_PINGUO_WIF_FAIL = 7004;
	// Combo scans can take 5-6s to complete - set to 10s.
	private static final int WIFI_RESCAN_INTERVAL_MS = 5 * 1000;
	private static final int DELAY_CONNECT_FAIL = 10 * 1000;
	private static final int DELAY_CONNECT_FAIL_MAX = 30 * 1000;
	// wifi从打开到链接成功时间最坏的情况应该大概在20秒左右
	private static final int WIFI_CONNECT_TIME = 30 * 1000;
	// 扫描时间对话框的时间定格在10秒就足够了
	private static final int WIFI_SCANNER_DISMISS_DELAY = 10 * 1000;

	// wifi扫描类
	@SuppressLint("HandlerLeak")
	private class Scanner extends Handler {
		private int mRetry = 0;

		public Scanner(Looper loop) {
			super(loop);
		}

		public void resume() {
			try {
				if (!hasMessages(0)) {
					sendEmptyMessage(0);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.i(TAG, "exception=" + e.toString());
			}
		}

		public void forceScan() {
			try {
				removeMessages(0);
				sendEmptyMessage(0);
			} catch (Exception e) {
				e.printStackTrace();
				Log.i(TAG, "exception=" + e.toString());
			}
		}

		public void pause() {
			try {
				mRetry = 0;
				removeMessages(0);
			} catch (Exception e) {
				e.printStackTrace();
				Log.i(TAG, "exception=" + e.toString());
			}
		}

		@Override
		public void handleMessage(Message message) {
			try {
				if (mWifiManager.startScan()) {
					Log.e(TAG, "扫描wifi startScan=" + true);
					mRetry = 0;
				} else if (++mRetry >= 3) {
					mRetry = 0;
					Toast.makeText(mContext,"扫描wifi失败",Toast.LENGTH_LONG).show();
					return;
				}
				sendEmptyMessageDelayed(0, WIFI_RESCAN_INTERVAL_MS);
			} catch (Exception e) {
				e.printStackTrace();
				Log.i(TAG, "exception=" + e.toString());
			}
		}
	}
	private Runnable mWifiConnectionMessage = new Runnable() {

		@Override
		public void run() {
			// 防止扫描时间过长
			if (mHandler.hasMessages(CONNECT_PINGUO_DEVICE)) {
				mHandler.removeMessages(CONNECT_PINGUO_DEVICE);
			}
			if (mHandler.hasMessages(CONNECT_PINGUO_WIFI_FAIL)) {
				mHandler.removeMessages(CONNECT_PINGUO_WIFI_FAIL);
			}
			if (mHandler.hasMessages(CONNECT_PINGUO_WIFI_SUCCESS)) {
				mHandler.removeMessages(CONNECT_PINGUO_WIFI_SUCCESS);
			}
			mHandler.sendEmptyMessage(CONNECT_PINGUO_WIFI_FAIL);
		}
	};
	private Runnable mWifiSannerMessage = new Runnable() {

		@Override
		public void run() {
			mHandler.sendEmptyMessage(CACEL_DIALOG_MESSAGE);
			mHandler.sendEmptyMessage(SCAN_PINGUO_WIF_FAIL);
		}
	};

	// 在连接PINGUO wifi过程中按home键切换出去,我们需要做一些操作
	public void cancelEveryViewandVar() {
		// 当前处于扫描状态
		if (mWifiState == PINGUO_WIFI_SCANNER) {
			mHandler.removeCallbacks(mWifiSannerMessage);
			if (mScannerDialog != null && mScannerDialog.isShowing()) {
				mScannerDialog.cancel();
			}
			mScannerDialog = null;
			clear();
			return;
		}
		// 当前处于正在链接状态但是wifi列表已经显示
		if (mWifiState == PINGUO_WIFI_SHOW_DIAOG_OR_DIALOGS&& mWifiDialogs != null && mWifiDialogs.isShowing()) {
			mWifiDialogs.dismiss();
			mWifiDialogs = null;
			clear();
			return;
		}
		// 当前处于正在链接状态但是wifi dialog已经显示
		if (mWifiState == PINGUO_WIFI_SHOW_DIAOG_OR_DIALOGS&& mWifiDialog != null && mWifiDialog.isShowing()) {
			mWifiDialog.dismiss();
			mWifiDialog = null;
			clear();
			return;
		}
		// 当前处于正在链接状态
		if ((mWifiState == PINGUO_WIFI_CONNECTING && mWifiState != PINGUO_WIFI_CONNECTED_FAIL) || (mWifiState == PINGUO_WIFI_CONNECTING && mWifiState != PINGUO_WIFI_CONNECTED)) {
			mHandler.removeCallbacks(mWifiConnectionMessage);
			if (mProcessDialog != null && mProcessDialog.isShowing()) {
				mProcessDialog.cancel();
			}
			mProcessDialog = null;
			clear();
			return;
		}
		// 当前处于连接成功或者连接失败状态
		if (mWifiState == PINGUO_WIFI_CONNECTED_FAIL || mWifiState == PINGUO_WIFI_CONNECTED) {
			mHandler.removeCallbacks(mWifiConnectionMessage);
			if (mProcessDialog != null && mProcessDialog.isShowing()) {
				mProcessDialog.cancel();
			}
			mProcessDialog = null;
			clear();
			return;
		}
		// 下面是我暂时没有想到的状态或者其他异常状态
		clear();
	}

	private void dissMissAllDialog() {
		if (mScannerDialog != null && mScannerDialog.isShowing()) {
			mScannerDialog.cancel();
		}
		mScannerDialog = null;
		if (mWifiDialogs != null && mWifiDialogs.isShowing()) {
			mWifiDialogs.dismiss();
		}
		mWifiDialogs = null;
		if (mWifiDialog != null && mWifiDialog.isShowing()) {
			mWifiDialog.dismiss();
		}
		mWifiDialog = null;
		if (mProcessDialog != null && mProcessDialog.isShowing()) {
			mProcessDialog.cancel();
		}
		mProcessDialog = null;
		if(mNoPINGUODevice!=null && mNoPINGUODevice.isShowing()){
			mNoPINGUODevice.cancel();
		}
		mNoPINGUODevice=null;
	}

	private NetworkInterface getNetworkInterface() {
		String str1 = null;
		if ((mWifiManager != null) && (mWifiManager.isWifiEnabled())) {
			int i = mWifiManager.getConnectionInfo().getIpAddress();
			if (i != 0) {
				str1 = (0xFF & i >> 0) + "." + (0xFF & i >> 8) + "."+ (0xFF & i >> 16) + "." + (0xFF & i >> 24);
			}
			Log.i(TAG, "连接的ip地址: str1=" + str1);
		}
		try {
			NetworkInterface localNetworkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(str1));
			return localNetworkInterface;
		} catch (SocketException localSocketException) {
			localSocketException.printStackTrace();
		} catch (UnknownHostException localUnknownHostException) {
			localUnknownHostException.printStackTrace();
		}
		return null;
	}
}