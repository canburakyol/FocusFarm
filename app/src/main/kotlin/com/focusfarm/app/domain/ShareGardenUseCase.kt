package com.focusfarm.app.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class ShareGardenUseCase {

    fun generateGardenBitmap(
        context: Context,
        sessions: List<FocusSession>,
        totalMinutes: Int,
        streak: Int,
    ): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(android.graphics.Color.parseColor("#0F1712"))

        val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("FocusFarm", width / 2f, 120f, paint.apply {
            textSize = 72f
            color = android.graphics.Color.parseColor("#E4B156")
            typeface = Typeface.DEFAULT_BOLD
        })

        canvas.drawText("Bahcem", width / 2f, 200f, paint.apply {
            textSize = 48f
            color = android.graphics.Color.parseColor("#F6EFDD")
        })

        canvas.drawText("$streak Gun Seri - ${totalMinutes} Dakika Odak", width / 2f, 270f, paint.apply {
            textSize = 36f
            color = android.graphics.Color.parseColor("#D8C9AF")
        })

        val plantSize = 140f
        val spacing = 20f
        val cols = 5
        val startX = (width - (cols * (plantSize + spacing))) / 2f
        var y = 340f

        sessions.take(30).forEachIndexed { index, session ->
            val col = index % cols
            val row = index / cols
            val x = startX + col * (plantSize + spacing)
            val currentY = y + row * (plantSize + spacing + 60f)

            val bgPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#1C2A22")
            }
            val rect = RectF(x, currentY, x + plantSize, currentY + plantSize)
            canvas.drawRoundRect(rect, 30f, 30f, bgPaint)

            canvas.drawText(session.plantEmoji, x + plantSize / 2f, currentY + plantSize / 2f + 20f, paint.apply {
                textSize = 60f
                textAlign = Paint.Align.CENTER
            })

            canvas.drawText(session.plantName, x + plantSize / 2f, currentY + plantSize + 30f, paint.apply {
                textSize = 22f
                color = android.graphics.Color.parseColor("#D8C9AF")
                textAlign = Paint.Align.CENTER
            })
        }

        val footerY = height - 120f
        canvas.drawText("FocusFarm ile odaklan, bahceni buyut!", width / 2f, footerY, paint.apply {
            textSize = 32f
            color = android.graphics.Color.parseColor("#E4B156")
        })

        canvas.drawText("play.google.com/store/apps/details?id=com.focusfarm.app", width / 2f, footerY + 50f, paint.apply {
            textSize = 24f
            color = android.graphics.Color.parseColor("#71C37C")
        })

        return bitmap
    }

    fun shareImage(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val cachePath = File(context.cacheDir, "shares")
            cachePath.mkdirs()
            val file = File(cachePath, "garden_share_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
            } else {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_TEXT, "FocusFarm ile bahcemi buyutuyorum! Sen de odaklanarak kendi bahceni yetistir.")
            }

            context.startActivity(Intent.createChooser(shareIntent, "Bahceni Paylas"))
            true
        } catch (e: Exception) {
            false
        }
    }
}
