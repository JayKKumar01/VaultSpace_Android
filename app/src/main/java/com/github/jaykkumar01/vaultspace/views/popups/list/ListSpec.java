package com.github.jaykkumar01.vaultspace.views.popups.list;

import com.github.jaykkumar01.vaultspace.views.popups.core.Modal;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalSpec;

import java.util.List;
import java.util.function.Consumer;

public class ListSpec extends ModalSpec {

    public final String title;
    public final List<String> items;

    public Consumer<Integer> onItemSelected;
    public Runnable onCanceled;

    public ListSpec(
            String title,
            List<String> items,
            Consumer<Integer> onItemSelected,
            Runnable onCanceled
    ) {
        this.title = title;
        this.items = items;
        this.onItemSelected = onItemSelected;
        this.onCanceled = onCanceled;
    }

    @Override
    public Modal createModal() {
        return new ListModal(this);
    }
}
