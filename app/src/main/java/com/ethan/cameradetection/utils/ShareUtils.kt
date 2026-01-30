package com.ethan.cameradetection.utils

import android.content.Context
import android.content.Intent

object ShareUtils {

    fun shareTextWithHighlightedLinks(context: Context, text: String,myUrl: String) {
        // 检测文本中的所有链接并高亮
        val highlightedHtml = highlightUrlsInText(text,myUrl)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)  // 纯文本
            putExtra(Intent.EXTRA_HTML_TEXT, highlightedHtml)  // HTML 高亮版
            putExtra(Intent.EXTRA_SUBJECT, "分享")
        }

        try {
            context.startActivity(Intent.createChooser(shareIntent, "分享到"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "分享失败", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun highlightUrlsInText(text: String, myUrl: String): String {
        // 构建 HTML
        val htmlBuilder = StringBuilder("<html><body style='font-family:sans-serif;'>")

        // 如果myUrl存在于text中
        if (text.contains(myUrl)) {
            // 找到myUrl的位置
            val startIndex = text.indexOf(myUrl)
            val endIndex = startIndex + myUrl.length

            // 添加myUrl之前的文本
            if (startIndex > 0) {
                htmlBuilder.append(text.substring(0, startIndex)
                    .replace("\n", "<br/>")
                    .replace("  ", " &nbsp;"))
            }

            // 高亮显示myUrl
            val fullUrl = if (myUrl.startsWith("www")) "https://$myUrl" else myUrl
            htmlBuilder.append("""<a href="$fullUrl" style="color:#FF5722;text-decoration:underline;font-weight:bold;">""")
            htmlBuilder.append(myUrl)
            htmlBuilder.append("</a>")

            // 添加myUrl之后的文本
            if (endIndex < text.length) {
                htmlBuilder.append(text.substring(endIndex)
                    .replace("\n", "<br/>")
                    .replace("  ", " &nbsp;"))
            }
        } else {
            // 如果myUrl不在text中，直接显示原始文本
            htmlBuilder.append(text
                .replace("\n", "<br/>")
                .replace("  ", " &nbsp;"))
        }

        htmlBuilder.append("</body></html>")
        return htmlBuilder.toString()
    }
}