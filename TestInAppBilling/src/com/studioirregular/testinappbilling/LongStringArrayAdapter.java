package com.studioirregular.testinappbilling;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class LongStringArrayAdapter<T> extends ArrayAdapter<T> {

	private static final String TAG = "LongStringArrayAdapter";
	
	public LongStringArrayAdapter(Context context, int resource) {
		super(context, resource);
	}
	
	public LongStringArrayAdapter(Context context, int resource, T[] objects) {
		super(context, resource, objects);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View result = super.getView(position, convertView, parent);
		
		Log.d(TAG, "getView result:" + result);
		
		if (result instanceof TextView) {
			((TextView)result).setSingleLine();
			((TextView)result).setEllipsize(TextUtils.TruncateAt.START);
		}
		
		return result;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		
		View result = super.getDropDownView(position, convertView, parent);
		
		if (result instanceof TextView) {
			((TextView)result).setSingleLine();
			((TextView)result).setEllipsize(TextUtils.TruncateAt.START);
		}
		
		return result;
	}

}
