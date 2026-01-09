package com.github.jaykkumar01.vaultspace.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.jaykkumar01.vaultspace.R;

public class AlbumActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Album";

    /* ---------------- Intent Keys ---------------- */

    public static final String EXTRA_ALBUM_ID = "album_id";
    public static final String EXTRA_ALBUM_NAME = "album_name";

    /* ---------------- State ---------------- */

    private String albumId;
    private String albumName;

    /* ---------------- Lifecycle ---------------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!readIntent()) {
            Log.e(TAG, "Missing album data. Finishing.");
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_album);
        applyWindowInsets();

        Log.d(TAG, "Opened album: " + albumName + " (" + albumId + ")");
    }

    /* ---------------- Intent ---------------- */

    private boolean readIntent() {
        albumId = getIntent().getStringExtra(EXTRA_ALBUM_ID);
        albumName = getIntent().getStringExtra(EXTRA_ALBUM_NAME);

        return albumId != null && !albumId.isEmpty();
    }

    /* ---------------- Insets ---------------- */

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
