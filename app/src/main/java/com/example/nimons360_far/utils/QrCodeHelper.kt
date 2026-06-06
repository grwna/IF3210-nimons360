package com.example.nimons360_far.utils

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.core.net.toUri
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

// Generate QR Code Bitmap
fun generateQrBitmap(content: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val bitmap = createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bitmap[x, y] = if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}

// Share as QR Code
@SuppressLint("QueryPermissionsNeeded")
fun shareQrCodeImage(context: Context, deepLink: String, familyName: String) {
    val familyCode = deepLink.toUri().getQueryParameter("code") ?: "UNKNOWN"
    val qrBitmap = generateQrBitmap(deepLink) ?: return
    val finalBitmap = createBitmap(512, 512 + 250, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(finalBitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    canvas.drawBitmap(qrBitmap, 0f, 100f, null)
    val textPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    textPaint.textSize = 32f
    textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
    canvas.drawText("Nimons360", 256f, 60f, textPaint)
    textPaint.textSize = 36f
    canvas.drawText("Join: $familyName", 256f, 512f + 140f, textPaint)
    textPaint.textSize = 28f
    textPaint.typeface = android.graphics.Typeface.DEFAULT
    textPaint.color = android.graphics.Color.DKGRAY
    canvas.drawText("Code: $familyCode", 256f, 512f + 190f, textPaint)
    try {
        val cachePath = File(context.cacheDir, "shared_files")
        cachePath.mkdirs()
        val imageFile = File(cachePath, "family_qr.png")
        finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(imageFile))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Nimons360 Invitation")
            putExtra(Intent.EXTRA_TEXT, "Join our family on Nimons360! Scan this QR Code and use the code: $familyCode")
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share QR Code")
        val resList = context.packageManager.queryIntentActivities(chooser, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resList) {
            context.grantUriPermission(resolveInfo.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(context, "Failed to Share QR Code", Toast.LENGTH_SHORT).show()
    }
}

// Share as Link (HTML)
@SuppressLint("QueryPermissionsNeeded")
fun shareAsHtml(context: Context, deepLink: String, familyName: String) {
    val familyCode = deepLink.toUri().getQueryParameter("code") ?: "XXXXXX"
    try {
        val cachePath = File(context.cacheDir, "shared_files")
        cachePath.mkdirs()
        val htmlFile = File(cachePath, "Nimons360_Invitation_$familyName.html")
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body { font-family: sans-serif; background-color: #F0F4F8; color: #333333; text-align: center; padding: 40px 20px; }
                    .card { border-radius: 16px; box-shadow: 0 8px 16px rgba(0,0,0,0.1); padding: 40px 20px; max-width: 400px; margin: auto; background-color: #FFFFFF; border: 1px solid #E0E0E0; }
                    .code { font-size: 28px; font-weight: bold; color: #2196F3; letter-spacing: 4px; margin: 16px 0; }
                    .btn { display: inline-block; background-color: #2196F3; color: #FFFFFF; text-decoration: none; padding: 16px 32px; border-radius: 28px; font-weight: bold; margin-top: 24px; font-size: 16px; }
                    h2 { color: #1565C0; margin-bottom: 8px; }
                    p { color: #555555; line-height: 1.5; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h2>Nimons360</h2>
                    <p>You have been invited to join the family<br><b>$familyName</b></p>
                    <div class="code">$familyCode</div>
                    <p>Please ensure Nimons360 is installed, then click the button below:</p>
                    <a class="btn" href="$deepLink">Open App & Join</a>
                </div>
            </body>
            </html>
        """.trimIndent()
        htmlFile.writeText(htmlContent)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", htmlFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Link via HTML")
        val resList = context.packageManager.queryIntentActivities(chooser, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resList) {
            context.grantUriPermission(resolveInfo.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(context, "Failed to Share HTML Link", Toast.LENGTH_SHORT).show()
    }
}