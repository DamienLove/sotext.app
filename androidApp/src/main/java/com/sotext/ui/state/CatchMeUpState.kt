package com.sotext.ui.state

import com.sotext.data.sms.SmsThreadItem
import com.sotext.domain.model.CatchUpResult

/** One conversation's card in a Catch Me Up briefing: the thread it belongs to plus its AI result. */
data class CatchUpCard(
    val thread: SmsThreadItem,
    val contactName: String?,
    val result: CatchUpResult
)

sealed class CatchMeUpState {
    object Idle : CatchMeUpState()
    object Loading : CatchMeUpState()
    data class Success(
        val needsResponse: List<CatchUpCard>,
        val importantUpdates: List<CatchUpCard>,
        val noActionNeeded: List<CatchUpCard>,
        val generatedAt: Long
    ) : CatchMeUpState() {
        val isEmpty: Boolean
            get() = needsResponse.isEmpty() && importantUpdates.isEmpty() && noActionNeeded.isEmpty()
    }
    data class Error(val message: String) : CatchMeUpState()
}
