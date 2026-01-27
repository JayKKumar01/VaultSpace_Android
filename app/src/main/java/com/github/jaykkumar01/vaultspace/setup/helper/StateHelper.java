package com.github.jaykkumar01.vaultspace.setup.helper;

import android.content.Context;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveStorageRepository;
import com.github.jaykkumar01.vaultspace.core.drive.TrustedAccountsRepository;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.github.jaykkumar01.vaultspace.setup.SetupState;
import com.google.api.services.drive.Drive;
import java.util.function.Consumer;

public final class StateHelper {

    private static final String TAG = "VaultSpace:StateHelper";

    private final Context context;
    private final NotKnownToAppHelper notKnown;
    private final OAuthRequiredHelper oauth;
    private final TrustedAccountsRepository repo;

    public StateHelper(AppCompatActivity a){
        context = a.getApplicationContext();
        notKnown = new NotKnownToAppHelper(a);
        oauth = new OAuthRequiredHelper(a);
        repo = TrustedAccountsRepository.getInstance(a);
    }

    public boolean isNotKnown(String email){
        return !GoogleCredentialFactory.canAccessAccount(context, email);
    }

    public boolean isLimited(String email){
        return repo.getAccountSnapshot(email) == null;
    }

    public void resolve(String email, Consumer<SetupState> cb){
        notKnown.resolve(email, r -> {
            switch (r){
                case STILL_UNKNOWN:
                    Log.w(TAG, "Account still not known: " + email);
                case CANCELLED:
                    cb.accept(SetupState.NOT_KNOWN_TO_APP); return;
                case ALREADY_KNOWN:
                case NOW_KNOWN:
                    resolveOAuth(email, cb);
            }
        });
    }

    private void resolveOAuth(String email, Consumer<SetupState> cb){
        oauth.resolve(email, r -> {
            switch (r){
                case DENIED:
                    Log.w(TAG, "OAuth denied for: " + email);
                    cb.accept(SetupState.OAUTH_REQUIRED); return;
                case FAILED:
                    Log.e(TAG, "OAuth failed for: " + email);
                    cb.accept(SetupState.LIMITED); return;
                case GRANTED:
                    promoteAsync(email, cb);
            }
        });
    }

    private void promoteAsync(String email, Consumer<SetupState> cb){
        SetupExecutors.IO.execute(() -> {
            SetupState state;
            try {
                Drive d = DriveClientProvider.forAccount(context, email);
                TrustedAccount acc = DriveStorageRepository.fetchStorageInfo(d, email);
                repo.addAccount(acc);
                state = SetupState.HEALTHY;
            } catch (Exception e){
                Log.e(TAG, "Promote failed for: " + email, e);
                state = SetupState.LIMITED;
            }
            final SetupState result = state;
            SetupExecutors.MAIN.post(() -> cb.accept(result));
        });
    }

}
