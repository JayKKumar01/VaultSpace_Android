package com.github.jaykkumar01.vaultspace.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.AlbumUiHelper;

public class AlbumActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Album";

    public static final String EXTRA_ALBUM_ID = "album_id";
    public static final String EXTRA_ALBUM_NAME = "album_name";

    /* ---------------- State ---------------- */

    private String albumId;
    private String albumName;

    /* ---------------- Views ---------------- */

    private TextView tvAlbumName;
    private ImageView btnBack;
    private FrameLayout contentContainer;

    /* ---------------- UI Helper ---------------- */

    private AlbumUiHelper albumUi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!readIntent()) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_album);
        applyWindowInsets();

        bindViews();
        bindHeader();
        initAlbumUi();
        setupBackHandling();

        Log.d(TAG, "Opened album: " + albumName + " (" + albumId + ")");
    }

    /* ---------------- Intent ---------------- */

    private boolean readIntent() {
        albumId = getIntent().getStringExtra(EXTRA_ALBUM_ID);
        albumName = getIntent().getStringExtra(EXTRA_ALBUM_NAME);
        return albumId != null && !albumId.isEmpty();
    }

    /* ---------------- Views ---------------- */

    private void bindViews() {
        tvAlbumName = findViewById(R.id.tvAlbumName);
        btnBack = findViewById(R.id.btnBack);
        contentContainer = findViewById(R.id.stateContainer);
    }

    private void bindHeader() {
        tvAlbumName.setText(albumName);
        btnBack.setOnClickListener(v -> finish());
    }

    /* ---------------- Album UI ---------------- */

    private void initAlbumUi() {
        albumUi = new AlbumUiHelper(this, contentContainer, albumId);
        albumUi.show();
    }

    /* ---------------- Back handling ---------------- */

    private void setupBackHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // AlbumUiHelper currently has no popups → default
                finish();
            }
        });
    }

    /* ---------------- Insets ---------------- */

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (albumUi != null) albumUi.release();
    }
}
