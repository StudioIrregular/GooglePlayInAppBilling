package com.studioirregular.testinappbilling;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.studioirregular.libinappbilling.IabException;
import com.studioirregular.libinappbilling.InAppBilling;
import com.studioirregular.libinappbilling.InAppBilling.NotSupportedException;
import com.studioirregular.libinappbilling.InAppBilling.ServiceNotReadyException;
import com.studioirregular.libinappbilling.Product;
import com.studioirregular.libinappbilling.PurchasedItem;
import com.studioirregular.libinappbilling.ServerResponseCode;
import com.studioirregular.libinappbilling.SignatureVerificationException;

public class TestCoins extends Activity {

	private static final String TAG = "TestCoins";
	
	private static class MyCoin {
		public final String id;
		public final int amount;
		
		public MyCoin(String id, int amount) {
			this.id = id;
			this.amount = amount;
		}
	}
	
	private static List<MyCoin> Coins = new ArrayList<MyCoin>();
	static {
		Coins.add(new MyCoin("com.studioirregular.testinappbilling.coffee.coin1", 10));
		Coins.add(new MyCoin("com.studioirregular.testinappbilling.coffee.coin2", 100));
		Coins.add(new MyCoin("com.studioirregular.testinappbilling.coffee.coin3", 1000));
	};
	
	private InAppBilling iab;
	
	private static final String PUBLIC_KEY_BASE64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqkBnzFjK8gqiRPkxc7QMFP9O5DClW8sX0I4cBfJD03/8ZvdP8piwq/2bAAvk9aEtaVcu6dmX2lkcRI8q/KeIdgYxWxr+XBzqoGRJe3tg89FCPl3i1M+p6lW2bjpu2Wa48PmUOpR6aSFC19P6qwu/h2uf5LcvbQuAUBGOj+iDu3V5HayFZy+Ak/rTg249uzB0IOWcFpx9xfqZtLvPbyYbiES9PrNkLXYoHgtSG+14CzqLLWy5/4yik2RQzHu8lRGY36CaFzuH9JHbWEfJrqNkK2JqKjEVrsEecC57UxOLqoFGeoIZyKdYBmJ+6b9E5j3AtOaenVPjEmyw3IgaQD+fiQIDAQAB";
	
	private static final int PURCHASE_ACITIVITY_CODE = 1001;
	
	private int coins = 0;
	private TextView coinView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test_coin);
		
		iab = new InAppBilling(PUBLIC_KEY_BASE64, null);
		iab.open(TestCoins.this);
		
		coinView = (TextView)findViewById(R.id.coin);
		
		setupCoinButton(R.id.buy_coin_1, Coins.get(0));
		setupCoinButton(R.id.buy_coin_2, Coins.get(1));
		setupCoinButton(R.id.buy_coin_3, Coins.get(2));
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (iab != null) {
			iab.close();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		Log.d(TAG, "onActivityResult requestCode:" + requestCode
				+ ",resultCode:" + resultCode + ",data:" + data);
		
		if (requestCode == PURCHASE_ACITIVITY_CODE) {
			
			if (resultCode == Activity.RESULT_OK) {
				try {
					PurchasedItem item = iab.onPurchaseActivityResult(data);
					consumeCoinProduct(item.productId);
				} catch (IabException e) {
					e.printStackTrace();
				} catch (SignatureVerificationException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else if (resultCode == Activity.RESULT_CANCELED) {
				Log.w(TAG, "onActivityResult: user canceled purchasing.");
				Toast.makeText(TestCoins.this, R.string.purchase_canceled, Toast.LENGTH_LONG).show();
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	private void setupCoinButton(int buttonId, final MyCoin coin) {
		
		View button = findViewById(buttonId);
		if (button == null) {
			return;
		}
		
		button.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				buyCoin(coin.id);
			}
		});
	}
	
	private void buyCoin(String productId) {
		
		try {
			iab.purchase(Product.Type.ONE_TIME_PURCHASE, productId, TestCoins.this, PURCHASE_ACITIVITY_CODE);
		} catch (NotSupportedException e) {
			e.printStackTrace();
		} catch (ServiceNotReadyException e) {
			e.printStackTrace();
		} catch (SendIntentException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (IabException e) {
			e.printStackTrace();
			
			final int code = e.errorCode.value;
			if (code == ServerResponseCode.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
				consumeCoinProduct(productId);
			}
		}
	}
	
	private void consumeCoinProduct(String id) {
		
		addCoin(id);
		
		try {
			iab.consume(Product.Type.ONE_TIME_PURCHASE, id);
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
	
	private void addCoin(String productId) {
		
		MyCoin coin = findCoin(productId);
		if (coin == null) {
			Log.e(TAG, "addCoin: cannot find coin of id:" + productId);
			return;
		}
		
		this.coins += coin.amount;
		coinView.setText(Integer.toString(this.coins));
	}
	
	private MyCoin findCoin(String id) {
		
		for (MyCoin coin : Coins) {
			if (coin.id.equals(id)) {
				return coin;
			}
		}
		return null;
	}
}
