package com.pocotech.hub;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class UiLocalizer {

    private static final Map<String, String> RU_TO_EN = new LinkedHashMap<>();

    static {
        put("Главная", "Home");
        put("Обновл.", "Updates");
        put("Инфо", "Info");
        put("Тест", "Bench");
        put("Настройки", "Settings");
        put("Терминал", "Terminal");
        put("Пасхалка, фиксы переключателей и HubBench 2.2", "Hidden mode, toggle fixes and HubBench 2.2");
        put("История версий HyperHub", "HyperHub version history");
        put("Следить за обновлениями", "Follow updates");
        put("• Переключатели теперь корректно двигаются, не застревают после открытия экрана и переключаются по нажатию на всю строку\n• HubBench 2.2: реальные серии замеров, медиана, разброс, latency RAM и процент доверия\n• Подчищены мелкие UX-проблемы и обновлены версии приложения/карточек интерфейса", "• Toggles now slide correctly, do not get stuck after opening the screen, and can be switched by tapping the whole row\n• HubBench 2.2: real measurement series, median, variance, RAM latency, and confidence percentage\n• Minor UX issues were cleaned up and app/interface card versions were refreshed");
        put("• Исправлена начальная позиция ползунков и расширена зона нажатия для переключателей\n• Статусы бенчмарка теперь корректно локализуются, а RAM-секция показывает ещё и задержку доступа", "• Fixed the initial toggle position and expanded the tap target for switches\n• Benchmark status messages are now localized correctly, and the RAM section also shows access latency");
        put("•  Игровой турбо-режим — Game Turbo через ADB\n•  MIUI/HyperOS Debug Tools — секретные коды\n•  Xiaomi / POCO Labs — бета-функции прошивки\n•  Уровень сигнала Wi-Fi (RSSI) в разделе Инфо\n•  Тип зарядки (USB / AC / беспроводная)\n• ↑↓ История команд в терминале (кнопки ↑ ↓)\n•  Быстрые команды — chips над полем ввода", "•  Game Turbo mode via ADB\n•  MIUI/HyperOS Debug Tools with secret codes\n•  Xiaomi / POCO Labs firmware beta features\n•  Wi-Fi signal strength (RSSI) in the Info screen\n•  Charging type (USB / AC / wireless)\n• ↑↓ Terminal command history with arrow buttons\n•  Quick command chips above the input field");
        put("• Нажатие на активный таб больше не перезагружает экран\n• Терминал: поле ввода не уезжает под клавиатуру\n• Исправлено наложение экранов при быстром тапе\n• Копирование инфо устройства — улучшенный формат\n• Closesheet: корректная высота при сворачивании\n• Акцентный цвет применяется к навбару сразу", "• Tapping the active tab no longer reloads the screen\n• Terminal: input field no longer hides behind the keyboard\n• Fixed screen overlap during rapid taps\n• Improved device-info copy format\n• Close sheet now keeps the correct collapsed height\n• Accent color now applies to the nav bar instantly");
        put("• Цветной вывод терминала: команды, успех, ошибки\n• Ограничение размера лога (40k символов) — нет утечек памяти\n• Разделы Главной разбиты на секции с заголовками\n• Версия в подзаголовке главного экрана\n• Предикт: локальный маппинг кодов → маркетинговых имён\n• Сводка устройства с заголовком и разделителем\n• Улучшен UX карточек подключения в терминале", "• Color terminal output for commands, success, and errors\n• Log size limit (40k chars) prevents memory leaks\n• Home screen is split into cleaner titled sections\n• Version added to the home subtitle\n• Prediction screen now maps codenames to marketing names locally\n• Device summary now has a proper title and divider\n• Better UX for terminal connection cards");
        put("Telegram Канал", "Telegram channel");
        put("Telegram канал", "Telegram channel");
        put("Новости и обновления HyperHub", "HyperHub news and updates");
        put("HyperHub", "HyperHub");
        put("v1.3.3.2 · Утилиты HyperOS", "v1.3.3.2 · HyperOS toolkit");
        put("Поиск по функциям", "Search features");
        put("Ничего не найдено", "Nothing found");
        put("Поддержать HyperHub", "Support HyperHub");
        put("DonationAlerts — спасибо!", "DonationAlerts — thank you!");
        put("ОПТИМИЗАЦИЯ", "OPTIMIZATION");
        put("Разгон FPS", "FPS boost");
        put("Буфер трассировки и GPU", "Trace buffer and GPU");
        put("Режим производительности", "Performance mode");
        put("Флагманские анимации", "Flagship animations");
        put("Blur-эффекты на Redmi/POCO", "Blur effects on Redmi/POCO");
        put("RAM и фоновые процессы", "RAM and background apps");
        put("Управление памятью LMK", "LMK memory management");
        put("Оптимизация пинга", "Ping optimization");
        put("Настройка энергорежима", "Power tuning");
        put("Low power режим и таймаут", "Low power mode and timeout");
        put("Игровой турбо-режим", "Game Turbo mode");
        put("Game Turbo — CPU/GPU приоритет", "Game Turbo — CPU/GPU priority");
        put("СКРЫТЫЕ НАСТРОЙКИ", "HIDDEN SETTINGS");
        put("Отключение bloatware", "Disable bloatware");
        put("Лишние Mi-приложения", "Extra Mi apps");
        put("Инженерный экран сети", "Engineering network screen");
        put("CIT Диагностика", "CIT diagnostics");
        put("Заводской тест аппаратуры", "Factory hardware tests");
        put("Параметры разработчика", "Developer options");
        put("ADB и расширенные настройки", "ADB and advanced settings");
        put("Скрытые настройки дисплея", "Hidden display settings");
        put("DC Dimming, 120 Гц, PWM", "DC Dimming, 120 Hz, PWM");
        put("Секретные коды инженерных меню", "Secret engineering menu codes");
        put("ГАЙДЫ", "GUIDES");
        put("Отключение рекламы", "Disable ads");
        put("MSA, GetApps, браузер", "MSA, GetApps, browser");
        put("Чистый рабочий стол", "Clean home screen");
        put("Скрыть подписи под иконками", "Hide icon labels");
        put("Фишки смены региона", "Region switch perks");
        put("SG, IN, US — разные плюшки", "SG, IN, US — different perks");
        put("Second Space / Клонирование", "Second Space / App cloning");
        put("Второй профиль, клон-приложения", "Second profile, cloned apps");
        put("Сторонние шрифты", "Custom fonts");
        put("Root и Shizuku методы", "Root and Shizuku methods");
        put("Xiaomi / POCO Labs", "Xiaomi / POCO Labs");
        put("Скрытые бета-функции системы", "Hidden system beta features");
        put("СООБЩЕСТВО", "COMMUNITY");
        put("@HyperHubRu — новости и советы", "@HyperHubRu — news and tips");
        put("Обновления", "Updates");
        put("Текущая", "Current");
        put("  Текущая ", "  Current ");
        put("НОВОЕ", "NEW");
        put("ИСПРАВЛЕНО", "FIXED");
        put("УЛУЧШЕНО", "IMPROVED");
        put("Глобальное обновление", "Major update");
        put("Предыдущая", "Previous");
        put("Архив", "Archive");
        put("Терминал и стекло", "Terminal and glass");
        put("Инфо и предикт", "Info and prediction");
        put("Ваше устройство", "Your device");
        put("Список устройств", "Device list");
        put("Результат предикта обновления", "Update prediction result");
        put("Список собран по политике обновлений Xiaomi и открытым совместимым спискам; спорные модели оставлены в статусе «возможно»", "The list is based on Xiaomi update policies and public compatibility lists; disputed devices are kept as “possible”.");
        put("Системная информация", "System information");
        put("↻  Обновить", "↻  Refresh");
        put("⎘  Скопировать", "⎘  Copy");
        put("УСТРОЙСТВО", "DEVICE");
        put("Модель", "Model");
        put("Android", "Android");
        put("Сборка", "Build");
        put("Патч безоп.", "Security patch");
        put("Аптайм", "Uptime");
        put("Разрешение", "Resolution");
        put("ПРОЦЕССОР", "PROCESSOR");
        put("Чипсет", "Chipset");
        put("Ядра", "Cores");
        put("Макс. частота", "Max frequency");
        put("ABI", "ABI");
        put("GPU", "GPU");
        put("Темп. CPU", "CPU temp");
        put("БАТАРЕЯ", "BATTERY");
        put("Статус", "Status");
        put("Температура", "Temperature");
        put("Напряжение", "Voltage");
        put("ПАМЯТЬ", "MEMORY");
        put("RAM", "RAM");
        put("Хранилище", "Storage");
        put("СЕТЬ", "NETWORK");
        put("Оператор", "Carrier");
        put("IP-адрес", "IP address");
        put("Wi-Fi SSID", "Wi-Fi SSID");
        put("Скорость", "Speed");
        put("Уровень сигнала", "Signal strength");
        put("Бенчмарк", "Benchmark");
        put("CPU не определён", "CPU not detected");
        put("Комплексный тест CPU, RAM, накопителя и GPU", "Combined CPU, RAM, storage and GPU test");
        put("Устройство", "Device");
        put("Полный прогон занимает около 40 секунд. Во время замера не трогайте телефон, не открывайте другие приложения и дайте устройству остыть перед повтором.", "A full run takes about 40 seconds. Do not touch the phone, open other apps, or rerun the test until the device has cooled down.");
        put("Нажмите «Запустить»", "Tap \"Start\"");
        put("Запустить тест", "Start benchmark");
        put("Тест выполняется…", "Benchmark in progress…");
        put("CPU: целые числа (6 окон)…", "CPU: integers (6 windows)…");
        put("CPU: плавающая точка (6 окон)…", "CPU: floating point (6 windows)…");
        put("CPU: ветвления (6 окон)…", "CPU: branch prediction (6 windows)…");
        put("CPU: все ядра (4 окна)…", "CPU: all cores (4 windows)…");
        put("RAM: пропускная способность (7 окон)…", "RAM: bandwidth (7 windows)…");
        put("RAM: латентность (7 окон)…", "RAM: latency (7 windows)…");
        put("Накопитель: SQLite (5 прогонов)…", "Storage: SQLite (5 runs)…");
        put("Накопитель: запись (5 прогонов)…", "Storage: write (5 runs)…");
        put("GPU: offscreen-рендер 1080p…", "GPU: 1080p off-screen render…");
        put("Подсчёт медианы и доверия…", "Calculating median and confidence…");
        put("ДЕТАЛИ", "DETAILS");
        put("CPU — одно ядро", "CPU — single core");
        put("CPU — все ядра", "CPU — all cores");
        put("Оперативная память", "Memory");
        put("Накопитель", "Storage");
        put("GPU (3D-рендер)", "GPU (3D render)");
        put("Возможности устройства", "Device capabilities");
        put("️ Бенчмарк использует медиану по нескольким окнам CPU/RAM/I/O. Если видите низкую уверенность — повторите прогон после 2–3 минут простоя и без фоновых загрузок.", "️ The benchmark uses median values across multiple CPU/RAM/I/O windows. If confidence is low, rerun it after 2–3 minutes of idle time with no background downloads.");
        put("Версия 1.3.3.2", "Version 1.3.3.2");
        put("Версия Android", "Android version");
        put("Версия ОС", "OS version");
        put("РАЗРАБОТЧИК", "DEVELOPER");
        put("ОБСУЖДЕНИЕ", "COMMUNITY");
        put("ПРОЧЕЕ", "MORE");
        put("Донаты", "Donations");
        put("Вы можете поддержать разработку", "You can support development");
        put("Политика конфиденциальности", "Privacy policy");
        put("Какие данные собирает приложение", "What data the app collects");
        put("© 2026 XiaoT & HyperHub", "© 2026 XiaoT & HyperHub");
        put("ЯЗЫК И ИНТЕРФЕЙС", "LANGUAGE & INTERFACE");
        put("Язык приложения", "App language");
        put("Выберите язык интерфейса", "Choose the interface language");
        put("Фоновые анимации", "Background animations");
        put("Aurora-эффекты на экранах", "Aurora effects on screens");
        put("Тактильный отклик", "Haptic feedback");
        put("Вибрация при нажатиях", "Vibration on taps");
        put("Заставка при запуске", "Launch splash screen");
        put("Показывать интро HyperHub", "Show HyperHub intro");
                put("Не подключено", "Not connected");
        put("СОПРЯЖЕНИЕ (PAIRING)", "PAIRING");
        put("Шаг 1", "Step 1");
        put("Порт сопряжения", "Pairing port");
        put("Код сопряжения", "Pairing code");
        put("Сопряжение", "Pair");
        put("ПОДКЛЮЧЕНИЕ", "CONNECTION");
        put("Шаг 2", "Step 2");
        put("Порт подключения", "Connection port");
        put("Подключить", "Connect");
        put("SHELL ВЫВОД", "SHELL OUTPUT");
        put("Очистить", "Clear");
        put("введите команду...", "enter a command...");
        put("Данные обновлены", "Data refreshed");
        put("Сводка скопирована", "Summary copied");
        put("Не удалось открыть ссылку", "Could not open the link");
                        put("Заряд Hyper Reactor", "Hyper Reactor charge");
        put("Подготовка теста…", "Preparing benchmark…");
        put("Идёт тест…", "Benchmark running…");
        put("Не удалось выполнить тест", "Benchmark failed");
        put("Попробуйте ещё раз", "Please try again");
        put("Запуск теста GPU…", "Starting GPU test…");
        put("Тест выполняется…", "Benchmark running…");
        put("Отменить", "Cancel");
        put("Отменено", "Cancelled");
        put("Запустить ещё раз", "Run again");
        put("Готово", "Done");
        put("Точность", "Confidence");
        put("GPU-тест недоступен на этом устройстве", "GPU test is unavailable on this device");
        put("просадка", "drop");
        put("ядер", "cores");
        put("МиБ/с", "MiB/s");
        put("МиБ/с запись", "MiB/s write");
        put("н/д", "n/a");
        put("нс", "ns");
        put("Получит HyperOS 4", "Will get HyperOS 4");
        put("Получит обновление HyperOS 4", "Will receive the HyperOS 4 update");
        put("Не получит HyperOS 4", "Will not get HyperOS 4");
        put("Не получит обновление HyperOS 4", "Will not receive the HyperOS 4 update");
        put("Возможно, не получит", "Possibly not");
        put("Данные по устройству отсутствуют", "No data available for this device");
        put("Не подключено", "Not connected");
        put("Wi‑Fi выключен", "Wi‑Fi off");
        put("Заряжается", "Charging");
        put("Разряжается", "Discharging");
        put("Заряжен", "Charged");
        put("Не заряд.", "Not charging");
        put("Н/Д", "N/A");
        put("Скопировать команды", "Copy commands");
        put("Скопировать ADB-команды", "Copy ADB commands");
        put("Скопировать инфо", "Copy info");
        put("Скопировать команду", "Copy command");
        put("Понятно", "OK");
        put("Скопировано!", "Copied!");
        put("Открыть", "Open");
        put("Открыть приложения", "Open apps");
        put("Отмена", "Cancel");

        put("️ Влияние: незначительно увеличивает нагрузку на CPU при трассировке. Рекомендуется на устройствах с 6+ ГБ RAM.\n\n1. Настройки → О телефоне → нажмите 7 раз на «Версию ОС», чтобы стать разработчиком.\n\n2. Дополнительно → Для разработчиков → найдите «Размер буфера трассировки» и выставьте максимум (32 МБ на ядро).\n\n3. Отключите «Включить трассировку по умолчанию» — снимает фоновую запись системных событий.", "️ Impact: slightly increases CPU load while tracing. Recommended for devices with 6+ GB RAM.\n\n1. Settings → About phone → tap \"OS version\" 7 times to unlock developer mode.\n\n2. Additional settings → Developer options → set \"Trace buffer size\" to the maximum (32 MB per core).\n\n3. Disable \"Enable tracing by default\" to stop background system-event recording.");
        put("️ Влияние: повышает энергопотребление и нагрев устройства. Не рекомендуется при заряде ниже 30%.\n\nЕсли команда возвращает ошибку — служба MQS недоступна на вашей прошивке (часть версий HyperOS 2.x).\n\nТребуется Termux или ADB Shell. Скопируйте команды и выполните по одной. После — перезагрузите телефон.", "️ Impact: increases power usage and heat. Not recommended below 30% battery.\n\nIf a command fails, the MQS service is unavailable on your firmware build (some HyperOS 2.x versions).\n\nTermux or ADB Shell is required. Copy the commands, run them one by one, then reboot the phone.");
        put("️ Влияние: фоновое размытие увеличивает нагрузку на GPU. На слабых устройствах (4 ГБ RAM) возможны подвисания.\n\nВключает плавные анимации и blur-эффекты на бюджетных POCO/Redmi.\n\n1. Скачайте Brevent или Shizuku + aShell.\n2. Активируйте беспроводную отладку.\n3. Выполните команды через ADB.\n4. Перезагрузите устройство.", "️ Impact: background blur increases GPU load. Weaker devices (4 GB RAM) may stutter.\n\nEnables smoother animations and blur effects on entry-level POCO/Redmi devices.\n\n1. Install Brevent or Shizuku + aShell.\n2. Enable wireless debugging.\n3. Run the commands through ADB.\n4. Reboot the device.");
        put(" Android самостоятельно управляет памятью через механизм LMK (Low Memory Killer) — это эффективнее любой ручной чистки.\n\nЕсли телефон реально тормозит из-за нехватки RAM:\n\n1. Настройки → Приложения → найдите «тяжёлые» приложения и ограничьте их фоновую активность.\n\n2. Безопасность → Оптимизация — штатный инструмент HyperOS.\n\n3. Отключите ненужные автозапуски: Настройки → Приложения → Автозапуск.", " Android manages memory on its own with LMK (Low Memory Killer) — it is more effective than any manual cleaner.\n\nIf your phone is genuinely slowing down because of low RAM:\n\n1. Settings → Apps → find heavy apps and restrict their background activity.\n\n2. Security → Optimization — HyperOS built-in maintenance tool.\n\n3. Disable unnecessary autostarts: Settings → Apps → Autostart.");
        put("️ Влияние: отключает автоматическую оценку качества сети. Телефон перестанет автоматически переключаться на более быстрый Wi-Fi.\n\nОтключает ограничение частоты Wi-Fi-сканирования и лишние сетевые службы — снижает задержку в играх.\n\nТребуется ADB Shell. Выполните команды через Termux или компьютер.", "️ Impact: disables automatic network scoring. The phone will stop switching to a faster Wi-Fi network automatically.\n\nDisables Wi-Fi scan throttling and extra network services to reduce gaming latency.\n\nADB Shell is required. Run the commands via Termux or a computer.");
        put("️ Влияние: отключает принудительный режим экономии — телефон будет расходовать чуть больше энергии в обычном режиме, но работать стабильнее.\n\n• Снимает принудительный Low Power режим\n• Выставляет таймаут экрана 2 минуты\n\nДля реальной экономии батареи дополнительно отключите «Всегда активный экран» и уберите лишние фоновые приложения.", "️ Impact: disables forced power saving — the phone will use a bit more battery in normal mode, but stay more stable.\n\n• Removes forced Low Power mode\n• Sets the screen timeout to 2 minutes\n\nFor real battery savings, also disable Always-on Display and close unnecessary background apps.");
        put("️ Влияние: блокирует уведомления во время игры. После выхода из игры режим отключается автоматически.\n\nВключает встроенный «Game Turbo» режим HyperOS:\n• Максимальная приоритизация CPU/GPU для игр\n• Отключение лишних фоновых уведомлений\n• Улучшенный тач-отклик\n\nТакже: Настройки → Особые возможности → Game Turbo → добавьте ваши игры.", "️ Impact: blocks notifications during gaming. The mode turns off automatically when you leave the game.\n\nEnables the built-in HyperOS Game Turbo mode:\n• Maximum CPU/GPU priority for games\n• Fewer distracting background notifications\n• Better touch responsiveness\n\nAlso check: Settings → Special features → Game Turbo → add your games.");
        put(" Набор скрытых инженерных инструментов для диагностики:\n\n• *#*#6484#*#* — Диагностика hardware\n• *#*#4636#*#* — Phone Info (сотовая сеть, батарея)\n• *#*#2846579#*#* — ProjectMenu HUAWEI (только EMUI)\n• *#*#0*#*#* — LCD тест\n• *#*#7378423#*#* — Service menu Sony\n\nДля MIUI/HyperOS нажмите кнопку ниже для открытия инженерного меню через код набора.", " A bundle of hidden engineering tools for diagnostics:\n\n• *#*#6484#*#* — hardware diagnostics\n• *#*#4636#*#* — Phone Info (cellular network, battery)\n• *#*#2846579#*#* — HUAWEI ProjectMenu (EMUI only)\n• *#*#0*#*#* — LCD test\n• *#*#7378423#*#* — Sony service menu\n\nOn MIUI/HyperOS, use the button below to open the engineering menu via dialer code.");
        put(" Бета-функции, доступные через скрытые разделы прошивки:\n\n• Always-on Display — Настройки → Экран → Всегда активный экран\n\n• Note Asst — скрытый AI-помощник, активируется через Глобальный/CN регион\n\n• AI Call Recording (CN прошивка) — автоматическая транскрипция\n\n• Hyper Charge профили — Настройки → Батарея → Режим зарядки\n\n• Dynamic Island style — сторонние решения: Dynamic Notch, ZaBar\n\n️ Для части функций нужен CN-регион или специальная прошивка.", " Beta features available through hidden firmware sections:\n\n• Always-on Display — Settings → Display → Always-on display\n\n• Note Asst — hidden AI helper enabled on Global/CN regions\n\n• AI Call Recording (CN firmware) — automatic transcription\n\n• Hyper Charge profiles — Settings → Battery → Charging mode\n\n• Dynamic Island style — third-party options like Dynamic Notch and ZaBar\n\n️ Some features require a CN region or a specific firmware build.");
        put("️ Отключение системных MIUI-служб может нарушить работу уведомлений, синхронизации и платёжных сервисов.\n\nНе отключайте то, в чём не уверены.\n\nБезопасно отключать: Mi Video, Mi Music, Mi Browser, GetApps, Mi AI.", "️ Disabling system MIUI services can break notifications, sync, and payment services.\n\nDo not disable components you do not understand.\n\nUsually safe to disable: Mi Video, Mi Music, Mi Browser, GetApps, Mi AI.");
        put(" Инженерный экран для просмотра параметров сотовой сети: уровень сигнала, тип сети (LTE/5G), информация о базовых станциях.\n\n️ Не меняйте настройки в этом меню без понимания — можно потерять сигнал сети. Используйте только для просмотра.\n\nЕсли экран не откроется — попробуйте набрать *#*#4636#*#* в приложении «Телефон».", " Engineering screen for checking cellular details: signal strength, network type (LTE/5G), and base-station info.\n\n️ Do not change anything here unless you know what it does — you may lose network signal. Use it for viewing only.\n\nIf the screen does not open, dial *#*#4636#*#* in the Phone app.");
        put(" Заводской тест аппаратной части: экран, виброотдача, камера, микрофон, датчики, Wi-Fi, Bluetooth.\n\n️ Некоторые тесты запускают датчики в нестандартном режиме. Не прерывайте тест принудительно — завершайте через кнопку «Выход» внутри приложения.", " Factory hardware diagnostics: display, vibration, camera, microphone, sensors, Wi-Fi, and Bluetooth.\n\n️ Some tests put sensors into non-standard modes. Do not force-close them — exit using the in-app button.");
        put("️ Раздел для опытных пользователей. Некорректные настройки могут снизить производительность или нарушить работу системы.\n\nДля включения ADB: откройте этот раздел → «Отладка по USB» → подтвердите на компьютере при первом подключении.", "️ This section is for experienced users. Wrong settings may reduce performance or break system behavior.\n\nTo enable ADB: open this section → \"USB debugging\" → confirm the fingerprint on your computer when connecting for the first time.");
        put(" Все параметры меняются штатно через настройки — без ADB и root.\n\n• DC Dimming — снижает мерцание ШИМ на низкой яркости. Настройки → Экран → Дополнительные настройки. Полезно при усталости глаз.\n\n• Частота обновления — там же, выберите «Auto» или фиксированный 120 Гц. Auto экономит батарею.\n\n• Цветовой профиль — Настройки → Экран → Цветовая схема. «Насыщенный» + ручной баланс для AMOLED.\n\n• Режим чтения — Настройки → Спецвозможности. Снижает синий свет без желтизны от PWM.\n\n️ DC Dimming может незначительно снизить равномерность яркости экрана.", " All of these options are available in system settings — no ADB or root required.\n\n• DC Dimming — reduces PWM flicker at low brightness. Settings → Display → Additional settings. Useful for eye comfort.\n\n• Refresh rate — choose Auto or a fixed 120 Hz in the same section. Auto saves battery.\n\n• Color profile — Settings → Display → Color scheme. \"Saturated\" plus manual balance works well on AMOLED.\n\n• Reading mode — Settings → Special features. Lowers blue light without the yellowish PWM workaround.\n\n️ DC Dimming may slightly reduce brightness uniformity.");
        put(" Не влияет на производительность. Только убирает рекламные рекомендации.\n\n1. Настройки → Пароли и безопасность → Доступ к личным данным.\n\n2. Найдите «msa» (MIUI System Ads), отзовите разрешение. Подождите 10 сек и подтвердите.\n\n3. Безопасность → Настройки () → Отключите «Получать рекомендации».\n\n4. Повторите в Проводнике и Очистке.\n\n5. Браузер Mi → Настройки → Конфиденциальность → отключите персонализированную рекламу.", " This does not improve performance. It only removes ad recommendations.\n\n1. Settings → Passwords & security → Authorization & revocation.\n\n2. Find \"msa\" (MIUI System Ads), revoke access, wait 10 seconds, and confirm.\n\n3. Security → Settings () → disable \"Receive recommendations\".\n\n4. Repeat the same in File Manager and Cleaner.\n\n5. Mi Browser → Settings → Privacy → disable personalized ads.");
        put("️ После выполнения подписи под иконками пропадут. Чтобы вернуть — перезагрузите телефон или выполните сброс лаунчера.\n\nСкрывает все подписи под иконками — рабочий стол выглядит минималистично.\n\nВыполните в Termux или ADB Shell. После — перезапустите лаунчер или перезагрузите телефон.", "️ After applying this, icon labels will disappear. To restore them, reboot the phone or reset the launcher.\n\nHides all icon labels for a cleaner home screen.\n\nRun it in Termux or ADB Shell, then restart the launcher or reboot the phone.");
        put(" Смена региона обратима — можно вернуть в любой момент. Язык системы остаётся русским.\n\n️ После смены региона некоторые приложения могут попросить обновить данные. Проверьте работу банковских приложений после смены.\n\nНастройки → Расширенные настройки → Регион.\n\nSG — Сингапур: снимает ограничение громкости наушников по EU-нормам. Обновления приходят раньше.\n\nIN — Индия: открывает каталог шрифтов в «Темах» + системные звуки.\n\nUS — США: оптимизирует плавность жестов лаунчера.", " Region switching is reversible — you can change it back at any time. The system language stays the same.\n\n️ Some apps may ask to refresh data after the change. Check banking apps afterward.\n\nSettings → Additional settings → Region.\n\nSG — Singapore: removes EU headphone volume limits and often gets updates earlier.\n\nIN — India: unlocks the fonts catalog in Themes plus extra system sounds.\n\nUS — USA: can improve launcher gesture smoothness.");
        put("️ Second Space занимает дополнительное место в памяти (~1–2 ГБ). На устройствах с 4 ГБ RAM может замедлить переключение между профилями.\n\nSecond Space — второй полноценный профиль с отдельными аккаунтами и приложениями.\n\nКак включить:\nНастройки → Спецвозможности → Second Space → Включить.\n\nКлонирование приложений:\nНастройки → Приложения → Клонирование приложений. Поддерживаются Telegram, WhatsApp, Instagram и другие.\n\nОба профиля полностью изолированы: разные аккаунты Google, разные данные.", "️ Second Space uses extra storage (~1–2 GB). On devices with 4 GB RAM, switching between profiles may feel slower.\n\nSecond Space is a full secondary profile with separate accounts and apps.\n\nHow to enable it:\nSettings → Special features → Second Space → Turn on.\n\nApp cloning:\nSettings → Apps → App cloning. Telegram, WhatsApp, Instagram and more are supported.\n\nBoth profiles are fully isolated: separate Google accounts and separate app data.");
        put("️ Root-метод: замена системного шрифта напрямую. При неправильном шрифте интерфейс может отображаться некорректно. Сделайте резервную копию оригинального файла перед заменой.\n\nС Root:\n1. Скопируйте TTF/OTF в /sdcard/Download/.\n2. Переименуйте в MiLanProVF.ttf.\n3. Через Root Explorer скопируйте в /system/fonts/ с заменой.\n\nБез Root (рекомендуется):\nShizuku + Font Manager из Play Market — позволяет ставить шрифты через ADB без root.", "️ Root method: replaces the system font directly. A bad font file can break UI rendering. Back up the original file before changing it.\n\nWith root:\n1. Copy the TTF/OTF file to /sdcard/Download/.\n2. Rename it to MiLanProVF.ttf.\n3. Use a root file manager to replace the file in /system/fonts/.\n\nWithout root (recommended):\nShizuku + Font Manager from Google Play lets you install fonts through ADB without root.");
    }

    private UiLocalizer() {}

    private static void put(String ru, String en) {
        RU_TO_EN.put(ru, en);
    }

    public static String translate(Context context, String source) {
        if (source == null) return null;
        if (!LocaleHelper.isEnglish(context)) return source;

        String exact = RU_TO_EN.get(source);
        if (exact != null) return exact;

        if (source.endsWith(" ядер")) {
            return source.substring(0, source.length() - " ядер".length()) + " cores";
        }
        if (source.startsWith("Версия ")) {
            return "Version " + source.substring("Версия ".length());
        }
        if (source.contains("\n")) {
            String[] parts = source.split("\\n", -1);
            boolean changed = false;
            for (int i = 0; i < parts.length; i++) {
                String translated = RU_TO_EN.get(parts[i]);
                if (translated != null) {
                    parts[i] = translated;
                    changed = true;
                }
            }
            if (changed) {
                StringBuilder out = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) out.append('\n');
                    out.append(parts[i]);
                }
                return out.toString();
            }
        }
        return source;
    }

    public static void localizeViewTree(View root, Context context) {
        if (root == null || !LocaleHelper.isEnglish(context)) return;
        applyToView(root, context);
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                localizeViewTree(group.getChildAt(i), context);
            }
        }
    }

    private static void applyToView(View view, Context context) {
        CharSequence contentDescription = view.getContentDescription();
        if (contentDescription != null && contentDescription.length() > 0) {
            view.setContentDescription(translate(context, contentDescription.toString()));
        }
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            CharSequence text = tv.getText();
            if (text != null && text.length() > 0) {
                tv.setText(translate(context, text.toString()));
            }
            CharSequence hint = tv.getHint();
            if (hint != null && hint.length() > 0) {
                tv.setHint(translate(context, hint.toString()));
            }
        }
    }

    public static String normalizeSearch(String query) {
        if (query == null) return "";
        String q = query.toLowerCase(Locale.ROOT).trim();
        if (q.equals("settings")) return "настройки settings";
        return q;
    }
}
