// composeApp/src/commonMain/kotlin/com/opclient/author/presentation/AuthorDetailScreen.kt
package com.opclient.author.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.opclient.presentation.DetailStatus
import com.opclient.ui.components.BookRow
import com.opclient.ui.components.SecondaryButton
import com.opclient.ui.components.SectionLabel
import com.opclient.ui.components.feedback.ErrorState
import com.opclient.ui.components.feedback.LoadingState
import com.opclient.ui.theme.AppThemeTokens
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthorDetailScreen(
    authorKey: String,
    onBookClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: AuthorDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography

    LaunchedEffect(authorKey) {
        viewModel.onIntent(AuthorDetailIntent.Load(authorKey))
    }

    when (uiState.status) {
        DetailStatus.Loading -> LoadingState()
        DetailStatus.Error -> ErrorState(
            message = "Failed to load author.",
            onRetry = { viewModel.onIntent(AuthorDetailIntent.Retry) },
        )
        DetailStatus.Success -> {
            val author = uiState.author ?: return
            LazyColumn(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                item {
                    SecondaryButton(
                        text = "← BACK",
                        onClick = onBack,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                item {
                    if (author.photoUrl != null) {
                        AsyncImage(
                            model = author.photoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            placeholder = ColorPainter(colors.surface2),
                            error = ColorPainter(colors.surface2),
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .size(80.dp)
                                .clip(CircleShape),
                        )
                    }
                }
                item {
                    SectionLabel(
                        text = author.name,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (author.birthDate != null || author.deathDate != null) {
                    item {
                        val dates = listOfNotNull(author.birthDate, author.deathDate).joinToString(" – ")
                        BasicText(
                            text = dates,
                            style = typography.bookAuthor.copy(color = colors.textSecondary),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
                if (author.bio != null) {
                    item {
                        BasicText(
                            text = author.bio,
                            style = typography.bookAuthor.copy(color = colors.textPrimary),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                item {
                    SectionLabel(
                        text = "WORKS",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(author.works, key = { it.key }) { work ->
                    BookRow(
                        title = work.title,
                        author = "",
                        coverContent = {
                            if (work.coverUrl != null) {
                                AsyncImage(
                                    model = work.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    placeholder = ColorPainter(colors.surface2),
                                    error = ColorPainter(colors.surface2),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize())
                            }
                        },
                        onClick = { onBookClick(work.key) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}
