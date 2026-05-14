package com.opclient.subject.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.opclient.platform.PlatformConfig
import com.opclient.presentation.DetailStatus
import com.opclient.subject.domain.SubjectWork
import com.opclient.ui.components.BookRow
import com.opclient.ui.components.SecondaryButton
import com.opclient.ui.components.SectionLabel
import com.opclient.ui.components.feedback.ErrorState
import com.opclient.ui.components.feedback.LoadingState
import com.opclient.ui.theme.AppThemeTokens
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SubjectDetailScreen(
    subjectName: String,
    onBookClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SubjectDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(subjectName) {
        viewModel.onIntent(SubjectDetailIntent.Load(subjectName))
    }

    when (uiState.status) {
        DetailStatus.Loading -> LoadingState()
        DetailStatus.Error -> ErrorState(
            message = "Failed to load subject.",
            onRetry = { viewModel.onIntent(SubjectDetailIntent.Retry) },
        )
        DetailStatus.Success -> {
            if (PlatformConfig.useLazyPagination) {
                SubjectDetailLazyContent(uiState, onBack, onBookClick, viewModel)
            } else {
                SubjectDetailScrollContent(uiState, onBack, onBookClick, viewModel)
            }
        }
    }
}

@Composable
private fun SubjectDetailScrollContent(
    uiState: SubjectDetailUiState,
    onBack: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: SubjectDetailViewModel,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        SecondaryButton(
            text = "← BACK",
            onClick = onBack,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        SectionLabel(
            text = uiState.subjectName,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        uiState.works.forEach { work ->
            SubjectWorkRow(work, onBookClick)
        }
        if (uiState.canLoadMore) {
            SecondaryButton(
                text = "Load more",
                onClick = { viewModel.onIntent(SubjectDetailIntent.LoadMore) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        if (uiState.isLoadingMore) {
            LoadingState()
        }
    }
}

@Composable
private fun SubjectDetailLazyContent(
    uiState: SubjectDetailUiState,
    onBack: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: SubjectDetailViewModel,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= uiState.works.size - 3 && uiState.canLoadMore && !uiState.isLoadingMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.onIntent(SubjectDetailIntent.LoadMore)
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            SecondaryButton(
                text = "← BACK",
                onClick = onBack,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        item {
            SectionLabel(
                text = uiState.subjectName,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        items(uiState.works, key = { it.key }) { work ->
            SubjectWorkRow(work, onBookClick)
        }
        if (uiState.isLoadingMore) {
            item { LoadingState() }
        }
    }
}

@Composable
private fun SubjectWorkRow(work: SubjectWork, onBookClick: (String) -> Unit) {
    val colors = AppThemeTokens.colors
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
    )
}
