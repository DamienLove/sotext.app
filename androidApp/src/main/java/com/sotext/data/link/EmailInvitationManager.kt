package com.sotext.data.link

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.sotext.auth.FirebaseAuthManager
import com.sotext.domain.model.Contact
import com.sotext.domain.model.LinkStatus
import com.sotext.domain.repository.ContactRepository
import com.sotext.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling email-based invitations for PulseLink contacts.
 * Coordinates with Firebase Cloud Functions to send invitation emails and
 * track invitation status in Firestore.
 */
@Singleton
class EmailInvitationManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val authManager: FirebaseAuthManager
) {
    companion object {
        private const val TAG = "EmailInvitationManager"
        private const val INVITATIONS_COLLECTION = "emailInvitations"
        private const val SEND_EMAIL_FUNCTION = "sendEmailInvitation"
        private const val CHECK_STATUS_FUNCTION = "checkInvitationStatus"
        private const val ACCEPT_INVITATION_FUNCTION = "acceptInvitation"
    }

    /**
     * Send an email invitation to a contact.
     * 
     * @param contactId The local database ID of the contact
     * @return Result with success message or error
     */
    suspend fun sendEmailInvitation(contactId: Long): Result<String> {
        return try {
            // Validate authentication
            val currentUser = authManager.currentUser()
            if (currentUser == null) {
                return Result.failure(Exception("User must be signed in to send invitations"))
            }

            // Get contact from repository
            val contact = contactRepository.getContact(contactId)
                ?: return Result.failure(Exception("Contact not found"))

            // Validate contact has email
            if (contact.email.isNullOrBlank()) {
                return Result.failure(Exception("Contact does not have an email address"))
            }

            // Generate or retrieve invitation code (similar to SMS link code)
            val invitationCode = generateInvitationCode()

            // Get sender name from settings
            val senderName = settingsRepository.settings.first().ownerName.ifBlank { "SoText User" }
            val senderDeviceId = settingsRepository.ensureDeviceId()

            // Prepare data for Cloud Function
            val data = hashMapOf(
                "recipientEmail" to contact.email!!,
                "senderName" to senderName,
                "senderDeviceId" to senderDeviceId,
                "invitationCode" to invitationCode,
                "contactId" to contactId
            )

            Log.d(TAG, "Sending email invitation to ${contact.email}")

            // Call Cloud Function
            val result = functions
                .getHttpsCallable(SEND_EMAIL_FUNCTION)
                .call(data)
                .await()

            val responseData = result.data as? Map<*, *>
            val success = responseData?.get("success") as? Boolean ?: false

            if (success) {
                val invitationId = responseData?.get("invitationId") as? String
                Log.d(TAG, "Email invitation sent successfully. ID: $invitationId")

                // Update contact status to OUTBOUND_PENDING
                updateContactLinkStatus(contactId, LinkStatus.OUTBOUND_PENDING, invitationCode)

                // Store invitation metadata locally if needed
                storeInvitationMetadata(contactId, invitationCode, invitationId)

                Result.success("Invitation email sent to ${contact.email}")
            } else {
                val message = responseData?.get("message") as? String ?: "Unknown error"
                Result.failure(Exception("Failed to send invitation: $message"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error sending email invitation", e)
            Result.failure(Exception("Failed to send invitation email: ${e.message}", e))
        }
    }

    /**
     * Check the status of an invitation by code.
     * 
     * @param invitationCode The invitation code to check
     * @return Result with invitation details or error
     */
    suspend fun checkInvitationStatus(invitationCode: String): Result<InvitationStatus> {
        return try {
            // Validate authentication
            val currentUser = authManager.currentUser()
            if (currentUser == null) {
                return Result.failure(Exception("User must be authenticated"))
            }

            val data = hashMapOf("invitationCode" to invitationCode)

            Log.d(TAG, "Checking invitation status for code: $invitationCode")

            val result = functions
                .getHttpsCallable(CHECK_STATUS_FUNCTION)
                .call(data)
                .await()

            val responseData = result.data as? Map<*, *>
            val success = responseData?.get("success") as? Boolean ?: false

            if (success) {
                val invitation = responseData?.get("invitation") as? Map<*, *>
                if (invitation != null) {
                    val status = InvitationStatus(
                        senderName = invitation["senderName"] as? String ?: "Unknown",
                        senderDeviceId = invitation["senderDeviceId"] as? String ?: "",
                        status = invitation["status"] as? String ?: "UNKNOWN"
                    )
                    Result.success(status)
                } else {
                    Result.failure(Exception("Invalid invitation data received"))
                }
            } else {
                val message = responseData?.get("message") as? String ?: "Invitation not found"
                Result.failure(Exception(message))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking invitation status", e)
            Result.failure(Exception("Failed to check invitation: ${e.message}", e))
        }
    }

    /**
     * Handle acceptance of an email invitation (called when user clicks deep link).
     * 
     * @param invitationCode The invitation code from the deep link
     * @param acceptorEmail The email of the user accepting the invitation
     * @return Result with success or error
     */
    suspend fun handleInvitationAcceptance(
        invitationCode: String,
        acceptorEmail: String
    ): Result<String> {
        return try {
            // First check if invitation is valid
            val statusResult = checkInvitationStatus(invitationCode)
            if (statusResult.isFailure) {
                return Result.failure(
                    statusResult.exceptionOrNull() ?: Exception("Invalid invitation")
                )
            }

            val invitationStatus = statusResult.getOrNull()!!

            // Create or update contact with sender information
            val contactId = createOrUpdateContactFromInvitation(
                name = invitationStatus.senderName,
                deviceId = invitationStatus.senderDeviceId,
                email = acceptorEmail
            )

            // Mark invitation as accepted via Cloud Function
            val data = hashMapOf("invitationCode" to invitationCode)
            val result = functions
                .getHttpsCallable(ACCEPT_INVITATION_FUNCTION)
                .call(data)
                .await()

            val responseData = result.data as? Map<*, *>
            val success = responseData?.get("success") as? Boolean ?: false

            if (success) {
                // Update local contact status
                updateContactLinkStatus(contactId, LinkStatus.LINKED, null)

                Log.d(TAG, "Invitation accepted successfully")
                Result.success("Connected with ${invitationStatus.senderName}")
            } else {
                Result.failure(Exception("Failed to accept invitation"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling invitation acceptance", e)
            Result.failure(Exception("Failed to accept invitation: ${e.message}", e))
        }
    }

    /**
     * Generate a unique invitation code.
     */
    private fun generateInvitationCode(): String {
        // Generate a random 8-character alphanumeric code
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8)
            .map { chars.random() }
            .joinToString("")
    }

    /**
     * Store invitation metadata locally for tracking.
     */
    private suspend fun storeInvitationMetadata(
        contactId: Long,
        invitationCode: String,
        invitationId: String?
    ) {
        Log.d(TAG, "Invitation metadata stored contactId=$contactId code=$invitationCode id=${invitationId ?: ""}")
    }

    private suspend fun updateContactLinkStatus(
        contactId: Long,
        status: LinkStatus,
        linkCode: String?
    ) {
        val contact = contactRepository.getContact(contactId) ?: return
        val updated = contact.copy(
            linkStatus = status,
            linkCode = linkCode ?: contact.linkCode,
            pendingApproval = status == LinkStatus.OUTBOUND_PENDING
        )
        contactRepository.upsert(updated)
    }

    private suspend fun createOrUpdateContactFromInvitation(
        name: String,
        deviceId: String,
        email: String
    ): Long {
        val existing = contactRepository.getByRemoteDeviceId(deviceId)
            ?: contactRepository.getByEmail(email)
        val displayName = name.ifBlank { existing?.displayName ?: "SoText Contact" }
        val updated = (existing ?: Contact(displayName = displayName, email = email)).copy(
            displayName = displayName,
            email = email,
            remoteDeviceId = deviceId,
            linkStatus = LinkStatus.LINKED,
            pendingApproval = false
        )
        contactRepository.upsert(updated)
        val resolved = contactRepository.getByRemoteDeviceId(deviceId) ?: contactRepository.getByEmail(email)
        return resolved?.id ?: updated.id
    }

    /**
     * Data class representing invitation status.
     */
    data class InvitationStatus(
        val senderName: String,
        val senderDeviceId: String,
        val status: String
    )
}
