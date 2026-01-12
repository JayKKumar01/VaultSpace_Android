package com.github.jaykkumar01.vaultspace.views.popups.core;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ModalHost {

    /* =======================
       ACTIVE ENTRY
       ======================= */

    private static final class ActiveEntry {
        final ModalSpec spec;
        final Modal modal;
        final FrameLayout overlay;
        final View modalView;

        ActiveEntry(ModalSpec spec, Modal modal, FrameLayout overlay, View modalView) {
            this.spec = spec;
            this.modal = modal;
            this.overlay = overlay;
            this.modalView = modalView;
        }
    }

    /* =======================
       STATE
       ======================= */

    private final Context context;
    private final ViewGroup root;

    private ActiveEntry active;

    private final EnumMap<ModalEnums.Priority, Deque<ModalSpec>> eventQueues =
            new EnumMap<>(ModalEnums.Priority.class);

    private final Deque<ModalSpec> stateQueue = new ArrayDeque<>();

    private final Set<String> stateKeys = new HashSet<>();
    private final Map<ModalSpec, SpecState> specStates = new HashMap<>();

    private enum SpecState {
        ACTIVE,
        QUEUED,
        DISMISSED
    }

    /* =======================
       ATTACH
       ======================= */

    private ModalHost(Activity activity) {
        this.context = activity;
        this.root = activity.findViewById(android.R.id.content);

        for (ModalEnums.Priority p : ModalEnums.Priority.values()) {
            eventQueues.put(p, new ArrayDeque<>());
        }

        this.active = null;
    }

    public static ModalHost attach(Activity activity) {
        return new ModalHost(activity);
    }

    /* =======================
       SAFE QUEUE ACCESS
       ======================= */

    private Deque<ModalSpec> queueFor(ModalEnums.Priority priority) {
        Deque<ModalSpec> q = eventQueues.get(priority);
        if (q == null) {
            q = new ArrayDeque<>();
            eventQueues.put(priority, q);
        }
        return q;
    }

    /* =======================
       REQUEST
       ======================= */

    public void request(ModalSpec spec) {
        if (spec == null) return;
        if (specStates.get(spec) == SpecState.DISMISSED) return;

        if (active == null) {
            activate(spec);
            return;
        }

        Modal incoming = spec.createModal();

        if (incoming.getKind() == ModalEnums.Kind.STATE) {
            handleStateRequest(spec, (StateModal) incoming);
        } else {
            handleEventRequest(spec, (EventModal) incoming);
        }
    }

    private void handleStateRequest(ModalSpec spec, StateModal modal) {
        String key = modal.getStateKey();
        if (stateKeys.contains(key)) return;

        stateQueue.addLast(spec);
        stateKeys.add(key);
        specStates.put(spec, SpecState.QUEUED);
    }

    private void handleEventRequest(ModalSpec spec, EventModal incoming) {
        if (active.modal.getKind() == ModalEnums.Kind.STATE) {
            moveActiveToQueue();
            activate(spec);
            return;
        }

        EventModal activeEvent = (EventModal) active.modal;
        if (incoming.getPriority().ordinal() >= activeEvent.getPriority().ordinal()) {
            moveActiveToQueue();
            activate(spec);
        } else {
            queueFor(incoming.getPriority()).addLast(spec);
            specStates.put(spec, SpecState.QUEUED);
        }
    }

    /* =======================
       ACTIVATE
       ======================= */

    private void activate(ModalSpec spec) {
        Modal modal = spec.createModal();
        View modalView = modal.createView(context);

        FrameLayout overlay = new FrameLayout(context);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlay.setClickable(true);

        modalView.setClickable(true);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.CENTER;

        overlay.addView(modalView, lp);

        ActiveEntry entry = new ActiveEntry(spec, modal, overlay, modalView);
        active = entry;

        overlay.setOnClickListener(v -> {
            if (entry.modal.canDismiss(ModalEnums.DismissRequest.OUTSIDE_TOUCH)) {
                dismiss(entry.spec, ModalEnums.DismissResult.CANCELED);
            }
        });

        specStates.put(spec, SpecState.ACTIVE);
        root.addView(overlay);
        modal.onShow();
    }

    /* =======================
       REPLACE
       ======================= */

    private void moveActiveToQueue() {
        ActiveEntry entry = active;

        if (entry.modal.getKind() == ModalEnums.Kind.STATE) {
            StateModal state = (StateModal) entry.modal;
            stateQueue.addLast(entry.spec);
            stateKeys.add(state.getStateKey());
        } else {
            EventModal event = (EventModal) entry.modal;
            queueFor(event.getPriority()).addLast(entry.spec);
        }

        specStates.put(entry.spec, SpecState.QUEUED);

        entry.modal.onHide();
        root.removeView(entry.overlay);

        active = null;
    }

    /* =======================
       DISMISS
       ======================= */

    public void dismiss(ModalSpec spec, ModalEnums.DismissResult result) {
        SpecState state = specStates.get(spec);
        if (state == SpecState.DISMISSED) return;

        if (active != null && spec == active.spec) {
            dismissActive(result);
            return;
        }

        if (state == SpecState.QUEUED) {
            dismissQueued(spec, result);
        }
    }

    private void dismissActive(ModalEnums.DismissResult result) {
        ActiveEntry entry = active;

        if (entry.modal instanceof StateModal) {
            stateKeys.remove(((StateModal) entry.modal).getStateKey());
        }

        entry.modal.onDismissed(result, null);
        entry.modal.onHide();
        root.removeView(entry.overlay);

        specStates.put(entry.spec, SpecState.DISMISSED);
        active = null;

        activateNext();
    }

    private void dismissQueued(ModalSpec spec, ModalEnums.DismissResult result) {
        if (spec.createModal().getKind() == ModalEnums.Kind.EVENT) {
            spec.createModal().onDismissed(result, null);
        }

        stateQueue.remove(spec);
        for (ModalEnums.Priority p : ModalEnums.Priority.values()) {
            queueFor(p).remove(spec);
        }

        specStates.put(spec, SpecState.DISMISSED);
    }

    /* =======================
       ACTIVATE NEXT
       ======================= */

    private void activateNext() {
        for (ModalEnums.Priority p : ModalEnums.Priority.values()) {
            ModalSpec next = queueFor(p).pollFirst();
            if (next != null) {
                activate(next);
                return;
            }
        }

        ModalSpec nextState = stateQueue.pollFirst();
        if (nextState != null) {
            activate(nextState);
        }
    }

    /* =======================
       INPUT
       ======================= */

    public boolean onBackPressed() {
        if (active == null) return false;

        if (active.modal.canDismiss(ModalEnums.DismissRequest.BACK_PRESS)) {
            dismiss(active.spec, ModalEnums.DismissResult.CANCELED);
        }
        return true;
    }
}
