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

La base incluye un opening nativo sin audio, un inicio con cinco secciones y navegación con el mando. Las secciones informan de su estado hasta que se integren las fuentes reales. Atrás omite el opening, vuelve a la tarjeta seleccionada desde una sección o sale de la aplicación desde el inicio.

Consulta [`docs/ROADMAP.md`](docs/ROADMAP.md) para las fases de desarrollo y [`AGENTS.md`](AGENTS.md) para pautas de trabajo con Codex.

## Compilación

Requiere **JDK 17**, **Android SDK 36** y **Build Tools 35.0.0**. Configura `JAVA_HOME` y `ANDROID_HOME`, o abre el proyecto en Android Studio. El wrapper descarga y verifica Gradle 8.13.

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

En macOS y Linux usa `./gradlew` con las mismas tareas. La APK de depuración queda en `app/build/outputs/apk/debug/app-debug.apk`. Las compilaciones correctas de [Android TV · Build APK](https://github.com/Ayanabyte/O/actions/workflows/android.yml) ofrecen el ZIP `O-One-Entertainment-debug` en **Artifacts** de cada ejecución.

## APK para Google TV

Usa la APK firmada de [GitHub Releases](https://github.com/Ayanabyte/O/releases) en la televisión. El paquete es **`com.ayanabyte.one`** y requiere Android 8.0 (API 26) o posterior. Para actualizar conservando los datos, usa la misma firma y aumenta `versionCode` para cada nueva versión. Android usa una firma distinta para las APK de depuración.

Con el ordenador y la TV en la misma red, activa la depuración en las opciones de desarrollador del televisor y acepta la solicitud de autorización que aparezca. Usa [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) en el ordenador. Para una TCL con ADB de red en el puerto 5555:

```powershell
adb connect IP_DE_LA_TV:5555
adb devices -l
adb -s IP_DE_LA_TV:5555 install -r .\O-One-Entertainment-0.1.0-alpha01.apk
```

Si la TV muestra **Depuración inalámbrica** con código de vinculación, ejecuta `adb pair IP_DE_LA_TV:PUERTO_DE_VINCULACION` y luego `adb connect IP_DE_LA_TV:PUERTO_DE_DEPURACION`. Usa ese segundo puerto al indicar el dispositivo con `-s`. Abre **O! ONE ENTERTAINMENT** desde las aplicaciones del televisor después de ver `Success`.

## Versiones aprobadas y firma

Configura en GitHub Actions los secretos de la clave de distribución original:

- `ONE_SIGNING_KEYSTORE_BASE64` contiene el almacén PKCS#12 codificado en Base64, con el alias `one-entertainment`.
- `ONE_SIGNING_PASSWORD` contiene la contraseña del almacén.

Conserva una copia segura del almacén y de su contraseña fuera del repositorio. El flujo verifica que el certificado coincide con el de la primera APK firmada antes de preparar un borrador.

Cuando se apruebe una versión, etiqueta en `main` el commit verificado con `v` seguido de su `versionName`, por ejemplo `v0.1.0-alpha01`, y sube la etiqueta. GitHub Actions vuelve a compilar, firma la APK y crea un borrador en **Releases** con la APK y su SHA-256. Revisa el borrador y publícalo para habilitar la descarga pública.

Para preparar localmente la misma APK firmada en Windows, carga `ONE_SIGNING_PASSWORD` en el entorno desde el gestor de secretos y usa el almacén original:

```powershell
.\gradlew.bat :app:assembleRelease
$apksigner = "$env:ANDROID_HOME\build-tools\35.0.0\apksigner.bat"
& $apksigner sign --ks 'RUTA_AL_ALMACEN.p12' --ks-type PKCS12 --ks-key-alias one-entertainment `
  --ks-pass env:ONE_SIGNING_PASSWORD --out O-One-Entertainment-0.1.0-alpha01.apk `
  app/build/outputs/apk/release/app-release-unsigned.apk
& $apksigner verify --verbose --print-certs O-One-Entertainment-0.1.0-alpha01.apk
Remove-Item Env:ONE_SIGNING_PASSWORD
```

## Aviso de marca

El nombre y el símbolo de O! son un concepto de marca en desarrollo. Comprobar disponibilidad legal antes de distribuir comercialmente.
