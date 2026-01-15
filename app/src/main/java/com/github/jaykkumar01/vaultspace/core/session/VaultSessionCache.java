package com.github.jaykkumar01.vaultspace.core.session;

import com.github.jaykkumar01.vaultspace.core.session.cache.AlbumMediaCache;
import com.github.jaykkumar01.vaultspace.core.session.cache.AlbumsCache;
import com.github.jaykkumar01.vaultspace.core.session.cache.TrustedAccountsCache;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;

public final class VaultSessionCache {

    public final AlbumsCache albums = new AlbumsCache();
    public final AlbumMediaCache albumMedia = new AlbumMediaCache();
    public final TrustedAccountsCache trustedAccounts = new TrustedAccountsCache();
    public final UploadCache uploadCache = new UploadCache();

    public void clear() {
        albums.clear();
        albumMedia.clear();
        trustedAccounts.clear();
        uploadCache.clear();
    }
}

