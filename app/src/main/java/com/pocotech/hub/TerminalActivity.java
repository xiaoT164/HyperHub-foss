package com.pocotech.hub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.muntashirakon.adb.AdbStream;

public class TerminalActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    private static final String PREFS_NAME     = "terminal_prefs";
    private static final String KEY_HELP_SHOWN = "help_shown";
    private static final String KEY_HOST        = "host";
    private static final String KEY_PAIR_PORT   = "pair_port";
    private static final String KEY_CONNECT_PORT= "connect_port";

    // ── Views ─────────────────────────────────────────────────────────────
    private EditText  etHost, etPairPort, etPairCode, etConnectPort, etCommand;
    private TextView  tvOutput, tvStatus;
    private ScrollView scrollOutput;
    private Button    btnPair, btnConnect, btnSend, btnDisconnect, btnClear;

    // ── ADB State ─────────────────────────────────────────────────────────
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AdbConnectionManager adbManager;
    private AdbStream shellStream;
    private OutputStream shellOut;
    private volatile boolean shellRunning = false;

    // ── Command History ───────────────────────────────────────────────────
    private final List<String> cmdHistory = new ArrayList<>();
    private int historyIndex = -1;
    private static final int MAX_HISTORY = 50;

    // ── Output limit ──────────────────────────────────────────────────────
    private static final int MAX_OUTPUT_CHARS = 40_000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);
        UiLocalizer.localizeViewTree(getWindow().getDecorView(), this);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        bindViews();
        setupQuickCommands();
        setupInputField();

        etPairCode.setInputType(InputType.TYPE_CLASS_NUMBER);
        restoreFields();
        ensureAdbManager();

        btnPair.setOnClickListener(v -> doPair());
        btnConnect.setOnClickListener(v -> doConnect());
        btnSend.setOnClickListener(v -> sendCommand());
        btnDisconnect.setOnClickListener(v -> disconnect());
        if (btnClear != null) btnClear.setOnClickListener(v -> clearOutput());

        View btnHelp = findViewById(R.id.term_btn_help);
        if (btnHelp != null) btnHelp.setOnClickListener(v -> showHelpDialog());

        View btnDevSettings = findViewById(R.id.term_btn_dev_settings);
        if (btnDevSettings != null) btnDevSettings.setOnClickListener(v -> openDeveloperSettings());

        setShellUiEnabled(false);
        setStatus(UiLocalizer.translate(this, "Не подключено"));

        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!sp.getBoolean(KEY_HELP_SHOWN, false)) {
            showHelpDialog();
            sp.edit().putBoolean(KEY_HELP_SHOWN, true).apply();
        }
    }

    private void bindViews() {
        etHost        = findViewById(R.id.term_host);
        etPairPort    = findViewById(R.id.term_pair_port);
        etPairCode    = findViewById(R.id.term_pair_code);
        etConnectPort = findViewById(R.id.term_connect_port);
        etCommand     = findViewById(R.id.term_command);
        tvOutput      = findViewById(R.id.term_output);
        tvStatus      = findViewById(R.id.term_status);
        scrollOutput  = findViewById(R.id.term_output_scroll);
        btnPair       = findViewById(R.id.term_btn_pair);
        btnConnect    = findViewById(R.id.term_btn_connect);
        btnSend       = findViewById(R.id.term_btn_send);
        btnDisconnect = findViewById(R.id.term_btn_disconnect);
        btnClear      = findViewById(R.id.term_btn_clear);
    }

    // ── Quick command chips ────────────────────────────────────────────────
    private void setupQuickCommands() {
        LinearLayout quickBar = findViewById(R.id.term_quick_bar);
        if (quickBar == null) return;

        String[][] quickCmds = {
            {"ls",       "ls"},
            {"pwd",      "pwd"},
            {"id",       "id"},
            {"ps",       "ps | head -20"},
            {"uptime",   "uptime"},
            {"df -h",    "df -h"},
            {"free",     "cat /proc/meminfo | head -5"},
            {"cpu",      "cat /proc/cpuinfo | grep 'Hardware\\|Processor' | head -2"},
            {"clear",    "__CLEAR__"},
        };

        for (final String[] cmd : quickCmds) {
            Button chip = new Button(this);
            chip.setText(cmd[0]);
            chip.setTextSize(11f);
            chip.setTextColor(Color.WHITE);
            chip.setAllCaps(false);
            chip.setTypeface(Typeface.MONOSPACE);

            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(Color.argb(60, 100, 150, 255));
            bg.setCornerRadius(20 * getResources().getDisplayMetrics().density);
            bg.setStroke(1, Color.argb(80, 150, 180, 255));
            chip.setBackground(bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = (int)(8 * getResources().getDisplayMetrics().density);
            chip.setLayoutParams(lp);

            chip.setPadding(
                (int)(14 * getResources().getDisplayMetrics().density), (int)(6 * getResources().getDisplayMetrics().density),
                (int)(14 * getResources().getDisplayMetrics().density), (int)(6 * getResources().getDisplayMetrics().density));

            chip.setOnClickListener(v -> {
                if (cmd[1].equals("__CLEAR__")) {
                    clearOutput();
                } else if (shellRunning) {
                    etCommand.setText(cmd[1]);
                    sendCommand();
                } else {
                    etCommand.setText(cmd[1]);
                    etCommand.setSelection(etCommand.getText().length());
                    toast(LocaleHelper.isEnglish(TerminalActivity.this) ? "Connect to ADB first" : "Сначала подключись к ADB");
                }
            });
            quickBar.addView(chip);
        }
    }

    // ── Input field: history navigation + send on Enter ───────────────────
    private void setupInputField() {
        if (etCommand == null) return;

        etCommand.setImeOptions(EditorInfo.IME_ACTION_SEND | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        etCommand.setSingleLine(true);
        etCommand.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

        etCommand.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                sendCommand();
                return true;
            }
            return false;
        });

        etCommand.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                navigateHistory(-1);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                navigateHistory(1);
                return true;
            }
            return false;
        });
    }

    private void navigateHistory(int direction) {
        if (cmdHistory.isEmpty()) return;
        historyIndex = Math.max(0, Math.min(cmdHistory.size() - 1, historyIndex + direction));
        etCommand.setText(cmdHistory.get(cmdHistory.size() - 1 - historyIndex));
        etCommand.setSelection(etCommand.getText().length());
    }

    @Override
    protected void onPause() {
        super.onPause();
        persistFields();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
        executor.shutdownNow();
    }

    // ── Restore / Persist ─────────────────────────────────────────────────

    private void restoreFields() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        etHost.setText(sp.getString(KEY_HOST, "127.0.0.1"));
        etPairPort.setText(sp.getString(KEY_PAIR_PORT, ""));
        etConnectPort.setText(sp.getString(KEY_CONNECT_PORT, ""));
    }

    private void persistFields() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_HOST,         etHost.getText().toString().trim())
                .putString(KEY_PAIR_PORT,    etPairPort.getText().toString().trim())
                .putString(KEY_CONNECT_PORT, etConnectPort.getText().toString().trim())
                .apply();
    }

    private void ensureAdbManager() {
        if (adbManager != null) return;
        try {
            adbManager = AdbConnectionManager.getInstance(getApplicationContext());
        } catch (Exception e) {
            appendOutput("[!] Ошибка инициализации ADB: " + e.getMessage() + "\n", OutputType.ERROR);
        }
    }

    // ── Help Dialog ───────────────────────────────────────────────────────

    private void showHelpDialog() {
        String text =
            "КАК ПОДКЛЮЧИТЬСЯ К ADB WIRELESS\n\n" +
            "1. Откройте Настройки → О телефоне → нажмите 7 раз на «Версия MIUI / Номер сборки» " +
            "— появится «Вы стали разработчиком».\n\n" +
            "2. Настройки → Дополнительно → Для разработчиков.\n\n" +
            "3. Включите «Отладка по USB» (если есть).\n\n" +
            "4. Включите «Беспроводная отладка» → нажмите на пункт → " +
            "«Сопряжение устройства с помощью кода».\n\n" +
            "5. Откроется окно с IP, портом сопряжения и 6-значным кодом.\n\n" +
            "6. Введите эти данные в блок СОПРЯЖЕНИЕ и нажмите кнопку.\n\n" +
            "7. На экране «Беспроводная отладка» появится основной порт.\n" +
            "   Введите его в блок ПОДКЛЮЧЕНИЕ → нажмите «Подключить».\n\n" +
            "8. Внизу появится поле для команд — вводите ADB shell команды!\n\n" +
            "Быстрые команды доступны через кнопки над полем ввода.\n" +
            "История команд: кнопки ↑ ↓ на клавиатуре.\n" +
            "IP обычно 127.0.0.1 если терминал запущен на том же устройстве.";

        new AlertDialog.Builder(this)
                .setTitle("Как подключиться к ADB")
                .setMessage(text)
                .setPositiveButton("Понятно", null)
                .setNeutralButton("Открыть настройки разработчика", (d, w) -> openDeveloperSettings())
                .show();
    }

    private void openDeveloperSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Exception e) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (Exception ex) { toast("Не удалось открыть настройки"); }
        }
    }

    // ── Pairing ───────────────────────────────────────────────────────────

    private void doPair() {
        ensureAdbManager();
        String host    = etHost.getText().toString().trim();
        String portStr = etPairPort.getText().toString().trim();
        String code    = etPairCode.getText().toString().trim();

        if (host.isEmpty() || portStr.isEmpty() || code.isEmpty()) {
            toast("Заполни IP, порт сопряжения и код");
            return;
        }

        int port;
        try { port = Integer.parseInt(portStr); }
        catch (NumberFormatException e) { toast("Неверный порт"); return; }

        hideKeyboard();
        persistFields();
        setStatus("Сопряжение...");
        btnPair.setEnabled(false);

        executor.execute(() -> {
            try {
                boolean ok = adbManager != null && adbManager.pair(host, port, code);
                mainHandler.post(() -> {
                    btnPair.setEnabled(true);
                    if (ok) {
                        setStatus("Сопряжено. Введи порт подключения и нажми «Подключить»");
                        appendOutput("[OK] Сопряжение успешно!\n", OutputType.SUCCESS);
                    } else {
                        setStatus("Сопряжение не удалось");
                        appendOutput("[ERR] Сопряжение не удалось — проверь код и порт\n", OutputType.ERROR);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    btnPair.setEnabled(true);
                    setStatus("Ошибка сопряжения");
                    appendOutput("[!] Ошибка сопряжения: " + e.getMessage() + "\n", OutputType.ERROR);
                });
            }
        });
    }

    // ── Connect ───────────────────────────────────────────────────────────

    private void doConnect() {
        ensureAdbManager();
        String host    = etHost.getText().toString().trim();
        String portStr = etConnectPort.getText().toString().trim();

        if (host.isEmpty() || portStr.isEmpty()) {
            toast("Заполни IP и порт подключения");
            return;
        }

        int port;
        try { port = Integer.parseInt(portStr); }
        catch (NumberFormatException e) { toast("Неверный порт"); return; }

        hideKeyboard();
        persistFields();
        setStatus("Подключение...");
        btnConnect.setEnabled(false);

        executor.execute(() -> {
            try {
                boolean connected = adbManager != null && adbManager.connect(host, port);
                if (!connected) {
                    mainHandler.post(() -> {
                        btnConnect.setEnabled(true);
                        setStatus("Не удалось подключиться");
                        appendOutput("[ERR] Подключение не удалось\n", OutputType.ERROR);
                    });
                    return;
                }

                AdbStream stream = adbManager.openStream("shell:");
                shellStream = stream;
                shellOut    = stream.openOutputStream();
                InputStream in = stream.openInputStream();
                shellRunning = true;

                mainHandler.post(() -> {
                    btnConnect.setEnabled(true);
                    setStatus("Подключено — shell активен (" + host + ":" + port + ")");
                    appendOutput("[OK] Подключено! Shell готов к работе.\n", OutputType.SUCCESS);
                    appendOutput("─────────────────────────────────\n", OutputType.DIM);
                    setShellUiEnabled(true);
                    etCommand.requestFocus();
                });

                readLoop(in);
            } catch (Exception e) {
                mainHandler.post(() -> {
                    btnConnect.setEnabled(true);
                    setStatus("Ошибка подключения");
                    appendOutput("[!] Ошибка: " + e.getMessage() + "\n", OutputType.ERROR);
                    setShellUiEnabled(false);
                });
            }
        });
    }

    // ── Read Loop ─────────────────────────────────────────────────────────

    private void readLoop(InputStream in) {
        byte[] buf = new byte[4096];
        try {
            int n;
            while (shellRunning && (n = in.read(buf)) != -1) {
                if (n == 0) continue;
                String chunk = new String(buf, 0, n, StandardCharsets.UTF_8);
                mainHandler.post(() -> appendOutput(chunk, OutputType.NORMAL));
            }
        } catch (IOException e) {
            if (shellRunning) {
                mainHandler.post(() -> {
                    appendOutput("\n[~] Соединение закрыто: " + e.getMessage() + "\n", OutputType.DIM);
                    setShellUiEnabled(false);
                    setStatus("Отключено");
                });
            }
        } finally {
            shellRunning = false;
        }
    }

    // ── Send Command ──────────────────────────────────────────────────────

    private void sendCommand() {
        if (shellOut == null || !shellRunning) {
            toast("Сначала подключись к ADB");
            return;
        }
        String cmd = etCommand.getText().toString();
        if (cmd.trim().isEmpty()) return;

        if (cmdHistory.isEmpty() || !cmdHistory.get(cmdHistory.size() - 1).equals(cmd)) {
            cmdHistory.add(cmd);
            if (cmdHistory.size() > MAX_HISTORY) cmdHistory.remove(0);
        }
        historyIndex = -1;

        appendOutput("$ " + cmd + "\n", OutputType.COMMAND);
        etCommand.setText("");

        final String finalCmd = cmd;
        executor.execute(() -> {
            try {
                shellOut.write((finalCmd + "\n").getBytes(StandardCharsets.UTF_8));
                shellOut.flush();
            } catch (IOException e) {
                mainHandler.post(() ->
                    appendOutput("[!] Ошибка отправки: " + e.getMessage() + "\n", OutputType.ERROR));
            }
        });
    }

    // ── Disconnect ────────────────────────────────────────────────────────

    private void disconnect() {
        shellRunning = false;
        if (shellOut != null) {
            try { shellOut.close(); } catch (IOException ignored) {}
            shellOut = null;
        }
        if (shellStream != null) {
            try { shellStream.close(); } catch (IOException ignored) {}
            shellStream = null;
        }
        try { if (adbManager != null) adbManager.close(); } catch (Exception ignored) {}
        adbManager = null;

        setShellUiEnabled(false);
        setStatus("Отключено");
    }

    // ── Output ────────────────────────────────────────────────────────────

    private enum OutputType { NORMAL, COMMAND, SUCCESS, ERROR, DIM }

    private void appendOutput(String text, OutputType type) {
        CharSequence current = tvOutput.getText();
        if (current != null && current.length() > MAX_OUTPUT_CHARS) {
            String trimmed = current.toString().substring(current.length() - MAX_OUTPUT_CHARS / 2);
            tvOutput.setText("... [вывод обрезан] ...\n" + trimmed);
        }

        int color;
        switch (type) {
            case COMMAND: color = Color.rgb(100, 200, 255); break;
            case SUCCESS: color = Color.rgb(80, 220, 120);  break;
            case ERROR:   color = Color.rgb(255, 90, 90);   break;
            case DIM:     color = Color.argb(120, 180, 180, 200); break;
            default:      color = Color.rgb(200, 230, 200); break;
        }

        SpannableString span = new SpannableString(text);
        span.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvOutput.append(span);
        scrollOutput.post(() -> scrollOutput.fullScroll(View.FOCUS_DOWN));
    }

    private void clearOutput() {
        tvOutput.setText("");
        appendOutput("[~] Вывод очищен\n", OutputType.DIM);
    }

    private void setStatus(String text) {
        if (tvStatus != null) tvStatus.setText(text);
    }

    private void setShellUiEnabled(boolean enabled) {
        if (etCommand != null)    etCommand.setEnabled(enabled);
        if (btnSend != null)      btnSend.setEnabled(enabled);
        if (btnDisconnect != null)btnDisconnect.setEnabled(enabled);

        if (etCommand != null) {
            etCommand.setHint(enabled ? "введите команду..." : "подключитесь к ADB");
            etCommand.setHintTextColor(enabled ? 0xFF405860 : 0xFF303848);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (imm != null && focus != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
