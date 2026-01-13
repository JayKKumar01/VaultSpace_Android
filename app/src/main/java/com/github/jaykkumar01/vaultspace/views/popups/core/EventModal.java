package com.github.jaykkumar01.vaultspace.views.popups.core;

public abstract class EventModal extends Modal {

    private final ModalEnums.Priority priority;

    // injected by ModalHost
    private DismissRequester dismissRequester;

    protected EventModal(ModalEnums.Priority priority) {
        super(ModalEnums.Kind.EVENT);
        this.priority = priority;
    }

    public final ModalEnums.Priority getPriority() {
        return priority;
    }

    /* =======================
       Host wiring (internal)
       ======================= */

    interface DismissRequester {
        void dismiss(
                ModalEnums.DismissRequest request,
                ModalEnums.DismissResult result,
                Object data
        );
    }

    final void attachDismissRequester(DismissRequester requester) {
        this.dismissRequester = requester;
    }

    /* =======================
       Modal → Host signal
       ======================= */

    protected final void requestDismiss(
            ModalEnums.DismissResult result,
            Object data
    ) {
        if (dismissRequester != null) {
            dismissRequester.dismiss(
                    ModalEnums.DismissRequest.USER_ACTION,
                    result,
                    data
            );
        }
    }
}
