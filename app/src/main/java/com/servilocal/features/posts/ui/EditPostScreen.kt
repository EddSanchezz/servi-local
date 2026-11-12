package com.servilocal.features.posts.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servilocal.R
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.servilocal.features.posts.viewmodel.CreatePostViewModel
import com.servilocal.features.posts.viewmodel.EditPostViewModel

@Composable
fun EditPostScreen(
    postId: String,
    viewModel: EditPostViewModel,
    createViewModel: CreatePostViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val post by viewModel.post.collectAsStateWithLifecycle()
    val editUiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(editUiState.isDeleted) {
        if (editUiState.isDeleted) onDeleted()
    }
    if (post == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    CreatePostScreen(
        viewModel = createViewModel,
        onBack = onBack,
        onOpenHome = onBack,
        onOpenProfile = onBack,
        editPost = post,
        editViewModel = viewModel,
        onEditSaved = onBack,
        modifier = modifier
    )
}
