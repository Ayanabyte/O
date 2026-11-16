# O! ONE ENTERTAINMENT

**All you love. One screen.**  
*Todo lo que te gusta. Una sola pantalla.*

Aplicación **Android TV** en **Kotlin**. Este repositorio comienza desde cero y no reutiliza código de aplicaciones anteriores.

> **Estado:** base inicial `0.1.0-alpha01`. Aún no reproduce televisión, IPTV, radio ni vídeo. El siguiente trabajo consistirá en integrar las fuentes reales y probarlas en Android TV.

## Identidad

- O blanca + punto celeste `#4DDCF5` sobre negro puro.
- Intro nativa: `O!` se transforma lentamente en la O con el punto centrado debajo. **ONE ENTERTAINMENT** aparece durante el movimiento; los eslóganes aparecen al final.
- La intro se mantiene sin audio hasta aprobar una pista definitiva de sintonización analógica, **sin campanas ni música**.
- Diseño centrado, con amplios márgenes y pensado para mando a distancia.

## Primer objetivo

Inicializar un proyecto Android TV compilable, con interfaz de arranque y navegación básica por mando, y publicar una APK de depuración en GitHub Actions. Esta base no debe confundirse con la aplicación funcional.

Consulta [`docs/ROADMAP.md`](docs/ROADMAP.md) para las fases de desarrollo y [`AGENTS.md`](AGENTS.md) para pautas de trabajo con Codex.

## Compilación

Requiere **JDK 17**, **Android SDK 36** y **Gradle 8.13**. Abre el proyecto en Android Studio; por ahora GitHub Actions proporciona Gradle al ejecutar la compilación. Para generar el wrapper en local: `gradle wrapper --gradle-version 8.13`.

La APK de prueba se publica como artefacto descargable de la acción **Android TV · Build APK** cuando la compilación finaliza correctamente.

## Aviso de marca

El nombre y el símbolo de O! son un concepto de marca en desarrollo. Comprobar disponibilidad legal antes de distribuir comercialmente.
