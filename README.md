# SUNO AI Architect

**Version:** 0.1 alpha  
**Developer:** ViBy Production  
**Author:** Vitalii Bychkov  

Элитный Android-клиент для генерации оптимизированных Style + Lyrics промптов для Suno AI.

## Возможности

- Текстовое описание идеи → Clean Block (Analysis + Style + Lyrics)
- Загрузка аудио / видео с устройства, Google Drive и других источников (SAF)
- Автоматическое извлечение аудиодорожки из видео
- Режимы: Style Cloning, Quality, Hybrid Genres, Advanced Voice, Instrumental, MAX MODE
- Выбор модели: **Gemini 3.6 Lite** / **Gemini 3.1 Pro**
- Целевая версия Suno: v5 / v4.5 / v4
- Полностью клиентское приложение (API-ключ хранится локально)

## Требования

- Android 8.0+ (API 26)
- Gemini API Key (получить на https://aistudio.google.com)

## Как открыть проект

1. Установите Android Studio (Ladybug / latest)
2. File → Open → выберите папку `SunoAIArchitect`
3. Дождитесь синхронизации Gradle
4. Добавьте иконки приложения в `app/src/main/res/mipmap-*` (или временно используйте стандартные)
5. Запустите на эмуляторе / устройстве

## Структура

```
com.vibyproduction.sunoaiarchitect
├── data/          # SettingsRepository, GeminiRepository
├── domain/        # Models, enums
├── ui/
│   ├── screens/   # MainScreen, MainViewModel
│   ├── theme/     # Colors, Typography, Theme
│   └── components/
└── util/          # AudioExtractor
```

## Важно

- Названия моделей Gemini (`gemini-3.6-flash-lite`, `gemini-3.1-pro`) — плейсхолдеры.  
  Замените на актуальные ID моделей в `domain/Models.kt` после проверки в Google AI Studio.
- Полноценная передача аудио-файла в Gemini зависит от поддержки multimodal в используемой версии Android SDK `generativeai`.  
  В текущей реализации при необходимости используется текстовый fallback + сильный system prompt.
- Для продакшена рекомендуется добавить Files API Gemini для больших аудио.

## Лицензия

Proprietary — ViBy Production / Vitalii Bychkov  
© 2026
