package com.eka.conversation.ui.presentation.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.eka.conversation.common.Utils
import com.eka.conversation.ui.presentation.models.BottomSectionConfiguration
import com.eka.conversation.ui.theme.Gray200

@Composable
fun ChatScreenBottomSection(
    modifier: Modifier = Modifier,
    onInputChange : (String) -> Unit,
    bottomSectionConfiguration: BottomSectionConfiguration = BottomSectionConfiguration.defaults()
) {
    val context = LocalContext.current.applicationContext
    var textInputState by remember {
        mutableStateOf("")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bottomSectionConfiguration.leadingIcon?.let {
            IconButton(onClick = bottomSectionConfiguration.onLeadingIconClick) {
                it.invoke()
            }
        }
        bottomSectionConfiguration.chatInputAreaConfiguration.let { chatInputAreaConfig ->
            TextField(
                modifier = chatInputAreaConfig.modifier
                    .padding(8.dp)
                    .weight(1f),
                value = textInputState,
                onValueChange = { newValue ->
                    textInputState = newValue
                    onInputChange(newValue)
                },
                enabled = true,
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Gray200,
                    unfocusedContainerColor = Gray200,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                placeholder = {
                    chatInputAreaConfig.hint?.invoke()
                },
                leadingIcon = chatInputAreaConfig.leadingIcon,
                trailingIcon = chatInputAreaConfig.trailingIcon,
                singleLine = true,
                maxLines = 1
            )
        }
        bottomSectionConfiguration.trailingIcon?.let {
            IconButton(onClick = {
                if(Utils.isNetworkAvailable(context = context)) {
                    bottomSectionConfiguration.onTrailingIconClick.invoke()
                    textInputState = ""
                } else {
                    Toast.makeText(context, "No Internet!", Toast.LENGTH_SHORT).show()
                }
            }) {
                it.invoke()
            }
        }
    }
}