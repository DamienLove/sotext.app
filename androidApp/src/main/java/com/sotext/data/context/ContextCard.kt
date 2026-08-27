package com.sotext.data.context

/**
 * A piece of "actionable context" detected in a message body by [MessageContextParser],
 * surfaced inline in the thread as a small card the user can act on with one tap.
 */
sealed class ContextCard {
    /** Stable per-message, per-detection key, suitable for a Compose `key`. */
    abstract val id: String

    /** The substring of the original message this card was extracted from. */
    abstract val matchedText: String

    data class Event(
        override val id: String,
        override val matchedText: String,
        val title: String,
        val startMillis: Long,
        val endMillis: Long,
        val allDay: Boolean
    ) : ContextCard()

    data class Place(
        override val id: String,
        override val matchedText: String,
        val query: String
    ) : ContextCard()

    data class Phone(
        override val id: String,
        override val matchedText: String,
        val number: String
    ) : ContextCard()

    data class Link(
        override val id: String,
        override val matchedText: String,
        val url: String
    ) : ContextCard()

    data class Tracking(
        override val id: String,
        override val matchedText: String,
        val number: String,
        val carrier: String,
        val trackingUrl: String
    ) : ContextCard()

    data class VerificationCode(
        override val id: String,
        override val matchedText: String,
        val code: String
    ) : ContextCard()
}
