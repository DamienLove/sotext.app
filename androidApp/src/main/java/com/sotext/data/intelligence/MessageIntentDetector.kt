package com.sotext.data.intelligence

import com.sotext.data.context.ContextCard
import com.sotext.data.context.MessageContextParser
import java.time.Instant
import java.time.ZoneId

/**
 * On-device, regex/heuristic conversational-intent detection - the free, instant, no-network
 * baseline tier of the Message Intelligence pipeline (see [MessageIntelligenceRepository]).
 * Same style and guarantee as [MessageContextParser]: everything here runs locally against the
 * message text, nothing leaves the device.
 *
 * Rather than re-implementing date/time/phone/address extraction, this calls
 * [MessageContextParser.extract] once per message and reuses whatever entity cards it already
 * found (an [ContextCard.Event] becomes the reminder/scheduling date+time, a [ContextCard.Place]
 * becomes a location entity, a [ContextCard.Phone] becomes a phone-number entity) - this file
 * only adds the *intent* layer (which trigger phrase, if any, turns those entities into a
 * request) on top.
 *
 * Phase 1 implements real detection for [MessageIntent.REMINDER], [MessageIntent.SCHEDULING],
 * [MessageIntent.LOCATION_REQUEST], [MessageIntent.CONTACT_REQUEST],
 * [MessageIntent.PAYMENT_REQUEST]/[MessageIntent.MONEY_OWED], and a generic
 * [MessageIntent.INFORMATION_REQUEST] fallback for plain questions. The rest of [MessageIntent]
 * is defined for extensibility but has no detector wired here yet - adding one is a new private
 * `find*` function plus one line in [analyze], not a redesign.
 */
object MessageIntentDetector {

    fun analyze(
        messageId: Long,
        body: String,
        timestampMillis: Long,
        senderAddress: String? = null
    ): List<IntentMatch> {
        if (body.isBlank()) return emptyList()

        val contextCards = MessageContextParser.extract(messageId, body, timestampMillis, senderAddress)
        val eventCard = contextCards.filterIsInstance<ContextCard.Event>().firstOrNull()
        val placeCard = contextCards.filterIsInstance<ContextCard.Place>().firstOrNull()
        val phoneCard = contextCards.filterIsInstance<ContextCard.Phone>().firstOrNull()
        val zone = ZoneId.systemDefault()

        val dateTimeEntities = eventCard?.let { event ->
            val start = Instant.ofEpochMilli(event.startMillis).atZone(zone)
            MessageEntities(
                dateEpochDay = start.toLocalDate().toEpochDay(),
                timeMinuteOfDay = if (event.allDay) null else start.toLocalTime().let { it.hour * 60 + it.minute },
                rawDateTimeText = event.matchedText
            )
        } ?: MessageEntities()

        val matches = mutableListOf<IntentMatch>()
        findReminder(body, dateTimeEntities)?.let { matches += it }
        findScheduling(body, dateTimeEntities)?.let { matches += it }
        findLocationRequest(body, placeCard)?.let { matches += it }
        findContactRequest(body, phoneCard)?.let { matches += it }
        findPaymentRequest(body)?.let { matches += it }
        // Generic question fallback always runs, even alongside a more specific match - a
        // message can genuinely carry both (spec section 6's own example: a reminder *and* a
        // separate question in the same text). It naturally sorts below anything more specific
        // since its confidence is capped at medium.
        findInformationRequest(body)?.let { matches += it }

        return matches.sortedByDescending { it.confidence }
    }

    // ---------------------------------------------------------------------
    // Reminder
    // ---------------------------------------------------------------------

    private val explicitReminderTrigger = Regex("""(?i)\bremind\s+me\b""")
    private val impliedReminderTrigger = Regex(
        """(?i)\b(?:i (?:need|have|want|got) to remember|don'?t (?:let me )?forget|i should remember)\b"""
    )
    // "remind me to <task>" is the common case, but a time phrase can sit between "me" and "to"
    // ("remind me tomorrow to pick up Sarah") - the optional group tolerates that so the task
    // still gets extracted instead of coming back null on an otherwise EXPLICIT/high-confidence
    // match (spec section 6's own multi-intent example uses exactly this phrasing).
    private val reminderTaskRegex = Regex(
        """(?i)(?:remind\s+me\s+(?:(?:tomorrow|tonight|today|next\s+\w+|on\s+\S+|at\s+\S+|by\s+\S+)\s+)?to|remember\s+to|forget\s+to)\s+(.+?)""" +
            """(?=\s+(?:tomorrow|tonight|today|on\s|at\s|next\s|by\s)|[.!?]|$)"""
    )

    private fun findReminder(body: String, dateTime: MessageEntities): IntentMatch? {
        val task = reminderTaskRegex.find(body)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        return when {
            explicitReminderTrigger.containsMatchIn(body) -> IntentMatch(
                intent = MessageIntent.REMINDER,
                confidence = 0.9f,
                actionability = Actionability.EXPLICIT,
                entities = dateTime.copy(task = task)
            )
            impliedReminderTrigger.containsMatchIn(body) -> IntentMatch(
                intent = MessageIntent.REMINDER,
                // Deliberately below the medium threshold by default: "I need to remember to buy
                // milk" is exactly the spec's example of task-like language that should NOT
                // auto-create a reminder. A resolved date/time nudges it into "worth a subtle
                // suggestion" territory, but never all the way to an automatic card.
                confidence = if (dateTime.hasDateTime()) 0.55f else 0.4f,
                actionability = Actionability.IMPLIED,
                entities = dateTime.copy(task = task)
            )
            else -> null
        }
    }

