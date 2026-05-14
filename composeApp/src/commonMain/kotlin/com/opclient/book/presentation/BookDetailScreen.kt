// composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailScreen.kt
package com.opclient.book.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.opclient.presentation.DetailStatus
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
                SecondaryButton(
                    text = "← BACK",
                    onClick = onBack,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                if (book.coverUrl != null) {
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        placeholder = ColorPainter(colors.surface2),
                        error = ColorPainter(colors.surface2),
                        modifier = Modifier.fillMaxWidth().height(220.dp),
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
                if (book.subjects.isNotEmpty()) {
                    FlowRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        book.subjects.take(8).forEach { subject ->
                            SubjectTag(text = subject, modifier = Modifier.padding(end = 4.dp, bottom = 4.dp))
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
            }
        }
    }
}
