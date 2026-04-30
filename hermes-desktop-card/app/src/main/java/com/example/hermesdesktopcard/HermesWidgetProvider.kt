package com.example.hermesdesktopcard

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.app.PendingIntent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class HermesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it, "点击按钮即可发送常用指令") }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_SEND_PROMPT -> {
                val prompt = intent.getStringExtra(EXTRA_PROMPT).orEmpty()
                sendPrompt(context.applicationContext, prompt)
            }
            ACTION_REFRESH -> refreshAll(context.applicationContext, "已刷新")
        }
    }

    private fun sendPrompt(context: Context, prompt: String) {
        refreshAll(context, "正在发送：$prompt")
        CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching { HermesApi(context).send(prompt) }
                .getOrElse { "请求失败：${it.message ?: it.javaClass.simpleName}" }
            withContext(Dispatchers.Main) {
                refreshAll(context, result.take(120))
                Toast.makeText(context, result.take(80), Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val ACTION_SEND_PROMPT = "com.example.hermesdesktopcard.ACTION_SEND_PROMPT"
        const val ACTION_REFRESH = "com.example.hermesdesktopcard.ACTION_REFRESH"
        const val EXTRA_PROMPT = "prompt"

        fun refreshAll(context: Context, status: String) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, HermesWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { updateWidget(context, manager, it, status) }
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int, status: String) {
            val views = RemoteViews(context.packageName, R.layout.hermes_widget)
            views.setTextViewText(R.id.status, status)
            views.setOnClickPendingIntent(R.id.btn_status, sendIntent(context, appWidgetId, "请告诉我当前 Hermes 状态和可用工具。"))
            views.setOnClickPendingIntent(R.id.btn_summary, sendIntent(context, appWidgetId, "请总结我最近的会话和待办事项。"))
            views.setOnClickPendingIntent(R.id.btn_settings, settingsIntent(context, appWidgetId))
            manager.updateAppWidget(appWidgetId, views)
        }

        private fun sendIntent(context: Context, id: Int, prompt: String): PendingIntent {
            val intent = Intent(context, HermesWidgetProvider::class.java).apply {
                action = ACTION_SEND_PROMPT
                putExtra(EXTRA_PROMPT, prompt)
            }
            return PendingIntent.getBroadcast(
                context,
                id + prompt.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun settingsIntent(context: Context, id: Int): PendingIntent {
            val intent = Intent(context, SettingsActivity::class.java)
            return PendingIntent.getActivity(
                context,
                id + 9999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

class HermesApi(private val context: Context) {
    private val prefs = context.getSharedPreferences("hermes_card", Context.MODE_PRIVATE)

    fun send(prompt: String): String {
        val endpoint = prefs.getString("endpoint", "http://127.0.0.1:8000/chat") ?: "http://127.0.0.1:8000/chat"
        val token = prefs.getString("token", "").orEmpty()
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
        }

        val payload = "{\"message\":${prompt.jsonQuote()}}"
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(payload) }
        val body = if (connection.responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP ${connection.responseCode}"
        }
        connection.disconnect()
        return body.ifBlank { "已发送，但服务器返回为空" }
    }
}

private fun String.jsonQuote(): String = buildString {
    append('"')
    this@jsonQuote.forEach { c ->
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
    append('"')
}
