package com.eka.conversation.ui.presentation.states

sealed class ChatUiState {
    object ChatInitLoading : ChatUiState()
    object ChatInitError : ChatUiState()
    object ChatInitSuccess : ChatUiState()
}