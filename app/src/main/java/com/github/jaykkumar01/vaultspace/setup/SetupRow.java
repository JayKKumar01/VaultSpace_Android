package com.github.jaykkumar01.vaultspace.setup;

public final class SetupRow {

    public final String email;
    public final SetupState state;

    public SetupRow(String email, SetupState state) {
        this.email = email;
        this.state = state;
    }
}
