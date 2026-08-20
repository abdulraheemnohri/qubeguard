package com.qubeguard.app.ml

import java.io.File
import java.util.Locale

/**
 * Small dependency-free BERT tokenizer for the URL classifier.
 * It implements the BasicTokenizer + WordPiece stages required by bert-base-uncased.
 */
class BertWordPieceTokenizer(vocabFile: File) {
    private val vocab: Map<String, Int> = vocabFile.readLines()
        .mapIndexed { index, token -> token to index }
        .toMap()

    private val unkId = vocab["[UNK]"] ?: 100
    private val clsId = vocab["[CLS]"] ?: 101
    private val sepId = vocab["[SEP]"] ?: 102
    private val padId = vocab["[PAD]"] ?: 0

    data class Encoded(
        val inputIds: LongArray,
        val attentionMask: LongArray,
        val tokenTypeIds: LongArray
    )

    fun encode(text: String, maxLength: Int = 128): Encoded {
        val tokens = mutableListOf<Int>()
        tokens += clsId

        for (piece in basicTokenize(text)) {
            tokens += wordPieceTokenize(piece)
            if (tokens.size >= maxLength - 1) break
        }

        tokens += sepId
        val padded = LongArray(maxLength) { padId.toLong() }
        val mask = LongArray(maxLength)
        val types = LongArray(maxLength)

        tokens.take(maxLength).forEachIndexed { index, id ->
            padded[index] = id.toLong()
            mask[index] = 1L
        }

        return Encoded(padded, mask, types)
    }

    private fun basicTokenize(input: String): List<String> {
        val normalized = input.lowercase(Locale.US)
            .replace('\u0000'.toString(), "")
            .replace('\ufffd'.toString(), "")

        val spaced = buildString {
            for (ch in normalized) {
                when {
                    ch.isWhitespace() -> append(' ')
                    isControl(ch) -> Unit
                    isPunctuation(ch) -> {
                        append(' ')
                        append(ch)
                        append(' ')
                    }
                    else -> append(ch)
                }
            }
        }
        return spaced.split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
    }

    private fun wordPieceTokenize(token: String): List<Int> {
        if (token.length > 100) return listOf(unkId)
        if (vocab.containsKey(token)) return listOf(vocab.getValue(token))

        val output = mutableListOf<Int>()
        var start = 0
        while (start < token.length) {
            var end = token.length
            var found: String? = null
            while (start < end) {
                var candidate = token.substring(start, end)
                if (start > 0) candidate = "##$candidate"
                if (vocab.containsKey(candidate)) {
                    found = candidate
                    break
                }
                end--
            }
            if (found == null) return listOf(unkId)
            output += vocab.getValue(found)
            start = end
        }
        return output
    }

    private fun isControl(ch: Char): Boolean =
        ch.code in 0..31 || ch.code == 127

    private fun isPunctuation(ch: Char): Boolean =
        ch in "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
}
