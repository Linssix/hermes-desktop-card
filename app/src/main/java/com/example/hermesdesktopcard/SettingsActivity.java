package com.example.hermesdesktopcard;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        EditText endpoint = findViewById(R.id.endpoint);
        EditText token = findViewById(R.id.token);
        EditText customPrompt = findViewById(R.id.customPrompt);

        endpoint.setText(getSharedPreferences("hermes_card", MODE_PRIVATE)
                .getString("endpoint", "http://127.0.0.1:8000/chat"));
        token.setText(getSharedPreferences("hermes_card", MODE_PRIVATE)
                .getString("token", ""));

        Button save = findViewById(R.id.save);
        save.setOnClickListener(v -> {
            getSharedPreferences("hermes_card", MODE_PRIVATE).edit()
                    .putString("endpoint", endpoint.getText().toString().trim())
                    .putString("token", token.getText().toString())
                    .apply();
            HermesWidgetProvider.refreshAll(this, "设置已保存");
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        });

        Button sendCustom = findViewById(R.id.sendCustom);
        sendCustom.setOnClickListener(v -> {
            String prompt = customPrompt.getText().toString().trim();
            if (prompt.isEmpty()) {
                Toast.makeText(this, "请输入指令", Toast.LENGTH_SHORT).show();
                return;
            }
            getSharedPreferences("hermes_card", MODE_PRIVATE).edit()
                    .putString("endpoint", endpoint.getText().toString().trim())
                    .putString("token", token.getText().toString())
                    .apply();
            HermesWidgetProvider.refreshAll(this, "正在发送：" + prompt);
            new Thread(() -> {
                String result;
                try {
                    result = new HermesApi(getApplicationContext()).send(prompt);
                } catch (Exception e) {
                    result = "请求失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                }
                String display = result.length() > 120 ? result.substring(0, 120) : result;
                new Handler(Looper.getMainLooper()).post(() -> {
                    HermesWidgetProvider.refreshAll(this, display);
                    Toast.makeText(this, display, Toast.LENGTH_LONG).show();
                });
            }).start();
        });
    }
}
