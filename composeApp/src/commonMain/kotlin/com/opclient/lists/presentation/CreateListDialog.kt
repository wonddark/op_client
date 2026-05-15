package com.opclient.lists.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.opclient.ui.components.PrimaryButton
import com.opclient.ui.components.SecondaryButton
import com.opclient.ui.components.SectionLabel

@Composable
fun CreateListDialog(
    onConfirm: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel(text = "NEW LIST")

            BasicTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Column {
                        SectionLabel(text = "NAME")
                        inner()
                    }
                },
            )

            BasicTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Column {
                        SectionLabel(text = "DESCRIPTION (OPTIONAL)")
                        inner()
                    }
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(text = "CANCEL", onClick = onDismiss)
                PrimaryButton(
                    text = "CREATE",
                    onClick = { if (name.isNotBlank()) onConfirm(name.trim(), description.trim()) },
                )
            }
        }
    }
}
