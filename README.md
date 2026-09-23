# Binaural Beats - Android Application (v1.2 BETA)

**Binaural Beats** is a native Android application designed for meditation, concentration, sleep, and relaxation using real-time binaural beat generation and Solfeggio frequencies. Users can choose specific frequency ranges, overlay ambient soundscapes, and customize listening sessions.

---

## 🚀 Release Notes - Version 1.2 BETA

This release introduces a complete UI/UX overhaul built on Material Design 3, significant audio engine optimizations, and enhanced ambient sound controls.

### 🔄 Changelog & Evolution from Original Version (v1.0 → v1.2 BETA)

#### 🎨 1. UI & Visual Overhaul (Material 3 Dark Theme)
* **Glassmorphic Card Architecture**: Transformed the flat UI into modern `MaterialCardView` components (`#12141C` dark background with `#1E2230` elevated surfaces).
* **Dynamic Wave Category Colors**: The visualizer automatically changes its primary glow and stroke colors based on the active frequency range:
  * **Delta (1-4 Hz)**: Mystic Blue (`#448AFF`) - Deep Sleep & Healing
  * **Theta (4-8 Hz)**: Meditation Purple (`#B388FF`) - Intuition & REM
  * **Alpha (8-13 Hz)**: Emerald Green (`#00E676`) - Relaxed Focus
  * **Beta (13-30 Hz)**: Concentration Gold (`#FFD700`) - Logic & Work
  * **Gamma (30-100 Hz)**: Energy Cyan (`#00E5FF`) - Cognition
  * **Solfeggio (174-639 Hz)**: Healing Magenta (`#FF4081`) - DNA Repair & Harmony
* **Material 3 Controls**: Replaced legacy SeekBars with modern `Slider` components and upgraded dropdowns to `MaterialAutoCompleteTextView`.

#### ⚡ 2. Audio Engine & Performance Optimizations
* **Zero-IPC Visualizer Architecture**: Replaced heavy system `Intent` broadcasts per audio frame with a direct `VisualizerListener` callback via `LocalBinder`, capped at ~40 FPS to prevent main thread blocking and micro-stutters.
* **Garbage-Collector-Free Rendering**: Redesigned `VisualizerView` with pre-allocated `Path`, `Paint`, and `Shader` objects. Rendered with smooth Quadratic Bezier curves (`quadTo`) and vertical gradient fill.
* **Sine Phase Angle Correction**: Applied phase angle wrapping (`angle %= 2π`) to eliminate float precision loss and harmonic distortion in long listening sessions.
* **Independent Ambient Mixer**: Upgraded ambient tracks (**Rain & Thunder, Campfire, Forest**) to feature **individual volume sliders** and async queue management when connecting to `PlaybackService`.

---

## 🔮 Roadmap & Upcoming Features (v1.3 / v2.0)

Here are the next planned features for upcoming releases:

- [ ] **🎵 Expanded Ambient Sound Library**: Add ocean waves, night crickets, white/pink noise, and Tibetan singing bowls.
- [ ] **🎛️ Custom Frequency Generator**: Allow users to input custom Base Hz and Binaural Hz values manually.
- [ ] **⭐ Preset System**: Quick one-tap presets (e.g., *"Power Nap: Delta 2Hz + Heavy Rain"*, *"Deep Focus: Beta 18Hz + Forest"*).
- [ ] **🎧 MediaSession & Headphone Controls**: Integrate `MediaSessionCompat` for Bluetooth headphone play/pause/stop buttons and Android lockscreen media controls.
- [ ] **🌙 Sleep Fade-Out Timer Enhancements**: Customizable fade-out durations (1 min to 10 min) before stopping playback automatically.
- [ ] **📊 Listening Analytics / Streak Tracker**: Minimalist dashboard to track meditation time and daily listening streaks.

---

## ✨ Key Features

* **🎶 Frequency Selection**:
  * **Binaural Waves**: Delta (1-4 Hz), Theta (4-8 Hz), Alpha (8-13 Hz), Beta (13-30 Hz), Gamma (30-100 Hz).
  * **Solfeggio Frequencies**: 174 Hz, 285 Hz, 396 Hz, 417 Hz, 528 Hz ("Miracle / Love Frequency"), 639 Hz.
* **ℹ️ Scientific Descriptions**: Informative dialogs explaining benefits and brainwave states.
* **🍃 Ambient Sound Mixer**: Layer rain, fire, and forest sounds with individual volume controls.
* **⏱️ Session Timer**: 5m, 10m, 30m, 1h, or continuous playback with smooth 5-second fade-out.
* **⏯️ Background Service**: Foreground service with notification bar controls.

---

## 🛠️ Tech Stack

* **Language**: Kotlin 2.0
* **UI**: Material Components 3, View Binding, Custom Canvas Drawing.
* **Audio Engine**: Android `AudioTrack` for real-time PCM 16-bit stereo synthesis, `MediaPlayer` for ambient loops.
* **Architecture**: Android `Service` with `LocalBinder` and non-blocking listener callbacks.

---
