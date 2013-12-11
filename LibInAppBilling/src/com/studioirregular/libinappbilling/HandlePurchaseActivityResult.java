package com.studioirregular.libinappbilling;

import org.json.JSONException;

import android.content.Intent;
import android.util.Log;

/*
 * This class process Intent data in onActivityResult (results from startActivity with 
 * pending intent from IAB API: getBuyIntent).
 */
public class HandlePurchaseActivityResult {

	private static final boolean DEBUG_LOG = true;
	
	private Intent data;
	
	public HandlePurchaseActivityResult(Intent data)
			throws IllegalArgumentException {
		
		if (DEBUG_LOG) {
			Log.d(Constants.LOG_TAG, "HandlePurchaseActivityResult");
		}
		
		verifyInput(data);
		
		this.data = data;
	}
	
	public boolean isPurchaseSuccessful() {
		
		if (data == null) {
			
			if (DEBUG_LOG) {
				Log.e(Constants.LOG_TAG,
						"HandlePurchaseActivityResult::isPurchaseSuccessful: invalid intent data:" + data);
			}
			return false;
		}
		
		ServerResponseCode response = new ServerResponseCode(data);
		
		if (!response.isOK()) {
			if (DEBUG_LOG) {
				Log.e(Constants.LOG_TAG,
						"HandlePurchaseActivityResult::isPurchaseSuccessful: response code not ok, code:" + response.value);
			}
			return false;
		}
		
		if (DEBUG_LOG) {
			Log.d(Constants.LOG_TAG,
					"HandlePurchaseActivityResult::isPurchaseSuccessful: true");
		}
		
		return true;
	}
	
	public ServerResponseCode getResponseCode() {
		
		return new ServerResponseCode(data);
	}
	
	public PurchasedItem getPurchasedItem(String publicKeyBase64)
			throws SignatureVerificationException, JSONException {
		
		if (DEBUG_LOG) {
			Log.d(Constants.LOG_TAG,
					"HandlePurchaseActivityResult::getPurchasedItem: publicKeyBase64:"
							+ publicKeyBase64);
		}
		
		final String purchaseData = data.getExtras().getString(KEY_PURCHASE_DATA);
		final String signature = data.getExtras().getString(KEY_DATA_SIGNATURE);
		
		final boolean pass = Security.verifyPurchase(
				publicKeyBase64, purchaseData, signature);
		
		if (!pass) {
			throw new SignatureVerificationException(
					"getPurchasedItem: verify signature failed.");
		}		
		
		return new PurchasedItem(purchaseData);
	}
	
	private void verifyInput(Intent data) throws IllegalArgumentException {
		
		if (data == null) {
			throw new IllegalArgumentException("Invalid intent data:" + data);
		}
		
		if (!data.hasExtra(KEY_PURCHASE_DATA)) {
			throw new IllegalArgumentException("Expect intent extra has key:"
					+ KEY_PURCHASE_DATA);
		}
		
		if (!data.hasExtra(KEY_DATA_SIGNATURE)) {
			throw new IllegalArgumentException("Expect intent extra has key:"
					+ KEY_DATA_SIGNATURE);
		}
	}
	
	private static final String KEY_PURCHASE_DATA  = "INAPP_PURCHASE_DATA";
	private static final String KEY_DATA_SIGNATURE = "INAPP_DATA_SIGNATURE";
}
