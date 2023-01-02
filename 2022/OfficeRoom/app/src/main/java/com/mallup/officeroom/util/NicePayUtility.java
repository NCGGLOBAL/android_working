package com.mallup.officeroom.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NicePayUtility {
	
	public static String getyyyyMMddHHmmss(){
		/** yyyyMMddHHmmss Date Format */
		SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss");
		return yyyyMMddHHmmss.format(new Date());
	}

	
	public static String encrypt(String strData) { // ��ȣȭ ��ų ������
		String strOUTData = "";
		try {
		   MessageDigest md = MessageDigest.getInstance("MD5"); // "MD5 �������� ��ȣȭ"
		   
		   md.reset();
		   //byte[] bytData = strData.getBytes();  
		   //md.update(bytData);
		   md.update(strData.getBytes());
		    byte[] digest = md.digest();

		  StringBuffer hashedpasswd = new StringBuffer();
		    String hx;
		    
		    for (int i=0;i<digest.length;i++){
		    	hx =  Integer.toHexString(0xFF & digest[i]);
		    	//0x03 is equal to 0x3, but we need 0x03 for our md5sum
		    	if(hx.length() == 1){hx = "0" + hx;}
		    	hashedpasswd.append(hx);
		    	
		    }
		    strOUTData = hashedpasswd.toString();
		    byte[] raw = strOUTData.getBytes(); 
		    byte[] encodedBytes = Base64.encode(raw, 0);
		    strOUTData = new String(encodedBytes);
		    //strOUTData = new String(raw);
		   }
		   catch (NoSuchAlgorithmException e) {
			   
		  }
		  return strOUTData;  // ��ȣȭ�� �����͸� ����...
	}
	
	public static void AlertDialog(String title, String message, Context context)
	{
		AlertDialog.Builder ab = null;
	    ab = new AlertDialog.Builder( context );
	    ab.setMessage(message);
	    ab.setPositiveButton(android.R.string.ok, null);
	    ab.setTitle(title);
	    ab.show();
	}
	
	
	public static boolean isPackageInstalled(Context ctx, String pkgName) {
		
		
		try {
		ctx.getPackageManager().getPackageInfo(pkgName, PackageManager.GET_ACTIVITIES);
		} catch (NameNotFoundException e) {
		e.printStackTrace();
		return false;
		}
		return true;
	}
	
	
}
