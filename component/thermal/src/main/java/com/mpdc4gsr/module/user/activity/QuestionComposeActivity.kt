package com.mpdc4gsr.module.user.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.component.shared.app.config.RouterConfig
import com.mpdc4gsr.component.shared.app.navigation.NavigationManager
import com.mpdc4gsr.module.user.model.QuestionData
import com.mpdc4gsr.module.user.viewmodel.QuestionViewModel

class QuestionComposeActivity : BaseComposeActivity<QuestionViewModel>() {
    override fun createViewModel(): QuestionViewModel = viewModels<QuestionViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: QuestionViewModel) {
        val questions by viewModel.questions.collectAsState()
        // Get isTS001 from intent extras
        val isTS001 = intent.getBooleanExtra("isTS001", false)
        // Load questions on start
        LaunchedEffect(Unit) {
            viewModel.loadQuestions(isTS001)
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "FAQ",
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(questions) { question ->
                    QuestionItem(
                        question = question,
                        onClick = {
                            NavigationManager
                                .getInstance()
                                .build(RouterConfig.QUESTION_DETAILS)
                                .withString("question", question.question)
                                .withString("answer", question.answer)
                                .navigation(this@QuestionComposeActivity)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionItem(
    question: QuestionData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View Answer",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}


