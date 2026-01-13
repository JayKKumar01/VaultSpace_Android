package com.github.jaykkumar01.vaultspace.activities;

import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmSpec;
import com.github.jaykkumar01.vaultspace.views.popups.loading.LoadingSpec;

public class DemoActivity extends AppCompatActivity {

    private ModalHost modalHost;

    private LoadingSpec loadingSpec;
    private ConfirmSpec cancelSetupConfirm;
    private ConfirmSpec exitAppConfirm;

    private boolean setupCanceled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_demo);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        modalHost = ModalHost.attach(this);
        initBackHandling();

        startSetupFlow();
    }

    private void startSetupFlow() {

        loadingSpec = new LoadingSpec();
        modalHost.request(loadingSpec);

        // simulate user action → wants to cancel setup
        new Handler().postDelayed(this::showCancelSetupConfirm, 1000);

        // simulate setup work completion
        new Handler().postDelayed(this::onSetupCompleted, 2500);
    }

    private void showCancelSetupConfirm() {

        cancelSetupConfirm = new ConfirmSpec(
                "Cancel setup?",
                "Setup is in progress. Do you want to cancel it?",
                true,
                ModalEnums.Priority.HIGH,


                () -> {
                    // user CONFIRMED cancel → stop work
                    setupCanceled = true;

                    modalHost.dismiss(loadingSpec, ModalEnums.DismissResult.SYSTEM);
                    cancelSetupConfirm = null;
                },
                () -> {
                    // user chose to continue setup
                    cancelSetupConfirm = null;
                }
        );

        modalHost.request(cancelSetupConfirm);
    }

    private void onSetupCompleted() {

        // if setup was canceled, ignore completion
        if (setupCanceled) {
            return;
        }

        // setup finished → loading no longer needed
        modalHost.dismiss(loadingSpec, ModalEnums.DismissResult.SYSTEM);

        // cancel setup confirm now irrelevant → dismiss if still visible/queued
        if (cancelSetupConfirm != null) {
            modalHost.dismiss(cancelSetupConfirm, ModalEnums.DismissResult.REPLACED);
            cancelSetupConfirm = null;
        }

        // show exit app confirm
        showExitAppConfirm();
    }

    private void showExitAppConfirm() {

        exitAppConfirm = new ConfirmSpec(
                "Exit App",
                "Setup is complete. Exit the app?",
                true,
                ModalEnums.Priority.MEDIUM,
                () -> {
                    finish();
                },
                () -> {
                    exitAppConfirm = null;
                }
        );

        modalHost.request(exitAppConfirm);
    }

    private void initBackHandling() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {
                        if (modalHost.onBackPressed()) {
                            return;
                        }
                        //TODO
                    }
                });
    }

}
