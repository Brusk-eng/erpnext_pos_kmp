package com.erpnext.pos.printing.formatting

import com.erpnext.pos.domain.printing.model.PrintAlignment
import com.erpnext.pos.domain.printing.model.ReceiptDocument
import com.erpnext.pos.domain.printing.model.ReceiptLine

class ReceiptFormatter {
  fun format(document: ReceiptDocument, lineWidth: Int): List<String> {
    val result = mutableListOf<String>()

    if (document.header.lines.isNotEmpty()) {
      result += document.header.lines.map { align(it, lineWidth, document.header.alignment) }
      result += ""
    }

    document.bodyLines.forEach { line ->
      result += wrapLine(line, lineWidth)
    }

    result += "-".repeat(lineWidth)
    document.totals.subTotal?.let {
      result += formatLine(ReceiptLine(document.totalsLabels.subtotal, it), lineWidth)
    }
    document.totals.tax?.let { result += formatLine(ReceiptLine(document.totalsLabels.tax, it), lineWidth) }
    result +=
        formatLine(
            ReceiptLine(document.totalsLabels.total, document.totals.total, emphasis = true),
            lineWidth,
        )

    if (document.footer.lines.isNotEmpty()) {
      result += ""
      result += document.footer.lines.map { align(it, lineWidth, document.footer.alignment) }
    }

    return result
  }

  private fun wrapLine(line: ReceiptLine, width: Int): List<String> {
    if (line.right.isBlank()) return wrapSingleColumn(line.left, width)

    val gap = 2
    val trimmedRight = line.right.trim()
    val maxRightColumnWidth = (width * 0.45).toInt().coerceAtLeast(10)
    val rightColumnWidth = trimmedRight.length.coerceIn(8, maxRightColumnWidth)
    val leftColumnWidth = (width - rightColumnWidth - gap).coerceAtLeast(8)
    val rightChunks = wrapSingleColumn(trimmedRight, rightColumnWidth)
    val leftChunks = wrapSingleColumn(line.left, leftColumnWidth)
    if (leftChunks.isEmpty() && rightChunks.isEmpty()) return listOf("")
    if (leftChunks.isEmpty()) {
      return rightChunks.map { chunk -> " ".repeat(width - chunk.length) + chunk }
    }

    val lineCount = maxOf(leftChunks.size, rightChunks.size)
    return (0 until lineCount).map { index ->
      val leftPart = leftChunks.getOrElse(index) { "" }.take(leftColumnWidth)
      val rightPart = rightChunks.getOrElse(index) { "" }.take(rightColumnWidth)
      if (rightPart.isBlank()) {
        leftPart
      } else {
        val leftPadded = leftPart.padEnd(leftColumnWidth)
        val rightPadded = rightPart.padStart(rightColumnWidth)
        leftPadded + " ".repeat(gap) + rightPadded
      }
    }
  }

  private fun wrapSingleColumn(text: String, width: Int): List<String> {
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return listOf("")

    val lines = mutableListOf<String>()
    var current = ""
    words.forEach { word ->
      val candidate = if (current.isBlank()) word else "$current $word"
      if (candidate.length <= width) {
        current = candidate
      } else {
        if (current.isNotBlank()) {
          lines += current
        }
        current =
            if (word.length <= width) {
              word
            } else {
              word.chunked(width).also { chunks -> lines += chunks.dropLast(1) }.last()
            }
      }
    }
    if (current.isNotBlank()) lines += current
    return lines
  }

  private fun formatLine(line: ReceiptLine, width: Int): String {
    val left = line.left.trim()
    val right = line.right.trim()
    if (right.isBlank()) return left.take(width)

    val spaces = (width - left.length - right.length).coerceAtLeast(1)
    return (left + " ".repeat(spaces) + right).take(width)
  }

  private fun align(text: String, width: Int, alignment: PrintAlignment): String {
    val clean = text.trim().take(width)
    return when (alignment) {
      PrintAlignment.START -> clean.padEnd(width)
      PrintAlignment.CENTER -> {
        val leftPad = ((width - clean.length) / 2).coerceAtLeast(0)
        (" ".repeat(leftPad) + clean).padEnd(width)
      }
      PrintAlignment.END -> clean.padStart(width)
    }
  }
}
