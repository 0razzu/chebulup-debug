package com.example.myapplication

import android.content.Context
import android.util.Log
import org.linphone.core.Core
import org.linphone.core.Factory

interface VoipManager {
    fun login(username: String, password: String, domain: String)
    fun call(extension: String)
}

class VoipManagerV1(context: Context): VoipManager {
    private val core: Core
    private val factory = Factory.instance()

    init {
        factory.setDebugMode(true, "Linphone")
        AudioHook.install()
        core = factory.createCore(null, null, context)
        core.start()
        core.enableMic(true)
    }

    fun iterate() {
        core.iterate()
    }

    override fun login(username: String, password: String, domain: String) {
        val authInfo = Factory.instance().createAuthInfo(
            username,
            null,
            password,
            null,
            null,
            domain,
        )
        core.addAuthInfo(authInfo)

        val proxyConfig = core.createProxyConfig()
        proxyConfig.identityAddress = factory.createAddress("sip:$username@$domain")
        proxyConfig.serverAddr = factory.createAddress("sip:$domain")!!.asStringUriOnly()
        proxyConfig.enableRegister(true)
        core.addProxyConfig(proxyConfig)
        core.defaultProxyConfig = proxyConfig

//        val accountParams = core.createAccountParams()
//        accountParams.identityAddress = Factory.instance().createAddress("sip:$username@$domain")
//        accountParams.serverAddress = Factory.instance().createAddress("sip:$domain")
//        accountParams.isRegisterEnabled = true
//        val account = core.createAccount(accountParams)
//
//        core.addAuthInfo(authInfo)
//        core.addAccount(account)
//        core.defaultAccount = account
    }

    override fun call(extension: String) {
        val proxy = core.defaultProxyConfig ?: return
        val address = factory.createAddress("sip:$extension@${proxy.domain}")
        core.inviteAddress(address!!)
    }
}