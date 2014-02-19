package com.studioirregular.testinappbilling;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.studioirregular.libinappbilling.IabException;
import com.studioirregular.libinappbilling.InAppBilling;
import com.studioirregular.libinappbilling.InAppBilling.NotSupportedException;
import com.studioirregular.libinappbilling.InAppBilling.ServiceNotReadyException;
import com.studioirregular.libinappbilling.Product;
import com.studioirregular.libinappbilling.PurchasedItem;
import com.studioirregular.libinappbilling.SignatureVerificationException;

public class TestInAppBilling extends Activity {

	private static final String TAG = "TestInAppBilling";
	
	private static final String PUBLIC_KEY_BASE64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqkBnzFjK8gqiRPkxc7QMFP9O5DClW8sX0I4cBfJD03/8ZvdP8piwq/2bAAvk9aEtaVcu6dmX2lkcRI8q/KeIdgYxWxr+XBzqoGRJe3tg89FCPl3i1M+p6lW2bjpu2Wa48PmUOpR6aSFC19P6qwu/h2uf5LcvbQuAUBGOj+iDu3V5HayFZy+Ak/rTg249uzB0IOWcFpx9xfqZtLvPbyYbiES9PrNkLXYoHgtSG+14CzqLLWy5/4yik2RQzHu8lRGY36CaFzuH9JHbWEfJrqNkK2JqKjEVrsEecC57UxOLqoFGeoIZyKdYBmJ+6b9E5j3AtOaenVPjEmyw3IgaQD+fiQIDAQAB";
	
	private static String[] PRODUCT_ID_LIST = {
		"com.studioirregular.testinappbilling.syphon",
		"com.studioirregular.testinappbilling.coffee.bluemountain",
		"com.studioirregular.testinappbilling.coffee.java",
	};
	
	private static final int PURCHASE_ACITIVITY_CODE = 1001;
	
