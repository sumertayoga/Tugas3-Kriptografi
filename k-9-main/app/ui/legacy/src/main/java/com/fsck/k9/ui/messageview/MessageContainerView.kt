package com.fsck.k9.ui.messageview

// algoritma decrypt
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.AttributeSet
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.OnCreateContextMenuListener
import android.webkit.WebView
import android.webkit.WebView.HitTestResult
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ShareCompat.IntentBuilder
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.fsck.k9.contact.ContactIntentHelper
import com.fsck.k9.doramei.decrypt
import com.fsck.k9.ecdsa.Point
import com.fsck.k9.helper.ClipboardManager
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.AttachmentResolver
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.message.html.DisplayHtml
import com.fsck.k9.ui.R
import com.fsck.k9.view.MessageWebView
import com.fsck.k9.view.MessageWebView.OnPageFinishedListener
import com.fsck.k9.view.WebViewConfigProvider
import java.util.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import com.fsck.k9.ecdsa.Sign.verifySignature
import com.fsck.k9.ecdsa.Signature
import com.fsck.k9.ecdsa.curves.Secp256k1
import java.math.BigInteger

class MessageContainerView(context: Context, attrs: AttributeSet?) :
    LinearLayout(context, attrs),
    OnCreateContextMenuListener,
    KoinComponent {

    private val displayHtml: DisplayHtml by inject(named("MessageView"))
    private val webViewConfigProvider: WebViewConfigProvider by inject()
    private val clipboardManager: ClipboardManager by inject()
    private val linkTextHandler: LinkTextHandler by inject()

    private lateinit var layoutInflater: LayoutInflater

    private lateinit var messageContentView: MessageWebView
    private lateinit var attachmentsContainer: ViewGroup
    private lateinit var unsignedTextContainer: View
    private lateinit var unsignedTextDivider: View
    private lateinit var unsignedText: TextView

    private var isShowingPictures = false
    private var currentHtmlText: String? = null
    private val attachmentViewMap = mutableMapOf<AttachmentViewInfo, AttachmentView>()
    private val attachments = mutableMapOf<Uri, AttachmentViewInfo>()
    private var attachmentCallback: AttachmentViewCallback? = null
    private var currentAttachmentResolver: AttachmentResolver? = null

    // decrypted text variable
    private var decryptText: Boolean = false
    private var keyTodecrypt: String? = null

    private var keyToVerify: String? = null
    private var msginHtml: String? = null



    @get:JvmName("hasHiddenExternalImages")
    var hasHiddenExternalImages = false
        private set

    public override fun onFinishInflate() {
        super.onFinishInflate()

        layoutInflater = LayoutInflater.from(context)
        showDialogBox()
        messageContentView = findViewById<MessageWebView>(R.id.message_content).apply {
            if (!isInEditMode) {
                configure(webViewConfigProvider.createForMessageView())
            }

            setOnCreateContextMenuListener(this@MessageContainerView)
            visibility = VISIBLE
        }
        attachmentsContainer = findViewById(R.id.attachments_container)
        unsignedTextContainer = findViewById(R.id.message_unsigned_container)
        unsignedTextDivider = findViewById(R.id.message_unsigned_divider)
        unsignedText = findViewById(R.id.message_unsigned_text)
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu)
        val webView = view as WebView
        val hitTestResult = webView.hitTestResult

        when (hitTestResult.type) {
            HitTestResult.SRC_ANCHOR_TYPE -> {
                createLinkMenu(menu, webView, linkUrl = hitTestResult.extra)
            }
            HitTestResult.IMAGE_TYPE, HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                createImageMenu(menu, imageUrl = hitTestResult.extra)
            }
            HitTestResult.PHONE_TYPE -> {
                createPhoneNumberMenu(menu, phoneNumber = hitTestResult.extra)
            }
            HitTestResult.EMAIL_TYPE -> {
                createEmailMenu(menu, email = hitTestResult.extra)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDialogBox() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Decrypt Text?")
        builder.setMessage("Masukan Kunci")

        // tambahkan input field ke dalam dialog box
        val editText = EditText(context)
        val editText2 = EditText(context)
        val inputTitle1 = TextView(context)
        val inputTitle2 = TextView(context)
        inputTitle1.text = "Key Cipher"
        inputTitle2.text = "Key ECDSA"

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.addView(inputTitle1)
        layout.addView(editText)
        layout.addView(inputTitle2)
        layout.addView(editText2)
        builder.setView(layout)
        builder.setPositiveButton("OK") { dialog, which ->
            // mengambil value dari input field
            this.keyTodecrypt = editText.text.toString()
            this.keyToVerify = editText2.text.toString()

            println("thishtml"+this.msginHtml)

            val doc: Document = Jsoup.parse(this.msginHtml!!)
            var div = doc.select("div").first()
            // ------------------ Doramei Decrypt -------------
            if (this.keyTodecrypt != null && this.keyTodecrypt != ""){

                if (div != null) {
                    val divText = div.text()
                    val decryptedText = decrypt(divText, this.keyTodecrypt!!)
                    println("decrypt :"+ decryptedText)
                    div.text(decryptedText)
                    println("decryptext" + decryptedText)
                }

            }

            // ----------------------------------------------

            // ---------------- ECDSA Verify ------------------
            if (this.keyToVerify != null &&  this.keyToVerify != ""){
                if (div!=null){
                    val text: String = div.text()  // Mendapatkan teks dari elemen div
                    println(text)

                    if (text!=""){
                        if(text.indexOf("<ds>") != -1 && text.indexOf("</ds>") !=-1 ){
                            val ds = text.substring(
                                text.indexOf("<ds>") +4,
                                text.indexOf("</ds>"),
                            ).replace(" ", "")

                            println("ds =" + ds)
                            val msg = text.substring(
                                0,
                                text.indexOf("<ds>")-1
                            )
                            println(msg)
                            val data = msg.toByteArray()
                            // testing
                            this.keyToVerify = "55066263022277343669578718895168534326250603453777594175500187360389116729240,83121579216557378445487899878180864668798711284981320763518679672151497189239"
                            val splitStrings = this.keyToVerify!!.split(",")
                            val x = BigInteger(splitStrings[0])
                            val y = BigInteger(splitStrings[1])
                            val isValid = verifySignature(Point(x,y,Secp256k1) ,data, ds)
                            println("is valid: ${isValid}")
                        }
                    }

                }


            }

            val modifiedHtml = doc.toString()
            messageContentView.changeHtmlContent(modifiedHtml)
        }
        builder.setNegativeButton("Batal") { dialog, which ->
            // melakukan sesuatu ketika tombol "Batal" ditekan
            this.decryptText = false
        }

        // membuat dialog box
        val dialog = builder.create()
        dialog.show()
    }

    private fun createLinkMenu(
        menu: ContextMenu,
        webView: WebView,
        linkUrl: String?,
    ) {
        if (linkUrl == null) return

        val listener = MenuItem.OnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_ITEM_LINK_VIEW -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
                    startActivityIfAvailable(context, intent)
                }
                MENU_ITEM_LINK_SHARE -> {
                    IntentBuilder(context)
                        .setType("text/plain")
                        .setText(linkUrl)
                        .startChooser()
                }
                MENU_ITEM_LINK_COPY -> {
                    val label = context.getString(R.string.webview_contextmenu_link_clipboard_label)
                    clipboardManager.setText(label, linkUrl)
                }
                MENU_ITEM_LINK_TEXT_COPY -> {
                    val message = linkTextHandler.obtainMessage()
                    webView.requestFocusNodeHref(message)
                }
            }
            true
        }

        menu.setHeaderTitle(linkUrl)

        menu.add(
            Menu.NONE,
            MENU_ITEM_LINK_VIEW,
            0,
            context.getString(R.string.webview_contextmenu_link_view_action),
        ).setOnMenuItemClickListener(listener)

        menu.add(
            Menu.NONE,
            MENU_ITEM_LINK_SHARE,
            1,
            context.getString(R.string.webview_contextmenu_link_share_action),
        ).setOnMenuItemClickListener(listener)

        menu.add(
            Menu.NONE,
            MENU_ITEM_LINK_COPY,
            2,
            context.getString(R.string.webview_contextmenu_link_copy_action),
        ).setOnMenuItemClickListener(listener)

        menu.add(
            Menu.NONE,
            MENU_ITEM_LINK_TEXT_COPY,
            3,
            context.getString(R.string.webview_contextmenu_link_text_copy_action),
        ).setOnMenuItemClickListener(listener)
    }

    private fun createImageMenu(menu: ContextMenu, imageUrl: String?) {
        if (imageUrl == null) return

        val imageUri = Uri.parse(imageUrl)
        val attachmentViewInfo = getAttachmentViewInfoIfCidUri(imageUri)
        val inlineImage = attachmentViewInfo != null

        val listener = MenuItem.OnMenuItemClickListener { item ->
            val attachmentCallback = checkNotNull(attachmentCallback)

            when (item.itemId) {
                MENU_ITEM_IMAGE_VIEW -> {
                    if (inlineImage) {
                        attachmentCallback.onViewAttachment(attachmentViewInfo)
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, imageUri)
                        startActivityIfAvailable(context, intent)
                    }
                }
                MENU_ITEM_IMAGE_SAVE -> {
                    if (inlineImage) {
                        attachmentCallback.onSaveAttachment(attachmentViewInfo)
                    } else {
                        downloadImage(imageUri)
                    }
                }
                MENU_ITEM_IMAGE_COPY -> {
                    val label = context.getString(R.string.webview_contextmenu_image_clipboard_label)
                    clipboardManager.setText(label, imageUri.toString())
                }
            }
            true
        }

        if (inlineImage) {
            menu.setHeaderTitle(R.string.webview_contextmenu_image_title)
        } else {
            menu.setHeaderTitle(imageUrl)
        }

        menu.add(
            Menu.NONE,
            MENU_ITEM_IMAGE_VIEW,
            0,
            context.getString(R.string.webview_contextmenu_image_view_action),
        ).setOnMenuItemClickListener(listener)

        if (inlineImage || imageUri.scheme?.lowercase() in supportedDownloadUriSchemes) {
            menu.add(
                Menu.NONE,
                MENU_ITEM_IMAGE_SAVE,
                1,
                if (inlineImage) {
                    context.getString(R.string.webview_contextmenu_image_save_action)
                } else {
                    context.getString(R.string.webview_contextmenu_image_download_action)
                },
            ).setOnMenuItemClickListener(listener)
        }

        if (!inlineImage) {
            menu.add(
                Menu.NONE,
                MENU_ITEM_IMAGE_COPY,
                2,
                context.getString(R.string.webview_contextmenu_image_copy_action),
            ).setOnMenuItemClickListener(listener)
        }
    }

    private fun createPhoneNumberMenu(menu: ContextMenu, phoneNumber: String?) {
        if (phoneNumber == null) return

        val listener = MenuItem.OnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_ITEM_PHONE_CALL -> {
                    val uri = Uri.parse(WebView.SCHEME_TEL + phoneNumber)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivityIfAvailable(context, intent)
                }
                MENU_ITEM_PHONE_SAVE -> {
                    val intent = ContactIntentHelper.getAddPhoneContactIntent(phoneNumber)
                    startActivityIfAvailable(context, intent)
                }
                MENU_ITEM_PHONE_COPY -> {
                    val label = context.getString(R.string.webview_contextmenu_phone_clipboard_label)
                    clipboardManager.setText(label, phoneNumber)
                }
            }
            true
        }

        menu.setHeaderTitle(phoneNumber)

        menu.add(
            Menu.NONE,
            MENU_ITEM_PHONE_CALL,
            0,
            context.getString(R.string.webview_contextmenu_phone_call_action),
        ).setOnMenuItemClickListener(listener)

        menu.add(
            Menu.NONE,
            MENU_ITEM_PHONE_SAVE,
            1,
            context.getString(R.string.webview_contextmenu_phone_save_action),
        ).setOnMenuItemClickListener(listener)

        menu.add(
            Menu.NONE,
            MENU_ITEM_PHONE_COPY,
            2,
            context.getString(R.string.webview_contextmenu_phone_copy_action),
        ).setOnMenuItemClickListener(listener)
    }

    private fun createEmailMenu(menu: ContextMenu, email: String?) {
        if (email == null) return

        val listener = MenuItem.OnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_ITEM_EMAIL_SEND -> {
                    val uri = Uri.parse(WebView.SCHEME_MAILTO + email)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivityIfAvailable(context, intent)
                }
                MENU_ITEM_EMAIL_SAVE -> {
                    val intent = ContactIntentHelper.getAddEmailContactIntent(Address(email))
                    startActivityIfAvailable(context, intent)
                }
                MENU_ITEM_EMAIL_COPY -> {
                    val label = context.getString(R.string.webview_contextmenu_email_clipboard_label)
                    clipboardManager.setText(label, email)
                }
            }
            true
        }

        menu.setHeaderTitle(email)

        menu.add(
            Menu.NONE,
            MENU_ITEM_EMAIL_SEND,
            0,
            context.getString(R.string.webview_contextmenu_email_send_action),
        ).setOnMenuItemClickListener(listener)

        menu.add(
            Menu.NONE,
            MENU_ITEM_EMAIL_SAVE,
            1,
            context.getString(R.string.webview_contextmenu_email_save_action),
        ).setOnMenuItemClickListener(listener)

        menu.add(
            Menu.NONE,
            MENU_ITEM_EMAIL_COPY,
            2,
            context.getString(R.string.webview_contextmenu_email_copy_action),
        ).setOnMenuItemClickListener(listener)
    }

    private fun downloadImage(uri: Uri) {
        val request = DownloadManager.Request(uri).apply {
            if (Build.VERSION.SDK_INT >= 29) {
                val filename = uri.lastPathSegment
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            }

            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }

    private fun getAttachmentViewInfoIfCidUri(uri: Uri): AttachmentViewInfo? {
        if (uri.scheme != "cid") return null

        val attachmentResolver = checkNotNull(currentAttachmentResolver)

        val cid = uri.schemeSpecificPart
        val internalUri = attachmentResolver.getAttachmentUriForContentId(cid)

        return attachments[internalUri]
    }

    private fun startActivityIfAvailable(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.error_activity_not_found, Toast.LENGTH_LONG).show()
        }
    }

    private fun setLoadPictures(enable: Boolean) {
        messageContentView.blockNetworkData(!enable)
        isShowingPictures = enable
    }

    fun showPictures() {
        setLoadPictures(true)
        refreshDisplayedContent()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun displayMessageViewContainer(
        messageViewInfo: MessageViewInfo,
        onRenderingFinishedListener: OnRenderingFinishedListener,
        loadPictures: Boolean,
        hideUnsignedTextDivider: Boolean,
        attachmentCallback: AttachmentViewCallback?,
    ) {
        this.attachmentCallback = attachmentCallback

        println("display again")
        resetView()
        renderAttachments(messageViewInfo)


        this.msginHtml = messageViewInfo.text

        val messageText = messageViewInfo.text
        if (messageText != null && !isShowingPictures) {
            if (Utility.hasExternalImages(messageText)) {
                if (loadPictures) {
                    setLoadPictures(true)
                } else {
                    hasHiddenExternalImages = true
                }
            }
        }

        val textToDisplay = messageText
            ?: displayHtml.wrapStatusMessage(context.getString(R.string.webview_empty_message))

        displayHtmlContentWithInlineAttachments(
            htmlText = textToDisplay,
            attachmentResolver = messageViewInfo.attachmentResolver,
            onPageFinishedListener = onRenderingFinishedListener::onLoadFinished,
        )

        if (!messageViewInfo.extraText.isNullOrEmpty()) {
            unsignedTextContainer.isVisible = true
            unsignedTextDivider.isGone = hideUnsignedTextDivider
            unsignedText.text = messageViewInfo.extraText
        }
    }

    private fun displayHtmlContentWithInlineAttachments(
        htmlText: String,
        attachmentResolver: AttachmentResolver,
        onPageFinishedListener: OnPageFinishedListener,
    ) {
        currentHtmlText = htmlText
        currentAttachmentResolver = attachmentResolver
        messageContentView.displayHtmlContentWithInlineAttachments(htmlText, attachmentResolver, onPageFinishedListener)
    }

    private fun refreshDisplayedContent() {
        val htmlText = checkNotNull(currentHtmlText)

        messageContentView.displayHtmlContentWithInlineAttachments(
            htmlText = htmlText,
            attachmentResolver = currentAttachmentResolver,
            onPageFinishedListener = null,
        )
    }

    private fun clearDisplayedContent() {
        messageContentView.displayHtmlContentWithInlineAttachments(
            htmlText = "",
            attachmentResolver = null,
            onPageFinishedListener = null,
        )

        unsignedTextContainer.isVisible = false
        unsignedText.text = ""
    }

    private fun renderAttachments(messageViewInfo: MessageViewInfo) {
        if (messageViewInfo.attachments != null) {
            for (attachment in messageViewInfo.attachments) {
                attachments[attachment.internalUri] = attachment
                if (attachment.inlineAttachment) {
                    continue
                }

                val attachmentView = layoutInflater.inflate(
                    R.layout.message_view_attachment,
                    attachmentsContainer,
                    false,
                ) as AttachmentView

                attachmentView.setCallback(attachmentCallback)
                attachmentView.setAttachment(attachment)

                attachmentViewMap[attachment] = attachmentView
                attachmentsContainer.addView(attachmentView)
            }
        }

        if (messageViewInfo.extraAttachments != null) {
            for (attachment in messageViewInfo.extraAttachments) {
                attachments[attachment.internalUri] = attachment
                if (attachment.inlineAttachment) {
                    continue
                }

                val lockedAttachmentView = layoutInflater.inflate(
                    R.layout.message_view_attachment_locked,
                    attachmentsContainer,
                    false,
                ) as LockedAttachmentView

                lockedAttachmentView.setCallback(attachmentCallback)
                lockedAttachmentView.setAttachment(attachment)

                attachmentsContainer.addView(lockedAttachmentView)
            }
        }
    }

    private fun resetView() {
        setLoadPictures(false)
        attachmentsContainer.removeAllViews()

        currentHtmlText = null
        currentAttachmentResolver = null

        /*
         * Clear the WebView content
         *
         * For some reason WebView.clearView() doesn't clear the contents when the WebView changes
         * its size because the button to download the complete message was previously shown and
         * is now hidden.
         */
        clearDisplayedContent()
    }

    fun refreshAttachmentThumbnail(attachment: AttachmentViewInfo) {
        getAttachmentView(attachment)?.refreshThumbnail()
    }

    private fun getAttachmentView(attachment: AttachmentViewInfo): AttachmentView? {
        return attachmentViewMap[attachment]
    }

    interface OnRenderingFinishedListener {
        fun onLoadFinished()
    }

    companion object {
        private const val MENU_ITEM_LINK_VIEW = Menu.FIRST
        private const val MENU_ITEM_LINK_SHARE = Menu.FIRST + 1
        private const val MENU_ITEM_LINK_COPY = Menu.FIRST + 2
        private const val MENU_ITEM_LINK_TEXT_COPY = Menu.FIRST + 3
        private const val MENU_ITEM_IMAGE_VIEW = Menu.FIRST
        private const val MENU_ITEM_IMAGE_SAVE = Menu.FIRST + 1
        private const val MENU_ITEM_IMAGE_COPY = Menu.FIRST + 2
        private const val MENU_ITEM_PHONE_CALL = Menu.FIRST
        private const val MENU_ITEM_PHONE_SAVE = Menu.FIRST + 1
        private const val MENU_ITEM_PHONE_COPY = Menu.FIRST + 2
        private const val MENU_ITEM_EMAIL_SEND = Menu.FIRST
        private const val MENU_ITEM_EMAIL_SAVE = Menu.FIRST + 1
        private const val MENU_ITEM_EMAIL_COPY = Menu.FIRST + 2

        // DownloadManager only supports http and https URIs
        private val supportedDownloadUriSchemes = setOf("http", "https")
    }
}
