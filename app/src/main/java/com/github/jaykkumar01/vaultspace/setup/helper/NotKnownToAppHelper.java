package com.github.jaykkumar01.vaultspace.setup.helper;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.github.jaykkumar01.vaultspace.core.selection.AccountSelectionHelper;

import java.util.function.Consumer;

public final class NotKnownToAppHelper {

    public enum Result { ALREADY_KNOWN, NOW_KNOWN, STILL_UNKNOWN, CANCELLED }

    private final Context ctx;
    private final AccountSelectionHelper picker;

    public NotKnownToAppHelper(AppCompatActivity a){
        ctx = a;
        picker = new AccountSelectionHelper(a);
    }

    public void resolve(String email, Consumer<Result> cb){
        if (GoogleCredentialFactory.canAccessAccount(ctx, email)){
            cb.accept(Result.ALREADY_KNOWN); return;
        }

        picker.launch(new AccountSelectionHelper.Callback() {
            @Override public void onAccountSelected(String selected){
                if (!email.equalsIgnoreCase(selected)) {
                    cb.accept(Result.STILL_UNKNOWN); return;
                }
                cb.accept(GoogleCredentialFactory.canAccessAccount(ctx, email)
                        ? Result.NOW_KNOWN
                        : Result.STILL_UNKNOWN);
            }
            @Override public void onCancelled(){
                cb.accept(Result.CANCELLED);
            }
        });
    }
}

