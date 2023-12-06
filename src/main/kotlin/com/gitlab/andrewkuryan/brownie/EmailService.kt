package com.gitlab.andrewkuryan.brownie

import com.gitlab.andrewkuryan.brownie.entity.user.ContactData
import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import java.io.*
import java.nio.file.Path

class EmailService(
    smtpServer: String,
    smtpServerPort: Int,
    smtpServerUsername: String,
    smtpServerPassword: String,
    private val senderName: String,
    private val senderEmail: String,
    templatesRoot: Path
) {

    private val cfg = Configuration(Configuration.VERSION_2_3_31)
    private val mailer = MailerBuilder
        .withSMTPServer(smtpServer, smtpServerPort, smtpServerUsername, smtpServerPassword)
        .withTransportStrategy(TransportStrategy.SMTPS)
        .async()
        .buildMailer()

    init {
        println("smtpServer: $smtpServer")
        println("smtpServerUsername: $smtpServerUsername")
        println("smtpServerPassword: $smtpServerPassword")

        cfg.setDirectoryForTemplateLoading(templatesRoot.toFile())
        cfg.defaultEncoding = "UTF-8"
        cfg.templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        cfg.logTemplateExceptions = false
        cfg.wrapUncheckedExceptions = true
        cfg.fallbackOnNullLoopVariable = false
    }

    fun sendVerificationEmail(contactData: ContactData.Email, verificationCode: String) {
        val root = mutableMapOf(
            "verificationCode" to verificationCode
        )

        val temp = cfg.getTemplate("verification.ftl")
        val out = ByteArrayOutputStream()
        temp.process(root, OutputStreamWriter(out))

        sendEmail(contactData.emailAddress, "Verify your email address", out.toString(Charsets.UTF_8))
    }

    private fun sendEmail(to: String, subject: String, body: String) {
        val email = EmailBuilder.startingBlank()
            .to(to)
            .from(senderName, senderEmail)
            .withSubject(subject)
            .withHTMLText(body)
            .buildEmail()

        mailer.sendMail(email)
    }
}