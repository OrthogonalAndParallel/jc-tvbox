package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DiffUtil;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * Compose-based popup dialog wrapper that preserves the original Java API.
 * Internally delegates to SelectComposeDialogFragment for lifecycle-correct Compose UI.
 */
public class SelectDialog<T> extends android.app.Dialog {
    private final FragmentActivity activity;
    private final SelectComposeDialogFragment<T> fragment;

    public SelectDialog(@NonNull @NotNull Context context) {
        super(context, com.github.tvbox.osc.R.style.CustomDialogStyle);
        if (!(context instanceof FragmentActivity)) {
            throw new IllegalArgumentException("SelectDialog requires a FragmentActivity context");
        }
        this.activity = (FragmentActivity) context;
        this.fragment = new SelectComposeDialogFragment<>();
    }

    public SelectDialog(@NonNull @NotNull Context context, int resId) {
        this(context);
    }

    @Override
    public void show() {
        fragment.safeShow(activity.getSupportFragmentManager(), "SelectDialog");
    }

    @Override
    public void dismiss() {
        if (fragment.isAdded()) fragment.dismissAllowingStateLoss();
    }

    public void setTip(String tip) {
        fragment.setTip(tip);
    }

    public void setAdapter(SelectDialogAdapter.SelectDialogInterface<T> sourceBeanSelectDialogInterface,
                           DiffUtil.ItemCallback<T> sourceBeanItemCallback,
                           List<T> data,
                           int select) {
        fragment.setAdapter(sourceBeanSelectDialogInterface, sourceBeanItemCallback, data, select);
    }
}
