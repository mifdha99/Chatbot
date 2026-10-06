package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

fun chatViewModelFactory(application: Application): ViewModelProvider.Factory {
    return viewModelFactory {
        initializer {
            ChatViewModel(application)
        }
    }
}
