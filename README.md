# HyperHub

**HyperOS / MIUI toolkit for Xiaomi, Redmi and POCO devices**

🌐 [hyperhub.dafx.ru](http://hyperhub.dafx.ru) · 💬 [Telegram](https://t.me/HyperHubRu)

HyperHub — это набор инструментов для тонкой настройки и диагностики устройств на HyperOS/MIUI. Приложение объединяет твики производительности, системную информацию, бенчмарк и предсказание доступности обновлений HyperOS 4 — всё в одном месте, без лишних действий через ADB или сторонние утилиты.

## ✨ Возможности

### Оптимизация
- **FPS boost** — trace buffer и GPU-твики для повышения частоты кадров
- **Performance mode** — MQS Enhanced Mode
- **Flagship animations** — blur-эффекты на Redmi/POCO как на топовых моделях
- **RAM и фоновые приложения** — управление LMK (Low Memory Killer)
- **Ping optimization** — троттлинг Wi-Fi сканирования для стабильного пинга в играх
- **Power tuning** — режим энергосбережения и настройка таймаутов
- **Game Turbo mode** — приоритеты CPU/GPU через Game Turbo

### Диагностика
- **Info** — подробная системная информация: чипсет, ядра, частота, GPU, температура, батарея, память
- **Benchmark (HubBench v2)** — тест CPU, памяти, накопителя и GPU по линейной шкале, без искусственного потолка
- **Predict** — предсказание, получит ли конкретная модель обновление HyperOS 4, на основе списка поддерживаемых устройств

### Прочее
- История обновлений приложения внутри самого приложения
- Поддержка нескольких языков интерфейса
- Настраиваемые анимации фона и haptic-фидбек

## 📱 Требования

- Android 10+ (тестировалось на Android 15 / HyperOS)
- Устройство Xiaomi, Redmi или POCO (часть функций специфична для HyperOS/MIUI)

## ⚠️ О сборке FOSS

Это открытая (FOSS) версия приложения — без аналитики, трекеров и закрытых модулей, присутствующих в основной сборке.

## 🛠 Технологии

Java, Android SDK (XML-разметка, классический Android UI)

## 📦 Сборка

Склонируй репозиторий и собери через Gradle или открой проект в Android Studio:

    git clone https://github.com/xiaoT164/HyperHub-foss.git
    cd HyperHub-foss
    ./gradlew assembleDebug

## 🤝 Вклад в проект

Pull request'ы и issue приветствуются. Перед крупными изменениями лучше сначала открыть issue для обсуждения.

## 📄 Лицензия

MIT License — подробности в файле [LICENSE](LICENSE).

## 💬 Контакты

🌐 Сайт: [hyperhub.dafx.ru](http://hyperhub.dafx.ru)
Telegram: [@HyperHubRu](https://t.me/HyperHubRu)

---

Если приложение оказалось полезным, можешь поддержать разработку 💛 [DonationAlerts](https://www.donationalerts.com/r/xiaot)
