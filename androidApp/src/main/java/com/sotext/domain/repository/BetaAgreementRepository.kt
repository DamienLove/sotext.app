package com.sotext.domain.repository

interface BetaAgreementRepository {
    suspend fun recordAgreement(name: String, agreementVersion: String)
}
