# Binaural Beats - Aplicación para Android (v1.2 BETA)

**Binaural Beats** es una aplicación de Android diseñada para la meditación, la concentración y la relajación mediante la generación en tiempo real de ondas binaurales y frecuencias Solfeggio. Permite a los usuarios seleccionar diferentes frecuencias según sus necesidades cognitivas o de descanso, combinar los tonos con sonidos ambientales relajantes y personalizar cada sesión.

---

## 🚀 Novedades en la versión 1.2 BETA

* 🎨 **Rediseño UI en Material 3 (Glassmorphic Dark Theme)**: Nueva interfaz estructurada en tarjetas elegantes (`MaterialCardView`) con tonos oscuros místicos (`#12141C` y `#1E2230`).
* 🌈 **Colores Dinámicos por Tipo de Onda**: El visualizador cambia de color automáticamente según la categoría seleccionada (Delta, Theta, Alpha, Beta, Gamma o Solfeggio).
* 🌊 **Visualizador Ultra-Fluido a 60 FPS**: Renderizado de curvas Bézier suavizadas con relleno degradado sin sobrecargar la memoria de la UI (0 GC allocations en `onDraw`).
* 🎚️ **Mezclador Ambiental Pro con Volúmenes Independientes**: Control deslizante (`Slider`) de volumen individual para **Lluvia, Fuego y Bosque**.
* ⚡ **Arquitectura de Audio Optimizada**: Comunicación de alta velocidad entre el `PlaybackService` y la interfaz mediante callbacks directos (IPC-free `VisualizerListener`) y corrección de fase armónica.
* 📱 **Nuevos Desplegables Material 3**: Eliminación del bloqueo de filtrado en menús emergentes (`MaterialAutoCompleteTextView`).

---

## ✨ Características Principales

* **🎶 Selección de Frecuencias**:
  * **Ondas Binaurales**: Delta (1-4 Hz), Theta (4-8 Hz), Alpha (8-13 Hz), Beta (13-30 Hz) y Gamma (30-100 Hz).
  * **Frecuencias Solfeggio**: 174 Hz, 285 Hz, 396 Hz, 417 Hz, 528 Hz ("Frecuencia del Amor") y 639 Hz.
* **ℹ️ Información Detallada**: Diálogo explicativo con beneficios científicos y de relajación para cada frecuencia.
* **🍃 Mezclador de Ambiente**: Pistas relajantes superponibles de lluvia/truenos, fogata y bosque con regulación independiente.
* **⏱️ Temporizador con Fundido Suave**: Opciones de sesión de 5m, 10m, 30m, 1h o sin límite con *Fade-Out* progresivo de 5 segundos.
* **⏯️ Reproducción en Segundo Plano**: Servicio de primer plano (`Foreground Service`) persistente que continúa funcionando al apagar la pantalla o minimizar la app.

---

## 🛠️ Tecnologías Utilizadas

* **Lenguaje**: Kotlin 2.0
* **UI**: Material Components 3, View Binding, Custom Vector Graphics.
* **Audio Engine**: Android `AudioTrack` para síntesis en tiempo real de ondas senoidales estéreo, `MediaPlayer` para ambiente.
* **Arquitectura**: `Service` en primer plano con enlace local (`Binder`) e IPC optimizado.

---
