# Prueba de concepto HbbTV ORB para O! (Android TV)

**Estado:** experimento aislado; no integrado en la aplicación O!, no probado en la TCL.

## Qué hace

El workflow [orb-poc.yml](../../.github/workflows/orb-poc.yml) descarga una revisión fijada de
[OpenRedButtonProject/orb](https://github.com/OpenRedButtonProject/orb),
`355d1babae3a09266ed6562f815ac8bc4ff3509b` (17-11-2026), y compila su aplicación Android
**mock203app**, con una pantalla de diagnóstico original y una **AIT y un canal simulados localmente**.

No modifica `:app`, el paquete `com.ayanabyte.one`, los datos del usuario ni el acceso a TCL TV.
La APK de laboratorio se instala como `org.orbtv.mock203app`.

Este experimento sigue la configuración Android **HbbTV 2.0.3** que ORB activa por defecto;
no implica conformidad con HbbTV 2.0.5 ni soporte de producción.

## Cómo compilar y obtener la APK

1. Abre la ejecución de **HbbTV ORB lab (isolated APK)** en Actions del PR, o ejecútala manualmente
   en GitHub cuando el flujo esté disponible en la rama predeterminada.
2. Si la compilación está en verde, descarga el artefacto **O-HbbTV-ORB-Lab-debug**.
3. Descomprime el ZIP y localiza `mock203app-debug.apk`.
4. Instálala en la TV con ADB, sin desinstalar O!:

   ```bash
   adb install -r mock203app-debug.apk
   adb shell am start -n org.orbtv.mock203app/.MainActivity
   ```

   También puede abrirse desde Aplicaciones como «O! HbbTV ORB LAB».
   Para retirarla: `adb uninstall org.orbtv.mock203app`.

## Comprobación en la TCL

1. Debe verse una pantalla negra con **O!** y «Documento HbbTV iniciado».
2. Comprueba en pantalla si el objeto OIPF fue reconocido.
3. Pulsa las flechas y OK; comprueba «Tecla recibida» y que no hay bloqueos.
4. Inspecciona los mensajes del motor y los errores:

   ```bash
   adb logcat -c
   adb shell am force-stop org.orbtv.mock203app
   adb shell am start -n org.orbtv.mock203app/.MainActivity
   adb logcat -d -s O_ORB_POC:I AndroidRuntime:E chromium:E
   ```

   Busca `O_ORB_POC: SESSION_READY`. En caso de error, adjunta las líneas de
   `AndroidRuntime` y `chromium`, sin exponer credenciales ni datos personales.

## Criterios de validación

- **Nivel 1, compilación:** workflow verde con APK realmente generada.
- **Nivel 2, Android TV:** app arranca en la TCL sin crash.
- **Nivel 3, motor:** aparece `SESSION_READY` y se carga el documento local por la AIT simulada.
- **Nivel 4, interacción:** la aplicación reconoce teclado/D-pad/OK y el objeto OIPF.
- **Nivel 5, futuro:** probar una AIT real, servicios TDT/IPTV y después LOVEStv, respetando
  permisos, certificación, DRM y acuerdos del proveedor. Nada de esto forma parte de esta APK.

Un resultado positivo en los niveles 1–4 **no** significa que LOVEStv real funcione dentro de O!.
El motor de ORB necesita puentes con el sintonizador, información del servicio y media playback.
La TV TCL analizada usa Android 11 y no expone la API pública de aplicaciones de TV interactivas
introducida en Android 13. Véase [HBBTV.md](../../docs/HBBTV.md).

## Límites técnicos / legales

- ORB upstream es Apache-2.0 pero incorpora dependencias de terceros: revisar licencias
  antes de una distribución comercial.
- El workflow usa JDK 11, Gradle 7.2, AGP 4.2.2 y NDK 21.4 porque son las versiones
  configuradas por ORB; sustituye JCenter por Maven Central *solo dentro de la copia temporal*.
  Si esas dependencias dejan de resolver, el experimento puede fallar sin afectar a O!.
- No hay sintonizador físico, IPTV, descubrimiento AIT real, caché de emisión, DRM ni sonido de TV
  en esta prueba.
- El contenido de muestra es local y no hace peticiones a los servicios de RTVE,
  Atresmedia, Mediaset o LOVEStv.
