package com.pocotech.hub;

public class BenchmarkResult {

    private boolean isEnglish() {
        return java.util.Locale.getDefault().getLanguage().toLowerCase(java.util.Locale.US).startsWith("en");
    }

    public int totalScore      = 0;
    public int cpuSingleScore  = 0;
    public int cpuMultiScore   = 0;
    public int ramScore        = 0;
    public int storageScore    = 0;
    public int gpuScore        = 0;

    public int   cpuCores      = 0;
    public long  cpuSingleRaw  = 0; // усл. MiB/s вычислительной нагрузки
    public long  cpuMultiRaw   = 0; // усл. MiB/s вычислительной нагрузки
    public long  ramBandwidth  = 0; // MiB/s
    public long  storageWrite  = 0; // MiB/s
    public long  storageRead   = 0; // MiB/s

    public float cpuSingleVariation = 0f;
    public float cpuMultiVariation  = 0f;
    public float ramVariation       = 0f;
    public float storageVariation   = 0f;
    public int benchmarkConfidence  = 0;

    public boolean benchmarkValid = true;
    public String failureMessage = null;
    public float gpuAvgFps      = 0f;
    public float gpuDropPercent = 0f;
    public boolean gpuTested    = false;

    public void applyGpuResult(int gpuScoreValue, float avgFps, float dropPercent) {
        this.gpuScore = gpuScoreValue;
        this.gpuAvgFps = avgFps;
        this.gpuDropPercent = dropPercent;
        this.gpuTested = true;
        this.totalScore = cpuSingleScore + cpuMultiScore + ramScore + storageScore + gpuScore;
    }

    public void markFailure(String message) {
        this.benchmarkValid = false;
        this.failureMessage = message;
    }

    public float getWorstSectionVariation() {
        return Math.max(
            Math.max(cpuSingleVariation, cpuMultiVariation),
            Math.max(ramVariation, storageVariation)
        );
    }

    public String getConfidenceLabel() {
        float worst = getWorstSectionVariation();
        if (isEnglish()) {
            if (benchmarkConfidence >= 90 && worst <= 4.5f) return "Very high";
            if (benchmarkConfidence >= 78 && worst <= 8.5f) return "Normal";
            return "Background-sensitive";
        }
        if (benchmarkConfidence >= 90 && worst <= 4.5f) return "Очень высокая";
        if (benchmarkConfidence >= 78 && worst <= 8.5f) return "Нормальная";
        return "Чувствителен к фону";
    }

    public String getMethodologySummary() {
        return isEnglish()
                ? "6 CPU windows, 7 RAM windows, 5 I/O runs, median with variance control"
                : "6 окон CPU, 7 окон RAM, 5 прогонов I/O, медиана и контроль разброса";
    }

    public String getGpuStability() {
        if (!gpuTested) return "";
        if (isEnglish()) {
            if (gpuDropPercent < 8f) return "Very stable";
            if (gpuDropPercent < 18f) return "Moderate drop";
            if (gpuDropPercent < 30f) return "Heat-sensitive";
            return "Heavy peak throttling";
        }
        if (gpuDropPercent < 8f) return "Очень стабильный";
        if (gpuDropPercent < 18f) return "Умеренная просадка";
        if (gpuDropPercent < 30f) return "Чувствителен к нагреву";
        return "Сильная просадка под пиком";
    }

    
    public String getRating() {
        if (isEnglish()) {
            if (totalScore >= 380000) return "Flagship / near-flagship";
            if (totalScore >= 220000)  return "Very powerful";
            if (totalScore >= 120000)  return "Strong mid-range";
            if (totalScore >= 60000)  return "Budget / basic";
            return "Entry level";
        }
        if (totalScore >= 380000) return "Флагман / почти флагман";
        if (totalScore >= 220000)  return "Очень мощный";
        if (totalScore >= 120000)  return "Крепкий средний класс";
        if (totalScore >= 60000)  return "Бюджетный / базовый";
        return "Начальный уровень";
    }

    public String getCapabilities() {
        if (isEnglish()) {
            if (totalScore >= 100000) {
                return "Heavy games on high settings — confidently\n4K editing and multitasking — comfortable\nEnough headroom for several years";
            } else if (totalScore >= 70000) {
                return "Games and emulators — very good\n1080p/1440p editing — comfortable\nThe system stays responsive under load";
            } else if (totalScore >= 45000) {
                return "Games on medium-high settings — fine\n1080p editing — manageable\nDaily multitasking — good";
            } else if (totalScore >= 22000) {
                return "Social apps, browser, messengers — excellent\nGames — mostly light titles or low settings\nLong heavy loads may bring visible limits";
            }
            return "Basic tasks — okay\nGames — simple titles only\nFewer background processes and heavy apps are recommended";
        }
        if (totalScore >= 100000) {
            return "Тяжёлые игры на высоких настройках — уверенно\nМонтаж 4K и многозадачность — комфортно\nЗапас по мощности на несколько лет";
        } else if (totalScore >= 70000) {
            return "Игры и эмуляторы — очень хорошо\nМонтаж 1080p/1440p — комфортно\nСистема останется отзывчивой даже под нагрузкой";
        } else if (totalScore >= 45000) {
            return "Игры на средне-высоких настройках — нормально\nМонтаж 1080p — без боли\nПовседневная многозадачность — хорошая";
        } else if (totalScore >= 22000) {
            return "Соцсети, браузер, мессенджеры — отлично\nИгры — в основном лёгкие или на низких настройках\nПод длительной нагрузкой возможны заметные ограничения";
        } else {
            return "Базовые задачи — нормально\nИгры — только простые\nЖелательно меньше фоновых процессов и тяжёлых приложений";
        }
    }
}

