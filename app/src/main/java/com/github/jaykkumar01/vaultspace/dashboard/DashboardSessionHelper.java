package com.github.jaykkumar01.vaultspace.dashboard;

import android.content.Context;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.core.session.UserSession;

public class DashboardSessionHelper {

    private static final String TAG = "VaultSpace:Session";

    private final UserSession userSession;

    public DashboardSessionHelper(Context context) {
        userSession = new UserSession(context);
    }

    public boolean isSessionValid() {
        boolean valid = userSession.isLoggedIn();
        Log.d(TAG, "Session valid = " + valid);
        return valid;
    }

    public String getPrimaryEmail() {
        String email = userSession.getPrimaryAccountEmail();
        Log.d(TAG, "Primary email = " + email);
        return email;
    }

    public void logout() {
        Log.d(TAG, "Clearing session");
        userSession.clearSession();
    }
}
