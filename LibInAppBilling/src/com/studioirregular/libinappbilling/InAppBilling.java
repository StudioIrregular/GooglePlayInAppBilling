package com.studioirregular.libinappbilling;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.json.JSONException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;
import com.studioirregular.libinappbilling.Product.ParsingException;

public class InAppBilling {

	private static final boolean DEBUG_LOG = true;
	
	private static final int USING_IAB_VERSION = 3;
	
	private final String PUBLIC_KEY;
	
	private Context context;
	private IabServiceConnection serviceConnection;
	
	public interface ReadyCallback {
		
		public void onIABReady();
	}
	
	private ReadyCallback readyCallback;
	
	public InAppBilling(String publicKeyBase64, ReadyCallback readyCallback) {
		this.PUBLIC_KEY = publicKeyBase64;
		this.readyCallback = readyCallback;
	}
	
	public boolean isReady() {
		try {
			return getService() != null;
		} catch (ServiceNotReadyException e) {
			;
		}
		return false;
	}
	
	public void open(Context context) {
		
		if (DEBUG_LOG) {
			Log.d(Constants.LOG_TAG, "InAppBilling::open");
		}
		
		this.context = context;
		this.serviceConnection = new IabServiceConnection(context);
		this.serviceConnection.observeConnectionStatus(serviceConnectionObserver);
		this.serviceConnection.bindService();
	}
	
	public void close() {
		
		if (DEBUG_LOG) {
			Log.d(Constants.LOG_TAG, "InAppBilling::close");
		}
		
		if (this.serviceConnection != null) {
			this.serviceConnection.unbindService();
			this.serviceConnection.unregisterConnectionStatus(serviceConnectionObserver);
			this.serviceConnection = null;
		}
		this.context = null;
		this.readyCallback = null;
	}
	
	@SuppressWarnings("serial")
	public class ServiceNotReadyException extends RuntimeException {
		
		public ServiceNotReadyException(String details) {
			super(details);
		}
	}
	
	@SuppressWarnings("serial")
	public class NotSupportedException extends RuntimeException {
		
		public NotSupportedException(String details) {
			super(details);
		}
	}
	
	public List<PurchasedItem> getPurchasedProducts(Product.Type type)
			throws ServiceNotReadyException, NotSupportedException {
		
		if (DEBUG_LOG) {
			Log.d(Constants.LOG_TAG, "InAppBilling::getPurchasedProducts type:" + type);
		}
		
		if (!isIabSupported(getService(), type)) {
			throw new NotSupportedException(
					"In app billing not supported for product type:" + type);
		}		
		
		Bundle purchases = getPurchases(getService(), type);
		if (DEBUG_LOG) {
			for (String key : purchases.keySet()) {
				Log.d(Constants.LOG_TAG,
						"\tpurchases[" + key + "]:" + purchases.get(key));
			}
		}
		
		return processGetPurchasesResult(purchases);
	}
	
	public boolean isProductPurchased(Product.Type type, String productId)
			throws ServiceNotReadyException, NotSupportedException {
		
		if (DEBUG_LOG) {
			Log.d(Constants.LOG_TAG, "InAppBilling::getPurchasedProducts type:"
					+ type + ",productId:" + productId);
		}
		
		List<PurchasedItem> productList = getPurchasedProducts(type);
		
		for (PurchasedItem item : productList) {
			if (productId.equals(item.productId)) {
				return true;
			}
		}
		
		return false;
	}
	
	public void purchase(Product.Type type, String productId,
			Activity activity, int activityRequestCode)
			throws ServiceNotReadyException, NotSupportedException, SendIntentException {
		
		if (DEBUG_LOG) {
			Log.d(Constants.LOG_TAG, "InAppBilling::purchase type:" + type
					+ ",productId:" + productId);
		}
		
		if (!isIabSupported(getService(), type)) {
			throw new NotSupportedException(
					"In app billing not supported for product type:" + type);
		}
		
		PendingIntent intent = getPurchaseIntent(type, productId);
		if (DEBUG_LOG) {
			Log.w(Constants.LOG_TAG, "purchaseIntent:" + intent);
		}
		
		activity.startIntentSenderForResult(intent.getIntentSender(),
				activityRequestCode, new Intent(), Integer.valueOf(0),
				Integer.valueOf(0), Integer.valueOf(0));
	}
	
