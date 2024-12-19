package com.eka.conversation.ui.presentation.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.eka.conversation.R
import com.eka.conversation.common.PermissionUtils
import com.eka.conversation.common.Response
import com.eka.conversation.common.Utils
import com.eka.conversation.common.models.ChatInitConfiguration
import com.eka.conversation.features.audio.DefaultAudioProcessor
import com.eka.conversation.ui.presentation.models.BottomSectionConfiguration
import com.eka.conversation.ui.presentation.viewmodels.ChatViewModel
import com.eka.conversation.ui.theme.Gray200

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenBottomSection(
    modifier: Modifier = Modifier,
    onInputChange : (String) -> Unit,
    chatInitConfiguration: ChatInitConfiguration,
    viewModel: ChatViewModel,
    bottomSectionConfiguration: BottomSectionConfiguration = BottomSectionConfiguration.defaults()
) {
    val context = LocalContext.current.applicationContext
    val currentTranscribeData by viewModel.currentTranscribeData.collectAsState(Response.Loading())
    var textInputState by remember {
        mutableStateOf("")
    }
    val isKeyboardOpen = WindowInsets.isImeVisible
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    val defaultAudioProcessor = DefaultAudioProcessor(context)
    var audioFilePath by remember {
        mutableStateOf("")
    }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (audioFilePath.isNotEmpty()) {
            AudioFileView(
                audioFilePath = audioFilePath
            ) {
                audioFilePath = ""
            }
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomSectionConfiguration.leadingIcon?.let {
                IconButton(onClick = bottomSectionConfiguration.onLeadingIconClick) {
                    it.invoke()
                }
            }
            bottomSectionConfiguration.chatInputAreaConfiguration.let { chatInputAreaConfig ->
                OutlinedTextField(
                    modifier = chatInputAreaConfig.modifier
                        .padding(0.dp)
                        .weight(1f),
                    value = textInputState,
                    onValueChange = { newValue ->
                        textInputState = newValue
                        onInputChange(newValue)
                    },
                    enabled = !isRecording,
                    shape = RoundedCornerShape(50),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedTextColor = Color(0xFF1A1A1A),
                        unfocusedTextColor = Color(0xFF1A1A1A),
                        focusedBorderColor = Color(0xFF7A9FFF),
                        unfocusedBorderColor = Color(0xFFD1D1D1),
                        containerColor = Color.White,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    placeholder = {
                        chatInputAreaConfig.hint?.invoke()
                    },
                    leadingIcon = chatInputAreaConfig.leadingIcon,
                    trailingIcon = {
                        if (chatInitConfiguration.audioFeatureConfiguration.isEnabled) {
                            if (!textInputState.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            if (Utils.isNetworkAvailable(context = context)) {
                                                onQuerySubmit(context, bottomSectionConfiguration)
                                                textInputState = ""
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "No Internet!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                ) {
                                    chatInputAreaConfig.trailingIcon?.invoke()
                                }
                            } else {
                                IconButton(onClick = {
                                    if (PermissionUtils.hasRecordAudioPermission(context) && Utils.isNetworkAvailable(
                                            context
                                        )
                                    ) {
                                        keyboardController?.hide()
                                        isRecording = !isRecording
                                    } else if (!Utils.isNetworkAvailable(context)) {
                                        showErrorToast(context, "Internet not available.")
                                    } else {
                                        showErrorToast(
                                            context,
                                            "Microphone permission not granted."
                                        )
                                    }
                                }) {
                                    Icon(
                                        painter = painterResource(
                                            id = R.drawable.ic_chat_sdk_mic
                                        ),
                                        contentDescription = "Voice"
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        if (Utils.isNetworkAvailable(context = context)) {
                                            onQuerySubmit(context, bottomSectionConfiguration)
                                            textInputState = ""
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "No Internet!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            ) {
                                chatInputAreaConfig.trailingIcon?.invoke()
                            }
                        }
                    },
                    singleLine = false,
                    maxLines = 3,
                )
            }
        }
        if (isRecording) {
            VoiceFeatureComponent(
                modifier = Modifier,
                viewModel = viewModel
            ) { response ->
                isRecording = false
                when (response) {
                    is Response.Loading -> {
                    }

                    is Response.Success -> {
                        val transcribedText = response.data.toString()
                        textInputState = transcribedText
                        onInputChange(transcribedText)
                        viewModel.clearRecording()
                    }

                    is Response.Error -> {
                        val errorMsg = response.message.toString()
                        showErrorToast(context, errorMsg)
                        viewModel.clearRecording()
                    }
                }
            }
        } else {
            viewModel.stopAudioRecordingInFileIfStarted()
        }
    }
}

private fun showErrorToast(
    context: Context,
    msg: String
) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

private fun onQuerySubmit(
    context: Context,
    bottomSectionConfiguration: BottomSectionConfiguration
) {
    if (Utils.isNetworkAvailable(context = context)) {
        bottomSectionConfiguration.onTrailingIconClick.invoke()
    } else {
        Toast.makeText(context, "No Internet!", Toast.LENGTH_SHORT).show()
    }
}