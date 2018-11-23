package com.zype.android.ui.Subscription;


import android.app.ProgressDialog;
import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.zype.android.Auth.AuthHelper;
import com.zype.android.Billing.MarketplaceManager;
import com.zype.android.Billing.PurchaseDetails;
import com.zype.android.Billing.Subscription;
import com.zype.android.R;
import com.zype.android.ZypeApp;
import com.zype.android.ui.NavigationHelper;
import com.zype.android.utils.DialogHelper;
import com.zype.android.utils.Logger;
import com.zype.android.webapi.WebApiManager;

import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 */
public class SubscribeOrLoginFragment extends Fragment {

    private ProgressDialog dialogProgress;

    private WebApiManager api;

    public SubscribeOrLoginFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        api = WebApiManager.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_subscribe_or_login, container, false);

        final NavigationHelper navigationHelper = NavigationHelper.getInstance(getActivity());
        Button buttonSubscribe = rootView.findViewById(R.id.buttonSubscribe);
        buttonSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AuthHelper.isLoggedIn()) {
                    navigationHelper.switchToSubscriptionScreen(getActivity(), getActivity().getIntent().getExtras());
                }
                else {
                    navigationHelper.switchToConsumerScreen(getActivity());
                }
            }
        });

        Button buttonLogin = rootView.findViewById(R.id.buttonLogin);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationHelper.switchToLoginScreen(getActivity(), null);
            }
        });

        final Button buttonRestorePurchases = rootView.findViewById(R.id.buttonRestorePurchases);
        ZypeApp.marketplaceGateway.getMarketplaceManager()
                .getPurchases(MarketplaceManager.PRODUCT_TYPE_SUBSCRIPTION,
                        new MarketplaceManager.PurchasesUpdatedListener() {
                            @Override
                            public void onPurchasesUpdated(MarketplaceManager.PurchasesUpdatedResponse response) {
                                if (response.isSuccessful()) {
                                    List<PurchaseDetails> purchases = response.getPurchases();
                                    buttonRestorePurchases.setOnClickListener(createRestorePurchasesOnClickListener(purchases.get(0).getSku()));
                                    if (!purchases.isEmpty()) {
                                        Logger.d("There are purchases, size=" + purchases.size());
                                        buttonRestorePurchases.setVisibility(View.VISIBLE);
                                    }
                                    else {
                                        Logger.d("There are no purchases on the device");
                                        buttonRestorePurchases.setVisibility(View.GONE);
                                    }
                                }
                                else {
                                    Logger.d("There are no purchases on the device");
                                    buttonRestorePurchases.setVisibility(View.GONE);
                                }
                            }
                        });

        api.subscribe(this);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        api.unsubscribe(this);

        super.onDestroyView();
    }

    private void showProgress(String message) {
        dialogProgress = new ProgressDialog(getActivity());
        dialogProgress.setMessage(message);
        dialogProgress.setCancelable(false);
        dialogProgress.show();
    }

    private void hideProgress() {
        if (dialogProgress != null) {
            dialogProgress.dismiss();
        }
    }

    private View.OnClickListener createRestorePurchasesOnClickListener(final String sku) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AuthHelper.isLoggedIn()) {
                    // Get first purchase from the purchase list and try to validate it
                    Subscription subscription = ZypeApp.marketplaceGateway.findSubscriptionBySku(sku);
                    if (subscription != null) {
                        showProgress(getString(R.string.subscription_verify));
                        ZypeApp.marketplaceGateway.verifySubscription(subscription).observe(SubscribeOrLoginFragment.this, new Observer<Boolean>() {
                            @Override
                            public void onChanged(@Nullable Boolean result) {
                                hideProgress();
                                if (result) {
                                    getActivity().setResult(RESULT_OK);
                                    getActivity().finish();
                                }
                                else {
                                    DialogHelper.showErrorAlert(getActivity(),
                                            getString(R.string.subscribe_or_login_error_validation));
                                }
                            }
                        });
                    }
                    else {
                        Logger.e("Not found Zype Plan for existing in-app purchase, sku=" + sku);
                        DialogHelper.showErrorAlert(getActivity(), getString(R.string.subscribe_or_login_error_restore_purchase_zype));
                    }
                }
                else {
                    NavigationHelper.getInstance(getActivity()).switchToConsumerScreen(getActivity());
                }
            }
        };
    }
}
