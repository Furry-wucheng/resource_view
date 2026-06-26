package dev.wucheng.resource_viewer.ui.screens.knowledge

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.R

/**
 * M16: 知识页面
 *
 * 占位页面，后续实现具体内容。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_knowledge)) },
                windowInsets = WindowInsets(0.dp),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Knowledge Screen",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
