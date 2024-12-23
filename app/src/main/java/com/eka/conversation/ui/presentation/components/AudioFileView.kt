package com.eka.conversation.ui.presentation.components

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eka.conversation.R
import com.eka.conversation.common.Utils
import kotlinx.coroutines.delay

@Composable
fun AudioFileView(
    audioFilePath: String,
    modifier: Modifier = Modifier,
    onRemove: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(Unit) {
        try {
            mediaPlayer.setDataSource(audioFilePath)
            mediaPlayer.prepare()
            duration = mediaPlayer.duration.toLong()

            mediaPlayer.setOnCompletionListener {
                isPlaying = false
                stopAudio(it)
            }
        } catch (e: Exception) {
            Log.e("AudioFileView", "Error loading audio file", e)
        }

        onDispose {
            mediaPlayer.release()
        }
    }

    Row(
        modifier = modifier
            .padding(8.dp)
            .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                isPlaying = if (isPlaying) {
                    pauseAudio(mediaPlayer)
                    false
                } else {
                    mediaPlayer.start()
                    true
                }
            }
        ) {
            if (isPlaying) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pause),
                    contentDescription = "Pause"
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play"
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Show the current position and duration
            Text(
                text = Utils.formatMillisToMinutesSeconds(currentPosition),
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                progress = { if (duration > 0) currentPosition / duration.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth()
            )
        }

        IconButton(onClick = onRemove) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove")
        }
    }

    // Update current position periodically
    LaunchedEffect(mediaPlayer) {
        while (isPlaying) {
            currentPosition = mediaPlayer.currentPosition.toLong()
            delay(500) // Update every 500ms
        }
    }
}

fun pauseAudio(mediaPlayer: MediaPlayer?) {
    mediaPlayer?.let {
        it.pause()
    }
}

fun stopAudio(mediaPlayer: MediaPlayer?) {
    mediaPlayer?.let {
        it.stop()
        it.release()
    }
}

@Composable
@Preview
fun preT() {
    AudioFileView(audioFilePath = "") {

    }
}