package com.github.jaykkumar01.vaultspace.models;

public class VaultSpaceMeta {

    public String app;
    public String account;
    public String event;
    public String note;
    public CreatedAt created_at;

    public static class CreatedAt {
        public String iso;
        public String date;
        public String time;
        public String timezone;
    }
}
