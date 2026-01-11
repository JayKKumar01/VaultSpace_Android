package com.github.jaykkumar01.vaultspace.core.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

public final class NetworkGateHelper {

    public interface Callback {
        void onNetworkAvailable();
    }

    private final ConnectivityManager connectivityManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean fired;
    private ConnectivityManager.NetworkCallback networkCallback;

    public NetworkGateHelper(@NonNull Context context) {
        connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Executes callback immediately if network is available,
     * otherwise waits until connectivity is restored (once).
     */
    public void awaitNetwork(@NonNull Callback callback) {
        if (isNetworkAvailable()) {
            callback.onNetworkAvailable();
            return;
        }

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback =
                new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        if (fired) return;
                        fired = true;

                        cleanup();
                        mainHandler.post(callback::onNetworkAvailable);
                    }
                };

        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    private boolean isNetworkAvailable() {
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps =
                connectivityManager.getNetworkCapabilities(network);

        return caps != null &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void cleanup() {
        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
            networkCallback = null;
        }
    }

    public void release() {
        cleanup();
    }
}
