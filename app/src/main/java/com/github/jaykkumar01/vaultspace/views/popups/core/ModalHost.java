package com.github.jaykkumar01.vaultspace.views.popups.core;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
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

public final class ModalHost{

    /* =======================
       ACTIVE ENTRY
       ======================= */

    private static final class ActiveEntry {
        final ModalSpec spec;
        final Modal modal;
        final View modalView;

        ActiveEntry(ModalSpec spec, Modal modal, View modalView) {
            this.spec = spec;
            this.modal = modal;
            this.modalView = modalView;
        }
    }


    /* =======================
       STATE
       ======================= */

    private final Context context;
    private final FrameLayout root;

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

    private static final String TAG = "VaultSpace:ModalHost";
    private static String id(Object o) {
        return o == null ? "null" : Integer.toHexString(System.identityHashCode(o));
    }


    private ModalHost(Activity activity) {
        this.context = activity;
        root = new FrameLayout(activity);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        root.setClickable(true);
        root.setVisibility(GONE);


        for (ModalEnums.Priority p : ModalEnums.Priority.values()) {
            eventQueues.put(p, new ArrayDeque<>());
        }

        this.active = null;
    }

    public static ModalHost attach(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        ModalHost host = new ModalHost(activity);
        content.addView(host.root);

        Log.d(TAG, "attach() host=" + id(host) + " rootAttached=true");
        return host;
    }



    /* =======================
       SAFE QUEUE ACCESS
       ======================= */

    private Deque<ModalSpec> queueFor(ModalEnums.Priority priority) {
        Deque<ModalSpec> q = eventQueues.get(priority);
        if (q == null) {
            q = new ArrayDeque<>();
            eventQueues.put(priority, q);
            Log.d(TAG, "queueFor() created queue for priority=" + priority);
        }
        return q;
    }


    /* =======================
       REQUEST
       ======================= */

    public void request(ModalSpec spec) {
        if (spec == null) return;

        SpecState state = specStates.get(spec);

        Log.d(TAG,
                "request() spec=" + id(spec) +
                        " state=" + state +
                        " hasActive=" + (active != null));

        if (state == SpecState.ACTIVE) return;

        if (active == null) {
            Log.d(TAG, "request() -> activate immediately");
            activate(spec);
            return;
        }

        Modal incoming = spec.createModal();
        Log.d(TAG, "request() -> incoming kind=" + incoming.getKind());

        if (incoming.getKind() == ModalEnums.Kind.STATE) {
            handleStateRequest(spec, (StateModal) incoming);
        } else {
            handleEventRequest(spec, (EventModal) incoming);
        }
    }

    private void handleStateRequest(ModalSpec spec, StateModal modal) {
        String key = modal.getStateKey();
        boolean exists = stateKeys.contains(key);

        Log.d(TAG,
                "handleStateRequest() spec=" + id(spec) +
                        " key=" + key +
                        " exists=" + exists);

        if (exists) return;

        stateQueue.addLast(spec);
        stateKeys.add(key);
        specStates.put(spec, SpecState.QUEUED);
    }


