# Binaural Beats - Aplicación para Android (Versión Beta)

**Binaural Beats** es una aplicación de Android diseñada para la meditación, la concentración y la relajación mediante la reproducción de ondas binaurales y frecuencias Solfeggio. La aplicación permite a los usuarios seleccionar diferentes tipos de frecuencias según sus necesidades, combinar los sonidos con pistas de ambiente y controlar la reproducción de forma sencilla.

## 🚧 Estado del Proyecto

Esta aplicación se encuentra actualmente en **versión beta**. Continuamos desarrollando nuevas funcionalidades y realizando mejoras.

## ✨ Características Principales

*   **🎶 Selección de Frecuencias:** Ofrece dos categorías principales:
    *   **Ondas Binaurales:** Incluye Delta, Theta, Alpha, Beta y Gamma, cada una asociada a diferentes estados mentales (sueño profundo, relajación, concentración, etc.).
    *   **Frecuencias Solfeggio:** Incluye frecuencias como 174 Hz, 528 Hz ("frecuencia del amor"), 396 Hz, entre otras, conocidas por sus beneficios a nivel energético y emocional.
*   **ℹ️ Información Detallada:** Muestra una descripción completa de los beneficios y usos de cada frecuencia seleccionada.
*   **🍃 Sonidos de Ambiente:** Permite superponer sonidos relajantes de **lluvia, fuego y bosque** a la frecuencia principal, con un control de volumen independiente.
*   **⏱️ Temporizador:** Incluye opciones de temporizador para programar la duración de la sesión de escucha (5, 10, 30 minutos, 1 hora o sin límite).
*   **🌊 Visualizador de Ondas:** Muestra una representación gráfica de la forma de onda del sonido que se está reproduciendo en tiempo real.
*   **⏯️ Controles de Reproducción:** Funciones de Play/Stop y control de volumen principal.
*   **🏃‍♂️ Reproducción en Segundo Plano:** Gracias al uso de un `Service`, la reproducción continúa aunque la aplicación no esté en primer plano.

## 🛠️ Tecnologías Utilizadas

*   **Lenguaje:** Kotlin
*   **Arquitectura de UI:** View Binding para una interacción segura y eficiente con las vistas.
*   **Componentes de Android Jetpack:** `AppCompatActivity`, `Service`, `BroadcastReceiver`.
*   **Audio:** `AudioTrack` para la generación de las ondas de audio en tiempo real.

---