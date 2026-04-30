package com.example.hermesdesktopcard

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("hermes_card", MODE_PRIVATE)
        val endpoint = findViewById<EditText>(R.id.endpoint)
        val token = findViewById<EditText>(R.id.token)
        val customPrompt = findViewById<EditText>(R.id.customPrompt)

        endpoint.setText(prefs.getString("endpoint", "http://127.0.0.1:8000/chat"))
        token.setText(prefs.getString("token", ""))

        findViewById<Button>(R.id.save).setOnClickListener {
            prefs.edit()
                .putString("endpoint", endpoint.text.toString().trim())
                .putString("token", token.text.toString())
                .apply()
            HermesWidgetProvider.refreshAll(this, "设置已保存")
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.sendCustom).setOnClickListener {
            val prompt = customPrompt.text.toString().trim()
            if (prompt.isBlank()) {
                Toast.makeText(this, "请输入指令", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit()
                .putString("endpoint", endpoint.text.toString().trim())
                .putString("token", token.text.toString())
                .apply()
            HermesWidgetProvider.refreshAll(this, "正在发送：$prompt")
            CoroutineScope(Dispatchers.IO).launch {
                val result = runCatching { HermesApi(applicationContext).send(prompt) }
                    .getOrElse { "请求失败：${it.message ?: it.javaClass.simpleName}" }
                withContext(Dispatchers.Main) {
                    HermesWidgetProvider.refreshAll(this@SettingsActivity, result.take(120))
                    Toast.makeText(this@SettingsActivity, result.take(120), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
