package com.sotext.data.intelligence

/** Which of the four taxonomy groups an [MessageIntent] belongs to. */
enum class IntentCategory {
    TASK,
    REQUEST,
    COMMUNICATION,
    TRANSACTION
}

/**
 * The full conversational-intent taxonomy the Message Intelligence pipeline can classify a
 * message as. Deliberately exhaustive (every intent named in the product spec) so nothing needs
 * redesigning to add a new one later - see [MessageIntentDetector] for which of these currently
 * have real detection logic (Phase 1: REMINDER, SCHEDULING, LOCATION_REQUEST, CONTACT_REQUEST,
 * PAYMENT_REQUEST, INFORMATION_REQUEST) versus which are defined but not yet wired to any
 * detector (everything else - they simply never match, so nothing breaks; implementing one is
 * "add a branch to the `when`", not a redesign).
 */
enum class MessageIntent(val category: IntentCategory) {
    // Tasks
    REMINDER(IntentCategory.TASK),
    TODO(IntentCategory.TASK),
    FOLLOW_UP(IntentCategory.TASK),
    CALL_SOMEONE(IntentCategory.TASK),
    MESSAGE_SOMEONE(IntentCategory.TASK),
    SCHEDULE(IntentCategory.TASK),
    RESCHEDULE(IntentCategory.TASK),
    CANCEL(IntentCategory.TASK),

    // Requests
    INFORMATION_REQUEST(IntentCategory.REQUEST),
    CONTACT_REQUEST(IntentCategory.REQUEST),
    LOCATION_REQUEST(IntentCategory.REQUEST),
    RECOMMENDATION_REQUEST(IntentCategory.REQUEST),
    HELP_REQUEST(IntentCategory.REQUEST),
    AVAILABILITY_REQUEST(IntentCategory.REQUEST),
    PERMISSION_REQUEST(IntentCategory.REQUEST),
    SCHEDULING(IntentCategory.REQUEST),

    // Communication
    QUESTION(IntentCategory.COMMUNICATION),
    CONFIRMATION(IntentCategory.COMMUNICATION),
    INVITATION(IntentCategory.COMMUNICATION),
    DECLINE(IntentCategory.COMMUNICATION),
    APOLOGY(IntentCategory.COMMUNICATION),
    THANK_YOU(IntentCategory.COMMUNICATION),

    // Transactions
    PAYMENT_REQUEST(IntentCategory.TRANSACTION),
    MONEY_OWED(IntentCategory.TRANSACTION),
    REIMBURSEMENT_REQUEST(IntentCategory.TRANSACTION),

    // No actionable intent detected.
    NONE(IntentCategory.COMMUNICATION)
}