    private void handleEventRequest(ModalSpec spec, EventModal incoming) {
        Log.d(TAG,
                "handleEventRequest() spec=" + id(spec) +
                        " priority=" + incoming.getPriority() +
                        " activeKind=" + active.modal.getKind());

        if (active.modal.getKind() == ModalEnums.Kind.STATE) {
            Log.d(TAG, "EVENT interrupts STATE");
            moveActiveToQueue();
            activate(spec);
            return;
        }

        EventModal activeEvent = (EventModal) active.modal;
        boolean replaces =
                incoming.getPriority().ordinal() >= activeEvent.getPriority().ordinal();

        Log.d(TAG, replaces ? "EVENT replaces active" : "EVENT queued");

        if (replaces) {
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

        active = new ActiveEntry(spec, modal, modalView);

        if (modal.getKind() == ModalEnums.Kind.EVENT) {
            ((EventModal) modal).attachDismissRequester((request, result, data) -> {
                dismissActive(result, data);
            });
        }

        root.addView(modalView);

        Log.d(TAG,
                "activate() spec=" + id(spec) +
                        " kind=" + modal.getKind() +
                        " children=" + root.getChildCount());

        root.setOnClickListener(v -> {
            if (active != null &&
                    active.modal.canDismiss(ModalEnums.DismissRequest.OUTSIDE_TOUCH)) {
                dismiss(active.spec, ModalEnums.DismissResult.CANCELED);
            }
        });

        specStates.put(spec, SpecState.ACTIVE);
        root.setVisibility(VISIBLE);

        Log.d(TAG, "activate() -> root VISIBLE");

        modal.onShow();
    }


    /* =======================
       REPLACE
       ======================= */

    private void moveActiveToQueue() {
        ActiveEntry entry = active;

        Log.d(TAG,
                "moveActiveToQueue() spec=" + id(entry.spec) +
                        " kind=" + entry.modal.getKind());

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
        root.removeView(entry.modalView);

        Log.d(TAG,
                "moveActiveToQueue() done children=" + root.getChildCount());

        active = null;
    }


    /* =======================
       DISMISS
       ======================= */

    public void dismiss(ModalSpec spec, ModalEnums.DismissResult result) {
        SpecState state = specStates.get(spec);

        Log.d(TAG,
                "dismiss() spec=" + id(spec) +
                        " result=" + result +
                        " state=" + state +
                        " activeMatch=" + (active != null && spec == active.spec));

        if (state == SpecState.DISMISSED) return;

        if (active != null && spec == active.spec) {
            dismissActive(result, null);
            return;
        }

        if (state == SpecState.QUEUED) {
            dismissQueued(spec, result);
        }
    }

    public void dismissAll(ModalEnums.DismissResult dismissResult) {
    }


    private void dismissActive(ModalEnums.DismissResult result, Object dismissedData) {
        ActiveEntry entry = active;

        Log.d(TAG,
                "dismissActive() spec=" + id(entry.spec) +
                        " result=" + result);

        if (entry.modal instanceof StateModal) {
            stateKeys.remove(((StateModal) entry.modal).getStateKey());
        }

        entry.modal.onHide();
        root.removeView(entry.modalView);

        specStates.put(entry.spec, SpecState.DISMISSED);
        active = null;

        activateNext();

        entry.modal.onDismissed(result, dismissedData);

        if (active == null) {
            root.setVisibility(GONE);
            Log.d(TAG, "dismissActive() -> root GONE");
        }

    }


    private void dismissQueued(ModalSpec spec, ModalEnums.DismissResult result) {
        Log.d(TAG,
                "dismissQueued() spec=" + id(spec) +
                        " result=" + result);

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
        Log.d(TAG, "activateNext()");

        for (ModalEnums.Priority p : ModalEnums.Priority.values()) {
            ModalSpec next = queueFor(p).pollFirst();
            if (next != null) {
                Log.d(TAG, "activateNext() -> EVENT priority=" + p);
                activate(next);
                return;
            }
        }

        ModalSpec nextState = stateQueue.pollFirst();
        if (nextState != null) {
            Log.d(TAG, "activateNext() -> STATE");
            activate(nextState);
        } else {
            Log.d(TAG, "activateNext() -> none");
        }
    }


    /* =======================
       INPUT
       ======================= */

    public boolean onBackPressed() {
        Log.d(TAG, "onBackPressed() hasActive=" + (active != null));

        if (active == null) return false;

        if (active.modal.canDismiss(ModalEnums.DismissRequest.BACK_PRESS)) {
            Log.d(TAG, "onBackPressed() -> dismiss");
            dismiss(active.spec, ModalEnums.DismissResult.CANCELED);
            return true;
        }
        return false;
    }

}
