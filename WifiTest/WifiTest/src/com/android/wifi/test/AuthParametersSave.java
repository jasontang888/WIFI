package com.android.wifi.test;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthParametersSave {
	public static final String AUTHENTICATING="auth";
	public static final int ERROR_AUTHENTICATING = 1;
	public static final int SUCCESS_AUTHENTICATING = -1;
    public static void saveAutchCode(Context context,String name,int code){
    	SharedPreferences sp=context.getSharedPreferences(name, Context.MODE_PRIVATE);
    	SharedPreferences.Editor editor=sp.edit();
    	editor.putInt(AUTHENTICATING, code);
    	editor.commit();
    }
    public static int getAutchCode(Context context,String name){
    	SharedPreferences sp=context.getSharedPreferences(name, Context.MODE_PRIVATE);
    	return sp.getInt(AUTHENTICATING, SUCCESS_AUTHENTICATING);
    }
}
