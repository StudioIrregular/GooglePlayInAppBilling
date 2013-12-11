package com.studioirregular.libinappbilling;

import android.app.PendingIntent;
import android.os.Bundle;
import android.util.Log;

public class BuyIntent {

	private static final boolean DEBUG_LOG = true;
	
	public BuyIntent(Bundle apiResult) {
		
		if (DEBUG_LOG) {
			Log.d(Constants.LOG_TAG, "BuyIntent: apiResult:" + apiResult);
		}
		
		this.apiResult = apiResult;
	}
	
	public ServerResponseCode getServerResponseCode() {
		
		return new ServerResponseCode(apiResult);
	}
	
	public PendingIntent getIntent() {
		
		return apiResult.getParcelable("BUY_INTENT");
	}
	
	private Bundle apiResult;
}
