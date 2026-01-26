package com.github.jaykkumar01.vaultspace.core.session.db;

import android.content.Context;

import com.github.jaykkumar01.vaultspace.core.session.SetupIgnoreStore;
import com.github.jaykkumar01.vaultspace.core.session.UploadRetryStore;

import java.util.HashMap;
import java.util.Map;

public final class SessionStoreRegistry {

    private final Context appContext;
    private final Map<Class<?>, SessionStore> stores = new HashMap<>();

    public SessionStoreRegistry(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @SuppressWarnings("unchecked")
    public synchronized <T extends SessionStore> T get(Class<T> cls) {
        SessionStore store = stores.get(cls);
        if (store != null) return (T) store;

        store = create(cls);
        stores.put(cls, store);
        return (T) store;
    }

    private SessionStore create(Class<?> cls) {
        if (cls == UploadRetryStore.class)
            return new UploadRetryStore(appContext);

        if (cls == SetupIgnoreStore.class)
            return new SetupIgnoreStore(appContext);

        throw new IllegalArgumentException("Unknown SessionStore: " + cls);
    }

    public synchronized void clearAll() {
        for (SessionStore store : stores.values())
            store.onSessionCleared();
        stores.clear();
    }
}
