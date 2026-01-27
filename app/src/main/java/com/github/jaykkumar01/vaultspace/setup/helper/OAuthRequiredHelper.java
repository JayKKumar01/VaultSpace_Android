package com.github.jaykkumar01.vaultspace.setup.helper;

import android.content.Intent;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import java.util.function.Consumer;

public final class OAuthRequiredHelper {

    public enum Result { GRANTED, DENIED, FAILED }

    private static final String TAG = "VaultSpace:OAuth";

    private final AppCompatActivity activity;
    private final ActivityResultLauncher<Intent> launcher;

    private Consumer<Result> pending;
    private String pendingEmail;

    public OAuthRequiredHelper(AppCompatActivity a){
        activity = a;
        launcher = a.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                r -> {
                    Consumer<Result> cb = pending;
                    String email = pendingEmail;
                    pending = null; pendingEmail = null;
                    if (cb == null) return;
                    if (r.getResultCode() != AppCompatActivity.RESULT_OK) {
                        cb.accept(Result.DENIED); return;
                    }
                    tryResolveAsync(email, cb);
                }
        );
    }

    public void resolve(String email, Consumer<Result> cb){
        tryResolveAsync(email, cb);
    }

    private void tryResolveAsync(String email, Consumer<Result> cb){
        SetupExecutors.IO.execute(() -> {
            try {
                Drive d = DriveClientProvider.forAccount(activity, email);
                d.about().get().setFields("user").execute();
                SetupExecutors.MAIN.post(() -> cb.accept(Result.GRANTED));
            } catch (UserRecoverableAuthIOException e){
                SetupExecutors.MAIN.post(() -> {
                    pending = cb;
                    pendingEmail = email;
                    launcher.launch(e.getIntent());
                });
            } catch (Exception e){
                Log.e(TAG, "OAuth check failed for: " + email, e);
                SetupExecutors.MAIN.post(() -> cb.accept(Result.FAILED));
            }
        });
    }
}
