# Ticksy - Aplicación de Gestión de Tareas

Una aplicación Android moderna para la gestión de tareas desarrollada con **Kotlin**, **Jetpack Compose** y **MVVM**.

## 🚀 Características

### ✅ Funcionalidades Principales
- **Icono personalizado**: Icono de aplicación basado en launcher_icono.png
- **Pantalla de bienvenida**: Splash screen animado con duración extendida (5.5 segundos)
- **Gestión completa de tareas**: Crear, editar, eliminar y marcar como completadas
- **Persistencia local**: Base de datos Room para almacenar tareas
- **Filtros dinámicos**: Ver todas las tareas, solo pendientes o solo completadas
- **Interfaz moderna**: Diseño Material Design 3 con animaciones fluidas
- **Soporte multilingüe**: Español e Inglés
- **Dark Mode**: Soporte completo para tema oscuro/claro

### 🎨 Diseño y UX
- **Splash Screen Animado**: Pantalla de bienvenida con animaciones de escala, fade y pulso (5.5 segundos)
- **Icono de Aplicación**: Icono personalizado launcher_icono.png visible en el launcher del dispositivo
- **Material Design 3**: Interfaz moderna y consistente
- **Animaciones**: Transiciones suaves al añadir, editar o eliminar tareas
- **Tareas completadas**: Se muestran tachadas con estilo visual diferenciado
- **Floating Action Button**: Acceso rápido para añadir nuevas tareas
- **Filtros con chips**: Navegación intuitiva entre diferentes vistas

### 🏗️ Arquitectura
- **MVVM**: Patrón Model-View-ViewModel
- **Jetpack Compose**: UI declarativa moderna
- **Room Database**: Persistencia local robusta
- **Hilt**: Inyección de dependencias
- **StateFlow**: Manejo reactivo del estado
- **Repository Pattern**: Separación clara de responsabilidades

## 📱 Capturas de Pantalla

La aplicación incluye:
- Lista principal de tareas con filtros
- Diálogo para añadir/editar tareas
- Pantalla de configuración para tema e idioma
- Soporte completo para Dark Mode

## 🛠️ Tecnologías Utilizadas

- **Kotlin** - Lenguaje de programación
- **Jetpack Compose** - Framework de UI
- **Room Database** - Base de datos local
- **Hilt** - Inyección de dependencias
- **Material Design 3** - Sistema de diseño
- **MVVM** - Patrón de arquitectura
- **StateFlow** - Manejo de estado reactivo

## 📦 Estructura del Proyecto

```
app/src/main/java/com/example/taksy/
├── data/                    # Capa de datos
│   ├── Task.kt             # Entidad de tarea
│   ├── TaskDao.kt          # DAO para operaciones de BD
│   ├── AppDatabase.kt      # Configuración de Room
│   └── Converters.kt       # Convertidores de tipos
├── repository/             # Capa de repositorio
│   └── TaskRepository.kt   # Lógica de acceso a datos
├── viewmodel/              # Capa de ViewModel
│   ├── TaskViewModel.kt    # ViewModel principal
│   └── SplashViewModel.kt  # ViewModel para splash
├── ui/                     # Capa de UI
│   ├── screens/            # Pantallas
│   │   ├── SplashScreen.kt # Pantalla de bienvenida
│   │   └── TaskScreen.kt   # Pantalla principal
│   ├── components/         # Componentes reutilizables
│   │   ├── TaskItem.kt     # Item de tarea
│   │   ├── AddEditTaskDialog.kt # Diálogo de tarea
│   │   └── TaskFilterChips.kt # Filtros
│   └── theme/              # Temas y estilos
└── TaksyApplication.kt     # Clase Application
```

## 🚀 Instalación y Uso

### Requisitos
- Android Studio Hedgehog o superior
- SDK mínimo: API 26 (Android 8.0)
- SDK objetivo: API 35 (Android 15)

### Pasos de instalación
1. Clona el repositorio
2. Abre el proyecto en Android Studio
3. Sincroniza las dependencias de Gradle
4. Ejecuta la aplicación en un dispositivo o emulador

### Uso de la aplicación
1. **Añadir tarea**: Toca el botón flotante "+" y completa el formulario
2. **Editar tarea**: Toca el icono de editar en cualquier tarea
3. **Marcar como completada**: Usa el checkbox o toca la tarea
4. **Eliminar tarea**: Toca el icono de eliminar
5. **Filtrar tareas**: Usa los chips de filtro en la parte superior
6. **Configuración**: Accede a la pantalla de configuración para cambiar tema e idioma

## 🌟 Características Destacadas

### Persistencia Robusta
- Base de datos Room con entidades bien definidas
- Operaciones asíncronas con corrutinas
- Manejo de errores y estados de carga

### UI/UX Moderna
- Diseño Material Design 3
- Animaciones fluidas y transiciones suaves
- Soporte completo para Dark Mode
- Interfaz responsive y accesible

### Arquitectura Limpia
- Separación clara de responsabilidades
- Código modular y mantenible
- Patrones de diseño bien implementados
- Inyección de dependencias con Hilt

### Multilingüe
- Soporte para Español e Inglés
- Cambio de idioma dinámico
- Strings externalizados y organizados

## 🔧 Configuración de Desarrollo

### Dependencias principales
- `androidx.compose.bom:2024.09.00`
- `androidx.room:2.6.1`
- `com.google.dagger.hilt.android:2.48`
- `androidx.lifecycle:2.9.2`

### Configuración de compilación
- Kotlin 2.0.21
- Compose Compiler 1.5.8
- Target SDK 35
- Min SDK 26

## 📝 Notas de Desarrollo

- La aplicación sigue las mejores prácticas de Android
- Código comentado y documentado
- Estructura modular para fácil mantenimiento
- Preparada para futuras extensiones

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:
1. Fork el proyecto
2. Crea una rama para tu feature
3. Commit tus cambios
4. Push a la rama
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

---

**Ticksy** - Una aplicación de gestión de tareas moderna, elegante y funcional. 🎯