	public boolean consume(Product.Type type, String productId)
			throws ServiceNotReadyException, NotSupportedException {
		
		if (DEBUG_LOG) {
			Log.d(Constants.LOG_TAG, "InAppBilling::consume type:" + type
					+ ",productId:" + productId);
		}
		
		List<PurchasedItem> purchases = getPurchasedProducts(type);
		
		final String purchaseToken = getPurchaseToken(purchases, productId);
		if (purchaseToken == null) {
			if (DEBUG_LOG) {
				Log.e(Constants.LOG_TAG,
						"InAppBilling::consume: cannot get valid purchase token for product:"
								+ productId);
			}
			return false;
		}
		
		API_consumePurchase api = new API_consumePurchase();
		
		api.setApiVersion(3);
		api.setPackageName(context.getPackageName());
		api.setPurchaseToken(purchaseToken);
		
		try {
			ServerResponseCode response = api.execute(getService());
			
			return response.isOK();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public List<Product> getProductDetails(Product.Type type,
			String[] productIds) throws ServiceNotReadyException {
		
		if (DEBUG_LOG) {
			Log.d(Constants.LOG_TAG, "InAppBilling::getProductDetails type:"
					+ type + ",products:");
			for (String id : productIds) {
				Log.d(Constants.LOG_TAG, "\t" + id);
			}
		}
		
		API_getSkuDetails api = new API_getSkuDetails();
		
		api.setApiVersion(3);
		api.setPackageName(context.getPackageName());
		api.setProductType(Product.Type.ONE_TIME_PURCHASE);
		
		for (String id : productIds) {
			api.addQueryProductId(id);
		}
		
		Bundle bundle = null;
		try {
			bundle = api.execute(getService());
		} catch (IllegalArgumentException e) {
			if (DEBUG_LOG) {
				e.printStackTrace();
			}
			return new ArrayList<Product>();
		} catch (RemoteException e) {
			if (DEBUG_LOG) {
				e.printStackTrace();
			}
			return new ArrayList<Product>();
		}
		
		try {
			return Product.parse(bundle);
		} catch (ParsingException e) {
			if (DEBUG_LOG) {
				e.printStackTrace();
			}
		}
		
		return new ArrayList<Product>();
	}
	
	/*
	 * Pass onActivityResult to me if request code equals the one client pass to
	 * purchase API. When user canceled the purchase, client can get this info
	 * through result code or ServerResponseCode.
	 */
	public ServerResponseCode onPurchaseActivityResult(int resultCode, Intent data) {
		
		if (DEBUG_LOG) {
			Log.d(Constants.LOG_TAG,
					"InAppBilling::onActivityResult resultCode:" + resultCode
							+ ",data:" + data);
		}
		
		if (data == null) {
			return new ServerResponseCode(ServerResponseCode.INVALID_CODE_VALUE);
		}
		
		HandlePurchaseActivityResult helper = null;
		try {
			helper = new HandlePurchaseActivityResult(data);
		} catch (IllegalArgumentException e) {
			if (DEBUG_LOG) {
				e.printStackTrace();
			}
			return new ServerResponseCode(ServerResponseCode.INVALID_CODE_VALUE);
		}
		
		return helper.getResponseCode();
	}
	
	private Observer serviceConnectionObserver = new Observer() {
		
		@Override
		public void update(Observable observable, Object data) {
			
			if (DEBUG_LOG) {
				Log.d(Constants.LOG_TAG,
						"InAppBilling::serviceConnectionObserver::update data:" + data);
			}
			
			final Boolean connected = (Boolean)data;
			
			if (connected) {
				onServiceConnected();
			} else {
				onServiceDisconnected();
			}
		}
	};
	
	private void onServiceConnected() {
		
		if (DEBUG_LOG) {
			Log.d(Constants.LOG_TAG, "onServiceConnected");
		}
		
		if (readyCallback != null) {
			readyCallback.onIABReady();
		}
	}
	
	private void onServiceDisconnected() {
		
		if (DEBUG_LOG) {
			Log.d(Constants.LOG_TAG, "onServiceDisconnected");
		}
		
	}
	
	private IInAppBillingService getService() throws ServiceNotReadyException {
		
		if (serviceConnection == null) {
			throw new ServiceNotReadyException("No service connection object.");
		}
		
		IInAppBillingService result = serviceConnection.getService();
		
		if (result == null) {
			throw new ServiceNotReadyException("Service not connected.");
		}
		
		return result;
	}
	
	private boolean isIabSupported(IInAppBillingService service, Product.Type type) {
		
		API_isBillingSupported api = new API_isBillingSupported();
		api.setApiVersion(USING_IAB_VERSION);
		api.setPackageName(context.getPackageName());
		api.setProductType(type);
		
		ServerResponseCode response = null;
		
		try {
			response = api.execute(service);
			
			if (response == null) {
				if (DEBUG_LOG) {
					Log.e(Constants.LOG_TAG,
							"InAppBilling::isIabSupported no response from API all.");
				}
			} else if (response.isOK()) {
				return true;
			} else {
				if (DEBUG_LOG) {
					Log.e(Constants.LOG_TAG,
							"InAppBilling::isIabSupported response:"
									+ response.value);
				}
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	private Bundle getPurchases(IInAppBillingService service, Product.Type type) {
		
		API_getPurchases api = new API_getPurchases();
		
		api.setApiVersion(USING_IAB_VERSION);
		api.setPackageName(context.getPackageName());
		api.setProductType(Product.Type.ONE_TIME_PURCHASE);
		api.setContinuationToken(null);
		
		Bundle result = null;
		try {
			result = api.execute(service);
		} catch (IllegalArgumentException e) {
			if (DEBUG_LOG) {
				e.printStackTrace();
			}
		} catch (RemoteException e) {
			if (DEBUG_LOG) {
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	private List<PurchasedItem> processGetPurchasesResult(Bundle bundle) {
		
		try {
			HandleGetPurchasesResult helper = new HandleGetPurchasesResult(
					PUBLIC_KEY, bundle);
			helper.execute();
			return helper.getPurchasedItemList();
		} catch (IllegalArgumentException e) {
			if (DEBUG_LOG) {
				Log.e(Constants.LOG_TAG,
						"processGetPurchasesResult: invalid bundle:"
								+ bundle);
				e.printStackTrace();
			}
		} catch (JSONException e) {
			if (DEBUG_LOG) {
				e.printStackTrace();
			}
		} catch (SignatureVerificationException e) {
			if (DEBUG_LOG) {
				e.printStackTrace();
			}
		}
		
		return new ArrayList<PurchasedItem>();
	}
	
	private PendingIntent getPurchaseIntent(Product.Type type, String productId)
			throws ServiceNotReadyException {
		
		API_getBuyIntent api = new API_getBuyIntent();
		api.setApiVersion(3);
		api.setPackageName(context.getPackageName());
		api.setProductId(productId);
		api.setProductType(Product.Type.ONE_TIME_PURCHASE);
		
		Bundle bundle = null;
		try {
			bundle = api.execute(getService());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		if (bundle == null) {
			Log.e(Constants.LOG_TAG,
					"InAppBilling::getPurchaseIntent invalid bundle:"
							+ bundle);
			return null;
		}
		
		HandleGetBuyIntentResult helper = new HandleGetBuyIntentResult(bundle);
		if (!helper.isApiCallSuccess()) {
			Log.e(Constants.LOG_TAG,
					"InAppBilling::getPurchaseIntent got error response:"
							+ helper.getResponseCode());
			return null;
		}
		
		return helper.getPurchaseIntent();
	}
	
	private String getPurchaseToken(List<PurchasedItem> list, String productId) {
		
		for (PurchasedItem item : list) {
			final String id = item.productId;
			if (id != null && id.equals(productId)) {
				
				PurchasedItem.PurchaseState state = item.purchaseState;
				if (state == PurchasedItem.PurchaseState.PURCHASED) {
					return item.purchaseToken;
				} else {
					if (DEBUG_LOG) {
						Log.w(Constants.LOG_TAG,
								"getPurchaseToken product invalid purchase state:"
										+ state);
					}
					return null;
				}
			}
		}
		return null;
	}

}
