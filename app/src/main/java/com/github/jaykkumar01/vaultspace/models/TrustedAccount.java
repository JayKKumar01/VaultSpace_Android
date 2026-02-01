package com.github.jaykkumar01.vaultspace.models;

public class TrustedAccount {

    public final String email;

    // Bytes (already adjusted)
    public final long totalQuota;
    public final long usedQuota;
    public final long freeQuota;

    private static final long RESERVED_BYTES = 256L * 1024 * 1024;

    public TrustedAccount(String email, long totalQuota, long usedQuota, long freeQuota) {
        this.email = email;
        this.totalQuota = totalQuota;
        this.usedQuota = usedQuota;
        long adjusted = freeQuota - RESERVED_BYTES;
        this.freeQuota = Math.max(adjusted, 0);
    }
}
