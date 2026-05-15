package com.opclient.lists.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.lists.domain.ReadingList
import com.opclient.ui.components.SecondaryButton
import com.opclient.ui.components.SectionLabel
import com.opclient.ui.components.feedback.EmptyState
import com.opclient.ui.components.feedback.ErrorState
import com.opclient.ui.components.feedback.LoadingState
import com.opclient.ui.theme.AppThemeTokens
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ListsScreen(
    onBack: () -> Unit,
    viewModel: ListsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showCreateDialog) {
        CreateListDialog(
            onConfirm = { name, desc -> viewModel.onIntent(ListsIntent.CreateList(name, desc)) },
            onDismiss = { viewModel.onIntent(ListsIntent.HideCreateDialog) },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SecondaryButton(text = "BACK", onClick = onBack)
            SecondaryButton(text = "+ NEW LIST", onClick = { viewModel.onIntent(ListsIntent.ShowCreateDialog) })
        }

        if (uiState.selectedList == null) {
            SectionLabel(
                text = "MY LISTS",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when {
                uiState.isLoading -> LoadingState()
                uiState.lists.isEmpty() -> EmptyState(message = "No lists yet. Create one!")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(uiState.lists, key = { it.key }) { list ->
                        ListRow(
                            list = list,
                            onClick = { viewModel.onIntent(ListsIntent.SelectList(list)) },
                            onDelete = { viewModel.onIntent(ListsIntent.DeleteList(list)) },
                        )
                    }
                }
            }

            uiState.error?.let { ErrorState(message = it) }
        } else {
            val selected = uiState.selectedList ?: return@Column
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(text = selected.name)
                SecondaryButton(
                    text = "ALL LISTS",
                    onClick = { viewModel.onIntent(ListsIntent.DeselectList) },
                )
            }

            when {
                uiState.isLoadingSeeds -> LoadingState()
                uiState.seeds.isEmpty() -> EmptyState(message = "No books in this list yet")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(uiState.seeds, key = { it.workKey }) { seed ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                BasicText(
                                    text = seed.title,
                                    style = AppThemeTokens.typography.bookTitle.copy(color = AppThemeTokens.colors.textPrimary),
                                )
                                BasicText(
                                    text = (seed.authorName ?: "").uppercase(),
                                    style = AppThemeTokens.typography.bookAuthor.copy(color = AppThemeTokens.colors.textSecondary),
                                )
                            }
                            SecondaryButton(
                                text = "REMOVE",
                                onClick = { viewModel.onIntent(ListsIntent.RemoveSeed(seed.workKey)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListRow(
    list: ReadingList,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SectionLabel(text = list.name)
            SectionLabel(text = "${list.seedCount} books")
        }
        SecondaryButton(text = "DELETE", onClick = onDelete)
    }
}
