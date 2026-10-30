# Сохраняем классы Yandex MapKit (иначе карта вылетит)
-keep class com.yandex.mapkit.** { *; }
-keep class com.yandex.runtime.** { *; }
-keep class com.yandex.maps.** { *; }
-dontwarn com.yandex.**

# Сохраняем классы для работы с медиа (Media3 ExoPlayer)
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Сохраняем наши сервисы и ресиверы (чтобы система их не убила при сжатии)
-keep class com.haval.m6launcher.BackAccessibilityService { *; }
-keep class com.haval.m6launcher.MediaControlService { *; }
-keep class com.haval.m6launcher.BootReceiver { *; }

# Сохраняем модели данных
-keep class com.haval.m6launcher.data.** { *; }

# Игнорируем предупреждения библиотек
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**