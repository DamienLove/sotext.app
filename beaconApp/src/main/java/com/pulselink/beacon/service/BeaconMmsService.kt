package com.pulselink.beacon.service

import android.app.Activity
import android.app.PendingIntent
import android.service.carrier.CarrierMessagingService
import android.service.carrier.MessagePdu

/**
 * Minimal CarrierMessagingService stub to satisfy default-SMS contract.
 * It immediately reports success; carrier MMS/SMS send-receive should be
 * verified on device. Replace with full MMS stack if needed.
 */
class BeaconMmsService : CarrierMessagingService() {
    override fun onReceiveTextSms(
        pdu: MessagePdu?,
        format: String?,
        destPort: Int,
        callback: ResultCallback<Int>
    ) {
        callback.onReceiveResult(RECEIVE_OPTIONS_DEFAULT)
    }

    override fun onSendTextSms(
        text: String,
        subId: Int,
        destAddress: String,
        sentIntent: PendingIntent?
    ) {
        sentIntent?.send(Activity.RESULT_OK)
    }

    override fun onSendDataSms(
        pdu: ByteArray?,
        subId: Int,
        destAddress: String?,
        destPort: Int,
        sentIntent: PendingIntent?
    ) {
        sentIntent?.send(Activity.RESULT_OK)
    }

    override fun onSendMultipartTextSms(
        parts: MutableList<String>?,
        subId: Int,
        destAddress: String?,
        sentIntents: MutableList<PendingIntent>?,
        deliveryIntents: MutableList<PendingIntent>?
    ) {
        sentIntents?.forEach { it.send(Activity.RESULT_OK) }
    }

    override fun onSendMms(
        pdu: MutableList<ByteArray>?,
        subId: Int,
        location: String?,
        sentIntent: PendingIntent?
    ) {
        sentIntent?.send(Activity.RESULT_OK)
    }

    override fun onDownloadMms(
        locationUrl: String?,
        subId: Int,
        downloadedIntent: PendingIntent?
    ) {
        downloadedIntent?.send(Activity.RESULT_OK)
    }
}
