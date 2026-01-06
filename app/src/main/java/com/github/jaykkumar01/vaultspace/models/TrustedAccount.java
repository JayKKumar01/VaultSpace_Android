package com.github.jaykkumar01.vaultspace.models;

public class TrustedAccount {

    public final String email;

    // Bytes
    public final long totalQuota;
    public final long usedQuota;
    public final long freeQuota;

    public TrustedAccount(
            String email,
            long totalQuota,
            long usedQuota,
            long freeQuota
    ) {
        this.email = email;
        this.totalQuota = totalQuota;
        this.usedQuota = usedQuota;
        this.freeQuota = freeQuota;
    }
}
