package com.github.tvbox.osc.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import com.github.tvbox.osc.R
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp

class SelectComposeDialogFragment<T> : DialogFragment() {
    private var tipText by mutableStateOf("")
    private var onClick: (T, Int) -> Unit = { _, _ -> }
    private val items = mutableStateListOf<T>()
    private var selectedIndex by mutableStateOf(0)
    private var displayProvider: ((T) -> String)? = null

    fun setTip(tip: String) { tipText = tip }
    fun setAdapter(
        listener: SelectDialogAdapter.SelectDialogInterface<T>,
        diff: DiffUtil.ItemCallback<T>,
        data: List<T>,
        select: Int
    ) {
        items.clear(); items.addAll(data)
        selectedIndex = select
        onClick = { value, pos -> listener.click(value, pos) }
        displayProvider = { value -> listener.getDisplay(value) }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.CustomDialogStyle)
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    AlertDialog(
                        onDismissRequest = { dismissAllowingStateLoss() },
                        title = { Text(text = tipText, style = MaterialTheme.typography.titleMedium) },
                        text = {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 420.dp)
                            ) {
                                itemsIndexed(items) { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedIndex = index
                                                @Suppress("UNCHECKED_CAST")
                                                onClick(item as T, index)
                                                dismissAllowingStateLoss()
                                            }
                                            .padding(vertical = 12.dp),
                                    ) {
                                        RadioButton(
                                            selected = index == selectedIndex,
                                            onClick = null
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = displayProvider?.invoke(item) ?: item.toString(),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { dismissAllowingStateLoss() }) {
                                Text("取消")
                            }
                        }
                    )
                }
            }
        }
    }

    fun safeShow(fm: FragmentManager, tag: String) {
        if (isAdded) return
        show(fm, tag)
    }
}
