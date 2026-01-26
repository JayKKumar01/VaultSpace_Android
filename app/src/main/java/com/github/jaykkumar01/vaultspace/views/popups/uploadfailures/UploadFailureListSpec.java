package com.github.jaykkumar01.vaultspace.views.popups.uploadfailures;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.views.popups.core.Modal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalSpec;

import java.util.List;

public final class UploadFailureListSpec extends ModalSpec {

    public String title;
    public List<UploadSelection> failures;
    public Runnable onOk;

    public UploadFailureListSpec() {
    }

    @Override
    public Modal createModal() {
        return new UploadFailureListModal(this);
    }

    public void setFailures(List<UploadSelection> failures) {
        this.failures = failures;
    }

    public void setOnOk(Runnable onOk) {
        this.onOk = onOk;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