	private InAppBilling iab;
	private InAppBilling.ReadyCallback iabReady = new InAppBilling.ReadyCallback() {

		@Override
		public void onIABReady() {
			updatePurchasedProducts();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setupStaticProductUI();
		setupPurchaseProductUI();
		setupGetSkuDetailButton(R.id.get_sku_details);
		setupPurchasedProductsListView();
		
		iab = new InAppBilling(PUBLIC_KEY_BASE64, iabReady);
		iab.open(TestInAppBilling.this);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (iab != null) {
			iab.close();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		if (item.getItemId() == R.id.action_test_coin) {
			Intent testCoin = new Intent(TestInAppBilling.this, TestCoins.class);
			startActivity(testCoin);
			return true;
		}
		
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		Log.d(TAG, "onActivityResult requestCode:" + requestCode
				+ ",resultCode:" + resultCode + ",data:" + data);
		
		if (requestCode == PURCHASE_ACITIVITY_CODE) {
			
			if (resultCode == Activity.RESULT_OK) {
				try {
					PurchasedItem item = iab.onPurchaseActivityResult(data);
					Log.w(TAG, "Purchase item:" + item);
					if (item != null) {
						updatePurchasedProducts();
					}
				} catch (IabException e) {
					e.printStackTrace();
				} catch (SignatureVerificationException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			} else if (resultCode == Activity.RESULT_CANCELED) {
				Log.w(TAG, "onActivityResult: user canceled purchasing.");
				Toast.makeText(TestInAppBilling.this, R.string.purchase_canceled, Toast.LENGTH_LONG).show();
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void testPurchase(Product.Type type, String id) {
		try {
			iab.purchase(type, id, TestInAppBilling.this,
					PURCHASE_ACITIVITY_CODE);
		} catch (NotSupportedException e) {
			e.printStackTrace();
		} catch (ServiceNotReadyException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (IabException e) {
			e.printStackTrace();
		} catch (SendIntentException e) {
			e.printStackTrace();
		}
	}
	
	private void testConsume(Product.Type type, String id) {
		
		try {
			iab.consume(type, id);
			removeProductFromPurchasedList(R.id.purchased_list, id);
		} catch (NotSupportedException e) {
			e.printStackTrace();
		} catch (ServiceNotReadyException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (IabException e) {
			e.printStackTrace();
		}
	}
	
	private void testGetSkuDetails(Product.Type type, String[] productIds) {
		
		Log.d(TAG, "testGetSkuDetails productIds:");
		for (String id : productIds) {
			Log.d(TAG, "\t" + id);
		}
		
		List<Product> products = null;
		
		try {
			products = iab.getProductDetails(type, PRODUCT_ID_LIST);
		} catch (ServiceNotReadyException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		
		if (products != null) {
			for (Product product : products) {
				Log.d(TAG, product.toString());
				Log.d(TAG, "=====================================");
			}
		}
	}
	
	private void setupStaticProductUI() {
		
		Spinner spinner = (Spinner)findViewById(R.id.static_product_spinner);
		
		if (spinner != null) {
			
			CharSequence[] strings = getResources().getTextArray(R.array.static_response_products);
			LongStringArrayAdapter<CharSequence> adapter = new LongStringArrayAdapter<CharSequence>(
					TestInAppBilling.this, android.R.layout.simple_spinner_item,
					strings);
			
			adapter.setDropDownViewResource(
					android.R.layout.simple_spinner_dropdown_item);
			
			spinner.setAdapter(adapter);
		}
		
		View purchase = findViewById(R.id.purchase_static);
		if (purchase != null) {
			purchase.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
					final String id = getChosenProductId(R.id.static_product_spinner);
					if (id != null) {
						testPurchase(Product.Type.ONE_TIME_PURCHASE, id);
					}
				}
			});
		}
		
		View consume = findViewById(R.id.consume_static);
		if (consume != null) {
			consume.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
					final String id = getChosenProductId(R.id.static_product_spinner);
					if (id != null) {
						testConsume(Product.Type.ONE_TIME_PURCHASE, id);
					}
				}
			});
		}
	}
	
	private void setupPurchaseProductUI() {
		
		Spinner spinner = (Spinner)findViewById(R.id.product_spinner);
		
		if (spinner != null) {
			
			CharSequence[] strings = getResources().getTextArray(R.array.product_list);
			
			LongStringArrayAdapter<CharSequence> adapter = new LongStringArrayAdapter<CharSequence>(
					TestInAppBilling.this, android.R.layout.simple_spinner_item,
					strings);
			
			adapter.setDropDownViewResource(
					android.R.layout.simple_spinner_dropdown_item);
			
			spinner.setAdapter(adapter);
		}
		
		View purchase = findViewById(R.id.purchase);
		if (purchase != null) {
			purchase.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
					final String id = getChosenProductId(R.id.product_spinner);
					if (id != null) {
						testPurchase(Product.Type.ONE_TIME_PURCHASE, id);
					}
				}
			});
		}
		
		View consume = findViewById(R.id.consume);
		if (consume != null) {
			consume.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
					final String id = getChosenProductId(R.id.product_spinner);
					if (id != null) {
						testConsume(Product.Type.ONE_TIME_PURCHASE, id);
					}
				}
			});
		}
	}
	
	private void setupGetSkuDetailButton(int buttonId) {
		
		View button = findViewById(buttonId);
		if (button != null) {
			button.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					testGetSkuDetails(Product.Type.ONE_TIME_PURCHASE, PRODUCT_ID_LIST);
				}
			});
		}
	}
	
	private void setupPurchasedProductsListView() {
		
		ListView lv = (ListView)findViewById(R.id.purchased_list);
		
		if (lv != null) {
			LongStringArrayAdapter<CharSequence> adapter = 
					new LongStringArrayAdapter<CharSequence>(TestInAppBilling.this, 
							android.R.layout.simple_list_item_1);
			
			adapter.setDropDownViewResource(
					android.R.layout.simple_spinner_dropdown_item);
			
			lv.setAdapter(adapter);
		}
	}
	
	private String getChosenProductId(int spinnerId) {
		
		Spinner spinner = (Spinner)findViewById(spinnerId);
		
		if (spinner == null) {
			return null;
		}
		
		final String id = (String)spinner.getSelectedItem();
		Log.d(TAG, "getChosenProductId id:" + id);
		return id;
	}
	
	private void updatePurchasedProducts() {
		
		Log.w(TAG, "updatePurchasedProducts");
		
		List<PurchasedItem> purchases = iab.getPurchasedProducts(Product.Type.ONE_TIME_PURCHASE);
		
		showPurchasedProducsToListView(R.id.purchased_list, purchases);
	}
	
	private void showPurchasedProducsToListView(int listViewId,
			List<PurchasedItem> list) {
		
		ListView lv = (ListView)findViewById(listViewId);
		if (lv != null) {
			
			ListAdapter adapter = lv.getAdapter();
			if (adapter != null && adapter instanceof ArrayAdapter) {
				
				@SuppressWarnings("unchecked")
				ArrayAdapter<String> aa = (ArrayAdapter<String>)adapter;
				
				aa.clear();
				aa.addAll(parseProductIdToList(list));
				
				aa.notifyDataSetChanged();
			}
		}
	}
	
	private List<String> parseProductIdToList(List<PurchasedItem> list) {
		
		List<String> result = new ArrayList<String>();
		
		for (PurchasedItem item : list) {
			result.add(item.productId);
		}
		
		return result;
	}
	
	private void removeProductFromPurchasedList(int listViewId, String productId) {
		
		ListView lv = (ListView)findViewById(listViewId);
		if (lv != null) {
			
			ListAdapter adapter = lv.getAdapter();
			if (adapter != null && adapter instanceof ArrayAdapter) {
				@SuppressWarnings("unchecked")
				ArrayAdapter<String> aa = (ArrayAdapter<String>)adapter;
				
				aa.remove(productId);
				
				aa.notifyDataSetChanged();
			}
		}
	}
}
