package com.focusfarm.app.data.billing

import com.google.common.truth.Truth.assertThat
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import org.junit.Test

class PurchaseSignatureVerifierTest {
    @Test
    fun validSignatureIsAcceptedButModifiedPurchaseIsRejected() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val purchase = """{"productId":"premium_lifetime","purchaseState":0}"""
        val signature = Signature.getInstance("SHA1withRSA").run {
            initSign(keyPair.private)
            update(purchase.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(sign())
        }
        val publicKey = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        assertThat(PurchaseSignatureVerifier.verify(publicKey, purchase, signature)).isTrue()
        assertThat(PurchaseSignatureVerifier.verify(publicKey, purchase.replace("premium_lifetime", "premium_yearly"), signature)).isFalse()
        assertThat(PurchaseSignatureVerifier.verify("", purchase, signature)).isFalse()
        assertThat(PurchaseSignatureVerifier.verify("not-a-key", purchase, signature)).isFalse()
        assertThat(PurchaseSignatureVerifier.verify(publicKey, purchase, "not-a-signature")).isFalse()
    }
}
