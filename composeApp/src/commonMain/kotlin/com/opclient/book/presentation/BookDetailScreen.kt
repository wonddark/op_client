// composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailScreen.kt
package com.opclient.book.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.opclient.presentation.DetailStatus
import com.opclient.library.domain.Shelf
import com.opclient.ui.components.BookRow
import com.opclient.ui.components.PrimaryButton
import com.opclient.ui.components.SecondaryButton
import com.opclient.ui.components.SectionLabel
import com.opclient.ui.components.SubjectTag
import com.opclient.ui.components.feedback.ErrorState
import com.opclient.ui.components.feedback.LoadingState
import com.opclient.ui.theme.AppThemeTokens
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookDetailScreen(
    workKey: String,
    onAuthorClick: (String) -> Unit,
    onBookClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: BookDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography

    LaunchedEffect(workKey) {
        viewModel.onIntent(BookDetailIntent.Load(workKey))
    }

    when (uiState.status) {
        DetailStatus.Loading -> LoadingState()
        DetailStatus.Error -> ErrorState(
            message = "Failed to load book.",
            onRetry = { viewModel.onIntent(BookDetailIntent.Retry) },
        )
        DetailStatus.Success -> {
            val book = uiState.book ?: return
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (book.coverUrl != null) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            placeholder = ColorPainter(colors.surface2),
                            error = ColorPainter(colors.surface2),
                            modifier = Modifier.fillMaxWidth().height(280.dp),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .background(colors.surface2),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent),
                                ),
                            ),
                    )
                    SecondaryButton(
                        text = "← BACK",
                        onClick = onBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                SectionLabel(
                    text = book.title,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                book.authors.forEach { author ->
                    BasicText(
                        text = author.name,
                        style = typography.bookAuthor.copy(color = colors.textPrimary),
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .clickable { onAuthorClick(author.key) },
                    )
                }
                if (book.firstPublishDate != null) {
                    BasicText(
                        text = book.firstPublishDate,
                        style = typography.bookAuthor.copy(color = colors.textSecondary),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Shelf.entries.forEach { shelf ->
                        if (shelf == uiState.currentShelf) {
                            PrimaryButton(
                                text = shelf.label,
                                onClick = { viewModel.onIntent(BookDetailIntent.RemoveFromLibrary) },
                            )
                        } else {
                            SecondaryButton(
                                text = shelf.label,
                                onClick = { viewModel.onIntent(BookDetailIntent.SetShelf(shelf)) },
                            )
                        }
                    }
                }
                if (book.description != null) {
                    BasicText(
                        text = book.description,
                        style = typography.bookAuthor.copy(color = colors.textPrimary),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (book.subjects.isNotEmpty()) {
                    FlowRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        book.subjects.take(8).forEach { subject ->
                            SubjectTag(text = subject, modifier = Modifier.padding(end = 4.dp, bottom = 4.dp))
                        }
                    }
                }
                if (uiState.relatedWorks.isNotEmpty()) {
                    SectionLabel(
                        text = "More in ${uiState.relatedSubjectName}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    uiState.relatedWorks.forEach { work ->
                        BookRow(
                            title = work.title,
                            author = work.authorName ?: "",
                            coverContent = {
                                AsyncImage(
                                    model = work.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    placeholder = ColorPainter(colors.surface2),
                                    error = ColorPainter(colors.surface2),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            },
                            onClick = { onBookClick(work.key) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}
