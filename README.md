# Ticksy - Aplicación de Gestión de Tareas

Una aplicación Android moderna para la gestión de tareas desarrollada con **Kotlin**, **Jetpack Compose** y **MVVM**.

## Características

### Funcionalidades principales
- Gestión completa de tareas: crear, editar, eliminar, completar y archivar
- Organización por categorías con colores e iconos personalizables
- Subtareas con auto-completado del padre cuando todas se completan
- Tareas recurrentes (diaria, semanal, mensual, anual): se regeneran al completarse
- Drag & drop para reordenar tareas y categorías
- Búsqueda global y por categoría
- Recordatorios puntuales por tarea y recordatorios diarios (mañana/tarde)
- Widget de pantalla de inicio con las tareas pendientes
- Dashboard de estadísticas: totales, tasa de finalización, racha, completadas de los últimos 7 días y categorías principales
- Backup y restauración de todos los datos a JSON
- Onboarding de 3 pantallas en el primer arranque
- Soporte multilingüe: español e inglés
- Tema claro y oscuro

### Diseño y UX
- Material Design 3
- Splash screen animado (2.5 s)
- Filtros con chips y navegación por drawer lateral
- Swipe-to-archive y swipe-to-delete con deshacer

### Arquitectura
- MVVM con Repository Pattern
- Jetpack Compose para la UI
- Room Database con migraciones explícitas (v14)
- Hilt para inyección de dependencias
- StateFlow y Flow para estado reactivo
- AlarmManager con `setExactAndAllowWhileIdle` para recordatorios

## Tecnologías

- **Kotlin** 2.0.21
- **Jetpack Compose** (BOM 2024.09.00)
- **Room** 2.6.1 (con KSP)
- **Hilt** 2.48
- **Material Design 3**
- **AppCompat / DrawerLayout** para el menú lateral

## Estructura del proyecto

```
app/src/main/java/com/example/taksy/
├── data/            # Entidades Room, DAOs, AppDatabase, Converters
├── repository/      # TaskRepository, CategoryRepository
├── viewmodel/       # TaskViewModel, CategoryViewModel, ThemeViewModel,
│                    # SplashViewModel, BackupViewModel, StatsViewModel,
│                    # ReminderViewModel
├── ui/
│   ├── screens/     # CategoryList, TasksByCategory, TaskDetail,
│   │                # Reminders, DailyReminder, ThemeSettings,
│   │                # LanguageSettings, About, Backup, Stats,
│   │                # Onboarding, Splash
│   ├── components/  # Diálogos, items de lista, chips, etc.
│   └── theme/       # Color, Theme, Type
├── service/         # ReminderScheduler(+Contract), DailyReminderService,
│                    # NotificationService
├── receiver/        # ReminderReceiver
├── widget/          # TaskWidgetProvider, TaskWidgetService
├── di/              # DatabaseModule (Hilt)
├── utils/           # BackupManager, LocaleHelper, DateUtils,
│                    # CategoryUtils, DeviceUtils
└── TaksyApplication.kt
```

## Instalación

### Requisitos
- Android Studio Hedgehog o superior
- Min SDK: 26 (Android 8.0)
- Target SDK: 35 (Android 15)
- JDK 11

### Pasos
1. Clona el repositorio
2. Abre el proyecto en Android Studio
3. Sincroniza las dependencias de Gradle
4. Ejecuta en un dispositivo o emulador

### Comandos Gradle

```bash
./gradlew assembleDebug        # APK de debug
./gradlew assembleRelease      # APK de release
./gradlew testDebugUnitTest    # Tests unitarios
./gradlew connectedAndroidTest # Tests instrumentados
./gradlew lint                 # Análisis de lint
```

En Windows usar `gradlew.bat` si `./gradlew` no funciona.

## Uso

1. **Añadir tarea**: pulsa el campo de entrada inline en una categoría
2. **Editar / añadir subtareas**: toca la tarea para abrir el detalle
3. **Completar**: marca el checkbox; las recurrentes se regeneran automáticamente
4. **Archivar**: swipe de izquierda a derecha
5. **Eliminar**: swipe de derecha a izquierda (con opción de deshacer)
6. **Reordenar**: mantén pulsado y arrastra
7. **Recordatorios**: configúralos en el detalle de la tarea o desde el drawer
8. **Estadísticas, copia de seguridad, tema e idioma**: desde el menú lateral

## Configuración de compilación

- Kotlin 2.0.21, Java 11
- Compose BOM 2024.09.00
- Room 2.6.1 + KSP
- Hilt 2.48
- ProGuard/R8 habilitado en release
- Esquemas de Room exportados a `app/schemas/`

## Tests

74 tests unitarios cubriendo `BackupManager`, `TaskViewModel`, `TaskRepository` y `CategoryRepository`.

## Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.
