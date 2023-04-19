package com.fsck.k9.ui.generatekey

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9Activity

// import fungsi generate key
import com.fsck.k9.ecdsa.curves.Secp256k1
import com.fsck.k9.ecdsa.KeyGenerator.generateKey
import java.math.BigInteger

class GenerateKeyActivity : K9Activity() {
    lateinit var privateKey : EditText
    lateinit var publicKey : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.activity_generate_key)
        setTitle(R.string.generate_key_action)

        privateKey = findViewById(R.id.editViewPrivateKey)
        publicKey = findViewById(R.id.textViewPublicKey)
    }

    fun stringToHex(input: String): String {
        return input.toByteArray().joinToString("") { "%02x".format(it) }
    }

    @SuppressLint("ServiceCast")
    fun copyPublicKey(view: View){
        // copy public key to clipboard
        val textToCopy = publicKey.text.toString()
        val clipBoardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("publicKey", textToCopy)
        clipBoardManager.setPrimaryClip(clipData)

        // show toast
        val toast = Toast.makeText(this, "Public Key Copied", Toast.LENGTH_SHORT)
        toast.show()
    }

    @SuppressLint("SetTextI18n")
    fun showPublicKey(view: View){
        val myString = privateKey.text.toString()
        val hexString = stringToHex(myString)
        val privateKeyString = BigInteger(hexString, 16)
        val keyPair = generateKey(privateKeyString, Secp256k1)
        publicKey.text = "${keyPair.publicKey.x},${keyPair.publicKey.y}"
        println("publickey y = " +publicKey.x)
        println("publickey y = " +publicKey.y)
    }

    companion object {
        @JvmStatic
        fun launch(activity: Activity) {
            val intent = Intent(activity, GenerateKeyActivity::class.java)
            activity.startActivity(intent)
        }
    }
}