    // ---------------------------------------------------------------------
    // Scheduling
    // ---------------------------------------------------------------------

    private val schedulingTrigger = Regex(
        """(?i)\b(?:are you free|you free|can (?:you|we) meet|does .+? work|""" +
            """when (?:are you free|can we meet)|available)\b"""
    )

    private fun findScheduling(body: String, dateTime: MessageEntities): IntentMatch? {
        if (!schedulingTrigger.containsMatchIn(body)) return null
        val isQuestion = body.trimEnd().endsWith("?")
        return IntentMatch(
            intent = MessageIntent.SCHEDULING,
            confidence = if (isQuestion) 0.85f else 0.5f,
            actionability = if (isQuestion) Actionability.EXPLICIT else Actionability.AMBIGUOUS,
            entities = dateTime
        )
    }

    // ---------------------------------------------------------------------
    // Location request
    // ---------------------------------------------------------------------

    private val locationTrigger = Regex(
        """(?i)\bwhere\s+(?:are|is|should|do)\b|\bwhat'?s\s+the\s+address\b|\bsend\s+(?:me\s+)?(?:the\s+)?address\b"""
    )

    private fun findLocationRequest(body: String, placeCard: ContextCard.Place?): IntentMatch? {
        if (!locationTrigger.containsMatchIn(body)) return null
        val isQuestion = body.trimEnd().endsWith("?")
        return IntentMatch(
            intent = MessageIntent.LOCATION_REQUEST,
            confidence = if (isQuestion) 0.85f else 0.6f,
            actionability = if (isQuestion) Actionability.EXPLICIT else Actionability.AMBIGUOUS,
            entities = MessageEntities(location = placeCard?.query)
        )
    }

    // ---------------------------------------------------------------------
    // Contact request
    // ---------------------------------------------------------------------

    private val contactTrigger = Regex(
        """(?i)\b(?:send|give|share)\s+me\b[^.!?]*\b(?:number|phone number|email|contact)\b|""" +
            """\bwhat'?s\b[^.!?]*\b(?:number|email)\b"""
    )
    private val personPossessiveRegex = Regex("""\b([A-Z][a-zA-Z]+)'s\s+(?:number|phone|email|contact)\b""")

    private fun findContactRequest(body: String, phoneCard: ContextCard.Phone?): IntentMatch? {
        if (!contactTrigger.containsMatchIn(body)) return null
        val person = personPossessiveRegex.find(body)?.groupValues?.getOrNull(1)
        return IntentMatch(
            intent = MessageIntent.CONTACT_REQUEST,
            confidence = 0.8f,
            actionability = Actionability.EXPLICIT,
            entities = MessageEntities(person = person, phoneNumber = phoneCard?.number)
        )
    }

    // ---------------------------------------------------------------------
    // Payment request / money owed
    // ---------------------------------------------------------------------

    private val amountRegex = Regex("""\$\s?(\d+(?:\.\d{1,2})?)|(\d+(?:\.\d{1,2})?)\s*dollars""", RegexOption.IGNORE_CASE)
    private val paymentRequestTrigger = Regex(
        """(?i)\b(?:can you send me|could you send me|send me|please send|venmo me|pay me)\b"""
    )
    private val moneyOwedTrigger = Regex("""(?i)\byou owe me\b""")

    private fun findPaymentRequest(body: String): IntentMatch? {
        val amountMatch = amountRegex.find(body)
        val amount = amountMatch?.let { m -> (m.groupValues[1].ifBlank { m.groupValues[2] }).toDoubleOrNull() }
        return when {
            paymentRequestTrigger.containsMatchIn(body) -> IntentMatch(
                intent = MessageIntent.PAYMENT_REQUEST,
                confidence = if (amount != null) 0.85f else 0.55f,
                actionability = if (amount != null) Actionability.EXPLICIT else Actionability.AMBIGUOUS,
                entities = MessageEntities(amount = amount)
            )
            moneyOwedTrigger.containsMatchIn(body) -> IntentMatch(
                intent = MessageIntent.MONEY_OWED,
                confidence = if (amount != null) 0.8f else 0.5f,
                actionability = if (amount != null) Actionability.EXPLICIT else Actionability.AMBIGUOUS,
                entities = MessageEntities(amount = amount)
            )
            else -> null
        }
    }

    // ---------------------------------------------------------------------
    // Generic information request (fallback for a plain question)
    // ---------------------------------------------------------------------

    private fun findInformationRequest(body: String): IntentMatch? {
        if (!body.trimEnd().endsWith("?")) return null
        return IntentMatch(
            intent = MessageIntent.INFORMATION_REQUEST,
            // Deliberately medium, not high - a generic question is real signal but not specific
            // enough to warrant an automatic card on its own (spec section 7's "medium confidence
            // -> subtle suggestion" tier), matching how it's used as a *secondary* intent in the
            // spec's multi-intent example rather than a primary one.
            confidence = 0.55f,
            actionability = Actionability.EXPLICIT,
            entities = MessageEntities()
        )
    }
}

data class IntentMatch(
    val intent: MessageIntent,
    val confidence: Float,
    val actionability: Actionability,
    val entities: MessageEntities
)
