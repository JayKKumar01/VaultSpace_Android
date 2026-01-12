package com.github.jaykkumar01.vaultspace.views.popups.old.core;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public final class ModalHostView extends FrameLayout {

    private ModalSpec activeSpec;
    private ModalController activeController;

    private final Deque<ModalSpec> queue = new ArrayDeque<>();

    /* ==========================================================
     * Attachment
     * ========================================================== */

    public static ModalHostView attach(@NonNull Activity activity) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        ModalHostView host = new ModalHostView(activity);
        root.addView(host);
        return host;
    }

    private ModalHostView(@NonNull Activity activity) {
        super(activity);
        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setClickable(true);
        setVisibility(GONE);
    }

    /* ==========================================================
     * Public API
     * ========================================================== */

    public void request(@NonNull ModalSpec spec) {

        // No active modal → show immediately
        if (activeSpec == null) {
            show(spec);
            return;
        }

        int currPriority = activeSpec.getPriority().priority;
        int nextPriority = spec.getPriority().priority;

        // Higher or same priority → replace active
        if (nextPriority >= currPriority) {
            queue.add(activeSpec);
            replaceActive(spec);
            return;
        }

        // Lower priority → queue only
        queue.add(spec);
    }

    public void dismiss(@NonNull ModalSpec spec, @NonNull ModalDismissReason reason) {

        if (spec.getType() == ModalType.STATE) {
            dismissStateSpec(spec, reason);
            return;
        }

        // EVENT dismiss
        dismissEventSpec(spec, reason);
    }

    public boolean handleBackPress() {
        if (activeController == null) return false;

        if (activeController.dismissOnBackPress()) {
            activeController.onCancel();
            dismiss(activeSpec, ModalDismissReason.CANCELED);
        }
        return true;
    }

    public boolean isBlocking() {
        return activeSpec != null;
    }

    /* ==========================================================
     * Internal — Show / Replace
     * ========================================================== */

    private void show(@NonNull ModalSpec spec) {
        activeSpec = spec;
        activeController = spec.createController(getContext());

        ModalController controller = spec.createController(getContext());

        controller.attachDismissRequester(reason -> {
            dismiss(spec,reason);
        });


        removeAllViews();
        addView(activeController.getView());

        setVisibility(VISIBLE);

        spec.onShow();
        activeController.onShow();
    }

    private void replaceActive(@NonNull ModalSpec next) {
        dismissActiveInternal(ModalDismissReason.REPLACED);
        show(next);
    }

    /* ==========================================================
     * Internal — Dismiss logic
     * ========================================================== */

    private void dismissEventSpec(
            @NonNull ModalSpec spec,
            @NonNull ModalDismissReason reason
    ) {
        // If queued → remove only that instance
        if (spec != activeSpec) {
            queue.remove(spec);
            return;
        }

        // If active → dismiss active only
        dismissActiveInternal(reason);
        resumeFromQueueIfNeeded();
    }

    private void dismissStateSpec(
            @NonNull ModalSpec spec,
            @NonNull ModalDismissReason reason
    ) {
        Class<?> stateClass = spec.getClass();

        // Remove all queued STATE specs of same spec type
        Iterator<ModalSpec> iterator = queue.iterator();
        while (iterator.hasNext()) {
            ModalSpec queued = iterator.next();
            if (queued.getType() == ModalType.STATE &&
                queued.getClass() == stateClass) {
                iterator.remove();
            }
        }

        // If active matches same STATE spec type → dismiss it
        if (activeSpec != null &&
            activeSpec.getType() == ModalType.STATE &&
            activeSpec.getClass() == stateClass) {

            dismissActiveInternal(reason);
            resumeFromQueueIfNeeded();
        }
    }

    private void dismissActiveInternal(@NonNull ModalDismissReason reason) {
        if (activeSpec == null) return;

        activeController.onDismiss(reason);
        activeSpec.onDismiss(reason);

        removeAllViews();

        activeSpec = null;
        activeController = null;
    }

    /* ==========================================================
     * Internal — Queue resume
     * ========================================================== */

    private void resumeFromQueueIfNeeded() {
        if (queue.isEmpty()) {
            setVisibility(GONE);
            return;
        }

        ModalSpec next = pollHighestPriority();
        show(next);
    }

    private ModalSpec pollHighestPriority() {
        ModalSpec highest = null;

        for (ModalSpec spec : queue) {
            if (highest == null ||
                spec.getPriority().priority > highest.getPriority().priority) {
                highest = spec;
            }
        }

        queue.remove(highest);
        return highest;
    }
}
