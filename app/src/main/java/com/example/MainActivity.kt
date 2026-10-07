package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.chatViewModelFactory
import com.example.ui.theme.MesraAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { enableEdgeToEdge() }
        setContent {
            MesraAITheme {
                val chatViewModel: ChatViewModel = viewModel(
                    factory = chatViewModelFactory(application)
                )
                ChatScreen(
                    viewModel = chatViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
