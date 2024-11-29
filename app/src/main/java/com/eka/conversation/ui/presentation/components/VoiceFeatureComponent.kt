package com.eka.conversation.ui.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eka.conversation.common.Response
import com.eka.conversation.ui.presentation.viewmodels.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceFeatureComponent(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel,
    onTranscriptionResult: (Response<String>) -> Unit,
) {
    // timer updates every 10 millis
    var recordingTime by remember { mutableStateOf(0L) }
    val currentTranscribedData by viewModel.currentTranscribeData.collectAsState(Response.Loading())
    var isRecording by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.clearRecording()
        isRecording = true
        viewModel.startAudioRecording()
    }

    LaunchedEffect(currentTranscribedData) {
        when (currentTranscribedData) {
            is Response.Loading -> {
                if (!isRecording) {
                    isLoading = true
                }
            }

            is Response.Success -> {
                isLoading = false
                onTranscriptionResult(currentTranscribedData)
            }

            is Response.Error -> {
                isLoading = false
                onTranscriptionResult(currentTranscribedData)
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTime = 0
            while (isRecording) {
                delay(10)
                recordingTime += 10
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.3f)
    ) {
        if (isRecording) {
            RecordingScreen(
                recordingTime = recordingTime,
                onStopRecording = {
                    isRecording = false
                    isLoading = true
                    coroutineScope.launch {
                        viewModel.stopAudioRecording()
                    }
                }
            )
        } else if (isLoading) {
            LoadingScreen()
        }
    }
}

@Composable
fun RecordingScreen(
    recordingTime: Long,
    onStopRecording: () -> Unit
) {
    val timer = rememberUpdatedState(recordingTime)
    val timeString = remember(timer.value) {
        val minutes = timer.value / 60000
        val seconds = (timer.value % 60000) / 1000
        val millis = (timer.value % 1000) / 10
        String.format("%02d:%02d:%02d", minutes, seconds, millis)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Gray.copy(alpha = 0.6f))
            .padding(16.dp)
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Recording Timer
            Text(
                text = timeString,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Stop Button
            Button(
                onClick = { onStopRecording() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = Color.Red
                ),
                modifier = Modifier
                    .size(64.dp)
                    .border(5.dp, Color.White, CircleShape)
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(Color.Red, CircleShape)
                )
            }
        }
    }
}

@Composable
fun AudioVisualizer(modifier: Modifier = Modifier) {
    val barHeights = remember { mutableStateOf(List(50) { (it + 1).dp }) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(100) // Adjust for smoothness
            barHeights.value = barHeights.value.map {
                (10..80).random().dp
            }
        }
    }

    Row(
        modifier
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        barHeights.value.forEach { height ->
            Box(
                Modifier
                    .width(5.dp)
                    .height(height)
                    .background(Color(0xFF03A9F4), RoundedCornerShape(50))
            )
        }
    }
}

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Gray.copy(alpha = 0.6f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = Color.White
        )
        Text(
            text = "Audio Processing...",
            color = Color.White
        )
    }
}