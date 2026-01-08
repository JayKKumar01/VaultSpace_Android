package com.github.jaykkumar01.vaultspace.views;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.github.jaykkumar01.vaultspace.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class CreateFolderView extends ConstraintLayout {

    public interface Callback {
        void onCreate(String name);
        void onCancel();
    }

    private TextInputLayout inputLayout;
    private TextInputEditText input;
    private Callback callback;

    public CreateFolderView(Context context) {
        this(context, null);
    }

    public CreateFolderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_create_folder, this, true);

        inputLayout = findViewById(R.id.inputLayout);
        input = findViewById(R.id.inputName);

        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnCreate = findViewById(R.id.btnCreate);

        btnCancel.setOnClickListener(v -> {
            clear();
            if (callback != null) callback.onCancel();
        });

        btnCreate.setOnClickListener(v -> {
            String name = input.getText() == null ? "" : input.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                inputLayout.setError("Name cannot be empty");
                return;
            }
            inputLayout.setError(null);
            clear();
            if (callback != null) callback.onCreate(name);
        });

        setVisibility(GONE);
    }

    public void show(String hint, Callback callback) {
        this.callback = callback;
        inputLayout.setHint(hint);
        setVisibility(VISIBLE);
        input.requestFocus();
    }

    public void hide() {
        clear();
        setVisibility(GONE);
    }

    private void clear() {
        input.setText("");
        inputLayout.setError(null);
    }
}
