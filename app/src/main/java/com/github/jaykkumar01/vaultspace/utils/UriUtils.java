package com.github.jaykkumar01.vaultspace.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.models.UriFileInfo;

public final class UriUtils {

    private UriUtils(){}

    /**
     * Resolves maximum reliable metadata from a SAF Uri.
     * Never throws. Never opens streams.
     */
    @NonNull
    public static UriFileInfo resolve(@NonNull Context context,@NonNull Uri uri){
        ContentResolver cr=context.getContentResolver();

        String name="unknown";
        long size=-1;
        long modified=-1;

        try(Cursor c=cr.query(
                uri,
                new String[]{
                        OpenableColumns.DISPLAY_NAME,
                        OpenableColumns.SIZE,
                        MediaStore.MediaColumns.DATE_MODIFIED
                },
                null,null,null
        )){
            if(c!=null&&c.moveToFirst()){
                int nameIdx=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIdx=c.getColumnIndex(OpenableColumns.SIZE);
                int modIdx=c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED);

                if(nameIdx!=-1) name=c.getString(nameIdx);
                if(sizeIdx!=-1) size=c.getLong(sizeIdx);
                if(modIdx!=-1){
                    long sec=c.getLong(modIdx);
                    if(sec>0) modified=sec*1000L;
                }
            }
        }catch(Exception ignored){}

        if(modified<=0){
            try(Cursor c=cr.query(
                    uri,
                    new String[]{MediaStore.MediaColumns.DATE_MODIFIED},
                    null,null,null
            )){
                if(c!=null&&c.moveToFirst()){
                    long sec=c.getLong(0);
                    if(sec>0) modified=sec*1000L;
                }
            }catch(Exception ignored){}
        }

        return new UriFileInfo(uri,name,size,modified);
    }

    /**
     * Fast metadata-only accessibility check.
     */
    public static boolean isUriAccessible(@NonNull Context context,@NonNull Uri uri){
        try(Cursor c=context.getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,null,null
        )){
            return c!=null&&c.moveToFirst();
        }catch(Exception e){
            return false;
        }
    }
}
