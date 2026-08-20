package com.qubeguard.app

import com.qubeguard.app.ml.BertWordPieceTokenizer
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BertWordPieceTokenizerTest {
    @Test
    fun `encodes fixed length BERT inputs`() {
        val vocab = File.createTempFile("qubeguard-vocab", ".txt")
        vocab.writeText(
            "[PAD]\n[UNK]\n[CLS]\n[SEP]\nhttps\n:\n/\nexample\n.\ncom\n"
        )

        try {
            val tokenizer = BertWordPieceTokenizer(vocab)
            val encoded = tokenizer.encode("https://example.com", 16)

            assertEquals(16, encoded.inputIds.size)
            assertEquals(16, encoded.attentionMask.size)
            assertEquals(16, encoded.tokenTypeIds.size)
            assertEquals(1L, encoded.attentionMask[0])
            assertTrue(encoded.attentionMask.sum() >= 2L)
        } finally {
            vocab.delete()
        }
    }
}
