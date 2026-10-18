# Ticksy - Aplicación de Gestión de Tareas

Aplicación Android de gestión de tareas desarrollada con **Kotlin**, **Jetpack Compose** y **MVVM**.

## Características

### Tareas
- Listas (categorías) con color e icono propios; mantener pulsada una lista en el inicio permite reordenarla
- Crear tareas desde la lista con título, fecha (sin fecha, hoy, mañana o elegida), prioridad y repetición
- Detalle de la tarea: título editable, descripción, fecha y hora, aviso, prioridad, repetición y subtareas; todo se guarda al momento
- Subtareas: al completarlas todas, la tarea se completa sola
- Tareas recurrentes (diaria, semanal, mensual): al completarlas se crea la siguiente ocurrencia con su aviso
- Borrar deslizando, con deshacer; renombrar manteniendo pulsado
- Búsqueda global (con accesos directos a las listas) y búsqueda dentro de cada lista

### Avisos
- Un aviso por tarea, elegido respecto a su fecha (a la hora, 10 min, 1 h o 1 día antes; para tareas sin hora, ese día o el anterior a las 9:00) que se mueve con la fecha
- Recordatorios diarios de mañana y tarde con el resumen de pendientes
- Alarmas exactas cuando el sistema da el permiso (Android 14+ lo pide aparte)

### Otros
- Inicio con saludo según la hora y las tareas de hoy
- Estadísticas: tasa de finalización, racha, últimos 7 días y listas principales
- Copia de seguridad y restauración en JSON
- Widget de pantalla de inicio con las tareas pendientes
- Presentación de 3 páginas en el primer arranque
- Español e inglés; tema claro, oscuro o del sistema

## Arquitectura

- MVVM con repositorios y una capa de dominio de casos de uso (`domain/`)
- Jetpack Compose para la UI, con su propia paleta (`HomePalette`) y la fuente Plus Jakarta Sans
- Room con migraciones explícitas (base de datos v16)
- Hilt para inyección de dependencias
- StateFlow / Flow para el estado reactivo
- AlarmManager para los avisos (exactos con permiso, inexactos si no)

```
app/src/main/java/com/example/taksy/
├── data/            # Entidades Room, DAOs, AppDatabase, preferencias
├── domain/          # Casos de uso: completar, subtareas, avisos, borrar tarea/lista
├── repository/      # TaskRepository, CategoryRepository
├── viewmodel/       # Task, Category, Theme, Backup y Stats ViewModels
├── ui/
│   ├── navigation/  # NavHost y menú lateral
│   ├── screens/     # Inicio, búsqueda, lista, detalle, ajustes, estadísticas, etc.
│   ├── components/  # Diálogos, selectores de fecha/aviso, filas de tarea, etc.
│   └── theme/       # Colores, paleta y tipografía
├── service/         # Programación de alarmas, notificaciones, recordatorios diarios
├── receiver/        # ReminderReceiver (avisos y reinicio del móvil)
├── widget/          # Widget de pendientes
├── di/              # Módulo de Hilt
├── utils/           # Copia de seguridad, fechas, categorías, idioma
├── MainActivity.kt
└── TaksyApplication.kt
```

La documentación técnica detallada está en [`CLAUDE.md`](CLAUDE.md).

## Tecnologías

- **Kotlin** 2.0.21, Java 11
- **Jetpack Compose** (BOM 2024.09.00) y Material 3
- **Room** 2.6.1 con KSP
- **Hilt** 2.51.1
- **AGP** 8.10.1
- Min SDK 26 (Android 8.0), Target SDK 35 (Android 15)

## Compilación

```bash
./gradlew assembleDebug        # APK de debug
./gradlew assembleRelease      # APK de release (R8 activado)
./gradlew testDebugUnitTest    # Tests unitarios
./gradlew connectedAndroidTest # Tests instrumentados
./gradlew lint                 # Análisis de lint
```

En Windows usar `gradlew.bat` si `./gradlew` no funciona. Los esquemas de Room se exportan a `app/schemas/`.

## Tests

111 tests unitarios: casos de uso (completar tareas, avisos, fechas, repetición), repositorios, ViewModels y copia de seguridad.

## Licencia

Este proyecto está bajo la Licencia MIT.
