package com.example.hermesdesktopcard;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HermesWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_SEND_PROMPT = "com.example.hermesdesktopcard.ACTION_SEND_PROMPT";
    public static final String ACTION_REFRESH = "com.example.hermesdesktopcard.ACTION_REFRESH";
    public static final String EXTRA_PROMPT = "prompt";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            updateWidget(context, manager, id, "点击按钮即可发送常用指令");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent == null || intent.getAction() == null) return;
        if (ACTION_SEND_PROMPT.equals(intent.getAction())) {
            String prompt = intent.getStringExtra(EXTRA_PROMPT);
            if (prompt == null) prompt = "";
            sendPrompt(context.getApplicationContext(), prompt);
        } else if (ACTION_REFRESH.equals(intent.getAction())) {
            refreshAll(context.getApplicationContext(), "已刷新");
        }
    }

    private void sendPrompt(Context context, String prompt) {
        refreshAll(context, "正在发送：" + prompt);
        final String finalPrompt = prompt;
        new Thread(() -> {
            String result;
            try {
                result = new HermesApi(context).send(finalPrompt);
            } catch (Exception e) {
                result = "请求失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
            String display = result.length() > 120 ? result.substring(0, 120) : result;
            new Handler(Looper.getMainLooper()).post(() -> {
                refreshAll(context, display);
                Toast.makeText(context, display, Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    public static void refreshAll(Context context, String status) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, HermesWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) {
            updateWidget(context, manager, id, status);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager manager, int appWidgetId, String status) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.hermes_widget);
        views.setTextViewText(R.id.status, status);
        views.setOnClickPendingIntent(R.id.btn_status, sendIntent(context, appWidgetId, "请告诉我当前 Hermes 状态和可用工具。"));
        views.setOnClickPendingIntent(R.id.btn_summary, sendIntent(context, appWidgetId, "请总结我最近的会话和待办事项。"));
        views.setOnClickPendingIntent(R.id.btn_settings, settingsIntent(context, appWidgetId));
        manager.updateAppWidget(appWidgetId, views);
    }

    private static PendingIntent sendIntent(Context context, int id, String prompt) {
        Intent intent = new Intent(context, HermesWidgetProvider.class);
        intent.setAction(ACTION_SEND_PROMPT);
        intent.putExtra(EXTRA_PROMPT, prompt);
        return PendingIntent.getBroadcast(
                context,
                id + prompt.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent settingsIntent(Context context, int id) {
        Intent intent = new Intent(context, SettingsActivity.class);
        return PendingIntent.getActivity(
                context,
                id + 9999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}

class HermesApi {
    private final Context context;

    HermesApi(Context context) {
        this.context = context;
    }

    String send(String prompt) throws Exception {
        String endpoint = context.getSharedPreferences("hermes_card", Context.MODE_PRIVATE)
                .getString("endpoint", "http://127.0.0.1:8000/chat");
        String token = context.getSharedPreferences("hermes_card", Context.MODE_PRIVATE)
                .getString("token", "");
        if (endpoint == null || endpoint.trim().isEmpty()) endpoint = "http://127.0.0.1:8000/chat";
        if (token == null) token = "";

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(60000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        if (!token.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }

        String payload = "{\"message\":" + jsonQuote(prompt) + "}";
        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(payload);
        }

        int code = connection.getResponseCode();
        java.io.InputStream stream = (code >= 200 && code <= 299) ? connection.getInputStream() : connection.getErrorStream();
        String body = "";
        if (stream != null) {
            try (java.util.Scanner scanner = new java.util.Scanner(stream, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
                body = scanner.hasNext() ? scanner.next() : "";
            }
        }
        connection.disconnect();
        if (body.trim().isEmpty()) return "已发送，但服务器返回为空";
        return body;
    }

    private static String jsonQuote(String value) {
        StringBuilder out = new StringBuilder();
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"': out.append("\\\""); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default: out.append(c);
            }
        }
        out.append('"');
        return out.toString();
    }
}
