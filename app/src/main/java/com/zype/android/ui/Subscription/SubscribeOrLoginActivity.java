package com.zype.android.ui.Subscription;

import android.app.ProgressDialog;
import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.zype.android.Billing.MarketplaceManager;
import com.zype.android.Billing.PurchaseDetails;
import com.zype.android.Billing.Subscription;
import com.zype.android.R;
import com.zype.android.ZypeApp;
import com.zype.android.ui.NavigationHelper;
import com.zype.android.ui.base.BaseActivity;
import com.zype.android.utils.BundleConstants;
import com.zype.android.utils.DialogHelper;
import com.zype.android.utils.Logger;

import java.util.List;

import static com.zype.android.utils.BundleConstants.REQUEST_CONSUMER;
import static com.zype.android.utils.BundleConstants.REQUEST_LOGIN;
import static com.zype.android.utils.BundleConstants.REQUEST_SUBSCRIPTION;

public class SubscribeOrLoginActivity extends BaseActivity {
    private static final String TAG = SubscribeOrLoginActivity.class.getSimpleName();

    private ProgressDialog dialogProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe_or_login);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.subscribe_or_login_title));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONSUMER:
                if (resultCode == RESULT_OK) {
                    updatePurchases();
                }
                else {
                    onCancel();
                }
                break;
            case REQUEST_LOGIN:
                if (resultCode == RESULT_OK) {
                    if (SubscriptionHelper.hasSubscription()) {
                        NavigationHelper.getInstance(this).switchToVideoDetailsScreen(this,
                                getIntent().getExtras().getString(BundleConstants.VIDEO_ID),
                                getIntent().getExtras().getString(BundleConstants.PLAYLIST_ID),
                                false);
                        finish();
                    }
                    else {
                        NavigationHelper.getInstance(this).switchToSubscriptionScreen(this, getIntent().getExtras());
                    }
                }
                else {
                    onCancel();
                }
                break;
            case REQUEST_SUBSCRIPTION:
                if (resultCode == RESULT_OK) {
                    NavigationHelper.getInstance(this).switchToVideoDetailsScreen(this,
                            getIntent().getExtras().getString(BundleConstants.VIDEO_ID),
                            getIntent().getExtras().getString(BundleConstants.PLAYLIST_ID),
                            false);
                    finish();
                }
                else {
                    onCancel();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void onCancel() {
        finish();
    }

    @Override
    protected String getActivityName() {
        return TAG;
    }

    private void showProgress() {
        dialogProgress = new ProgressDialog(this);
        dialogProgress.setMessage(getString(R.string.consumer_progress_create));
        dialogProgress.setCancelable(false);
        dialogProgress.show();
    }

    private void hideProgress() {
        if (dialogProgress != null) {
            dialogProgress.dismiss();
        }
    }


    private void updatePurchases() {
        ZypeApp.marketplaceGateway.getMarketplaceManager()
                .getPurchases(MarketplaceManager.PRODUCT_TYPE_SUBSCRIPTION,
                        new MarketplaceManager.PurchasesUpdatedListener() {
                            @Override
                            public void onPurchasesUpdated(MarketplaceManager.PurchasesUpdatedResponse response) {
                                if (response.isSuccessful()) {
                                    List<PurchaseDetails> purchases = response.getPurchases();
                                    if (purchases != null && purchases.size() > 0) {
                                        verifySubscription(purchases.get(0).getSku());
                                    }
                                    else {
                                        NavigationHelper.getInstance(SubscribeOrLoginActivity.this)
                                                .switchToSubscriptionScreen(SubscribeOrLoginActivity.this, getIntent().getExtras());
                                    }
                                }
                                else {
                                    NavigationHelper.getInstance(SubscribeOrLoginActivity.this)
                                            .switchToSubscriptionScreen(SubscribeOrLoginActivity.this, getIntent().getExtras());
                                }
                            }
                        });
    }

    private void verifySubscription(String sku) {
        Subscription subscription = ZypeApp.marketplaceGateway.findSubscriptionBySku(sku);
        if (subscription != null) {
            showProgress();
            ZypeApp.marketplaceGateway.verifySubscription(subscription).observe(SubscribeOrLoginActivity.this, new Observer<Boolean>() {
                @Override
                public void onChanged(@Nullable Boolean result) {
                    hideProgress();
                    if (result) {
                        setResult(RESULT_OK);
                        finish();
                    }
                    else {
                        DialogHelper.showErrorAlert(SubscribeOrLoginActivity.this,
                                getString(R.string.subscribe_or_login_error_validation));
                    }
                }
            });
        }
        else {
            Logger.e("Not found Zype Plan for existing in-app purchase, sku=" + sku);
            DialogHelper.showErrorAlert(SubscribeOrLoginActivity.this, getString(R.string.subscribe_or_login_error_restore_purchase_zype));
        }
    }
}
