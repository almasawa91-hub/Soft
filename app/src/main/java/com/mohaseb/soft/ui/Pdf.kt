package com.mohaseb.soft.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

fun PdfShare(ctx: Context, title: String, headers: List<String>, rows: List<List<String>>) {
    val doc = PdfDocument()
    val p = Paint().apply { color = android.graphics.Color.BLACK; textSize = 11f; isAntiAlias = true }
    val perPage = 36
    val pages = maxOf(1, (rows.size + perPage - 1) / perPage)
    for (pg in 0 until pages) {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pg + 1).create())
        val c = page.canvas
        p.textSize = 16f
        c.drawText(title, 20f, 40f, p)
        p.textSize = 11f
        c.drawText(headers.joinToString("  |  "), 20f, 70f, p)
        var y = 95f
        rows.drop(pg * perPage).take(perPage).forEach { r ->
            c.drawText(r.joinToString("  |  "), 20f, y, p)
            y += 20f
        }
        doc.finishPage(page)
    }
    val f = File(ctx.cacheDir, "report.pdf")
    FileOutputStream(f).use { doc.writeTo(it) }
    doc.close()
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".provider", f)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(intent, "مشاركة PDF"))
}
