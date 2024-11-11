package com.eka.conversation.common.models

import com.eka.conversation.ui.presentation.models.BottomSectionConfiguration
import com.eka.conversation.ui.presentation.models.ContentSectionConfiguration
import com.eka.conversation.ui.presentation.models.ThreadScreenConfiguration
import com.eka.conversation.ui.presentation.models.TopBarConfiguration

data class ChatInitConfiguration(
    val networkConfiguration: NetworkConfiguration,
    val topBarConfiguration: TopBarConfiguration? = null,
    val contentSectionConfiguration: ContentSectionConfiguration? = null,
    val bottomSectionConfiguration: BottomSectionConfiguration? = null,
    val threadScreenConfiguration: ThreadScreenConfiguration? = null
)
