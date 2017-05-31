package tech.kotlin.service.mail

import tech.kotlin.model.request.EmailReq
import tech.kotlin.model.response.EmptyResp
import tech.kotlin.utils.os.Handler
import tech.kotlin.utils.os.Looper
import tech.kotlin.utils.properties.Props
import tech.kotlin.utils.properties.str
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/*********************************************************************
 * Created by chpengzh@foxmail.com
 * Copyright (c) http://chpengzh.com - All Rights Reserved
 *********************************************************************/
object Emails {

    val handler = Handler(Looper.getMainLooper())
    val properties = Props.loads("project.properties")
    val authenticator: Authenticator by lazy {
        // 构建授权信息，用于进行SMTP进行身份验证
        object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(properties str "mail.user", properties str "mail.password")
            }
        }
    }

    fun send(req: EmailReq): EmptyResp {
        val task = Runnable {
            Transport.send(MimeMessage(Session.getInstance(properties, authenticator)).apply {
                setFrom(InternetAddress(properties str "mail.user"))
                setRecipient(MimeMessage.RecipientType.TO, InternetAddress(req.to))
                subject = req.subject
                setContent(req.content, "text/html;charset=UTF-8")
            })
        }
        if (req.async) {
            handler.post(task)
        } else {
            task.run()
        }
        return EmptyResp()
    }

}

fun main(args: Array<String>) {
    Emails.send(EmailReq().apply {
        this.to = "chpengzh@kotlin-cn.org"
        this.subject = "test mail"
        this.content = ""
        this.async = false
    })
}

