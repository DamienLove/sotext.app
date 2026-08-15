package com.sotext.ui.state

sealed class AiSummaryState {
    object Idle : AiSummaryState()
    object Loading : AiSummaryState()
    data class Success(val summary: String) : AiSummaryState()
    data class Error(val message: String) : AiSummaryState()
}

sealed class AiComposeState {
    object Idle : AiComposeState()
    object Loading : AiComposeState()
    data class Suggestion(val text: String) : AiComposeState()
    data class Error(val message: String) : AiComposeState()
}
