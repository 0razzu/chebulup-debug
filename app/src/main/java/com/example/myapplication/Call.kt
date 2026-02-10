package com.example.myapplication

import android.content.Context
import android.util.Log
import org.pjsip.pjsua2.Account
import org.pjsip.pjsua2.AccountConfig
import org.pjsip.pjsua2.AuthCredInfo
import org.pjsip.pjsua2.Call
import org.pjsip.pjsua2.CallOpParam
import org.pjsip.pjsua2.Endpoint
import org.pjsip.pjsua2.EpConfig
import org.pjsip.pjsua2.TransportConfig
import org.pjsip.pjsua2.pjsip_transport_type_e


interface VoipManager {
    fun login(username: String, password: String, domain: String)
    fun call(username: String, domain: String)
}

class VoipManagerV1(context: Context) : VoipManager {
    private lateinit var acc: Account
    private var ep: Endpoint

    companion object {
        private const val TAG = "VoipManagerV1"
    }

    init {
        System.loadLibrary("pjsua2")

        ep = Endpoint()
        ep.libCreate()
        ep.libInit(EpConfig())
        val sipTpConfig = TransportConfig()
        sipTpConfig.port = 5060
        ep.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, sipTpConfig)

        ep.libStart()
    }

    override fun login(username: String, password: String, domain: String) {
        val accCfg = AccountConfig()
        accCfg.idUri = "sip:$username@$domain"
        accCfg.regConfig.registrarUri = "sip:$domain"
        val cred = AuthCredInfo("digest", "*", username, 0, password)
        accCfg.sipConfig.authCreds.add(cred)

        acc = Account()
        acc.create(accCfg)
    }

    override fun call(username: String, domain: String) {
        val call = Call(acc)
        try {
            call.makeCall("sip:$username@$domain", CallOpParam(true))
        } catch (e: Exception) {
            call.delete()
            Log.e(TAG, "Failed to call", e)
        }
    }
}