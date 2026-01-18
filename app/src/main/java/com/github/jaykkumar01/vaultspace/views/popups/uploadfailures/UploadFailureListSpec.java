package com.github.jaykkumar01.vaultspace.views.popups.uploadfailures;

import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureEntity;
import com.github.jaykkumar01.vaultspace.views.popups.core.Modal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalSpec;

import java.util.List;

public final class UploadFailureListSpec extends ModalSpec {

    public String title;
    public List<UploadFailureEntity> failures;
    public Runnable onOk;

    public UploadFailureListSpec() {
    }

    public UploadFailureListSpec(
            String title,
            List<UploadFailureEntity> failures,
            Runnable onOk
    ) {
        this.title = title;
        this.failures = failures;
        this.onOk = onOk;
    }

    @Override
    public Modal createModal() {
        return new UploadFailureListModal(this);
    }

    public void setFailures(List<UploadFailureEntity> failures) {
        this.failures = failures;
    }

    public void setOnOk(Runnable onOk) {
        this.onOk = onOk;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
