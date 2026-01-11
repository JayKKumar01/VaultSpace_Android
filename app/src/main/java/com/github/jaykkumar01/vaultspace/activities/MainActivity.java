package com.github.jaykkumar01.vaultspace.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.core.network.NetworkGateHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;

public class MainActivity extends AppCompatActivity {

    private NetworkGateHelper networkGate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        networkGate = new NetworkGateHelper(this);

        networkGate.awaitNetwork(() -> {
            UserSession session = new UserSession(this);

            Intent intent = session.isLoggedIn()
                    ? new Intent(this, DashboardActivity.class)
                    : new Intent(this, LoginActivity.class);

            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkGate != null) {
            networkGate.release();
        }
    }
}
