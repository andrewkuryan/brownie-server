@file:DependsOn("org.bouncycastle:bcprov-jdk15on:1.69")

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.nio.file.Files
import java.nio.file.Paths
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.util.*

Security.addProvider(BouncyCastleProvider())

val keyGen = KeyPairGenerator.getInstance("ECDSA", "BC")
keyGen.initialize(ECGenParameterSpec("P-521"), SecureRandom())
val pair = keyGen.generateKeyPair()

val exportedPub = String(Base64.getEncoder().encode(pair.public.encoded))
val exportedPriv = String(Base64.getEncoder().encode(pair.private.encoded))

Files.write(
        Paths.get("src/main/resources/generatedProperties.conf"),
        listOf("""
            {
                security {
                    ecdsa {
                        privateKey = "$exportedPriv"
                    }
                }
            }
        """.trimIndent())
)

Files.write(
        Paths.get("web/.generated.env"),
        listOf("""
            ECDSA_SERVER_PUBLIC_KEY=$exportedPub
        """.trimIndent())
)