package com.github.jaykkumar01.vaultspace.core.upload.base;

public enum FailureReason {
        NO_TRUSTED_ACCOUNT(true),
        NO_ACCESS(true),
        NO_SPACE(true),
        URI_NOT_FOUND(false),
        IO_ERROR(true),
        DRIVE_ERROR(true),
        CANCELLED(false);

        private final boolean retryable;

        FailureReason(boolean retryable) {
            this.retryable = retryable;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }