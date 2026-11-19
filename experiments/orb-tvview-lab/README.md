# O! LAB — Prueba TDT TCL con TvView

Esta carpeta contiene una actividad Java independiente para comprobar si el laboratorio ORB `org.orbtv.mock203app` puede crear su propia sesión TIF y sintonizar La 1 HD.

## Identificadores de la TCL

- Entrada: `com.tcl.tvinput/.TDTVInputService/HW1413742848`
- Canal: `content://android.media.tv/channel/10032330`
- La aplicación principal `com.ayanabyte.one` no se modifica.

## Instalar en el código ORB existente (portátil con Codex)

Descarga esta carpeta y ejecuta desde ella:

```sh
python3 apply_to_orb.py /ruta/al/repositorio/ORB
```

El script comprueba que existe `android/mock203app` y modifica únicamente ese módulo: añade `TdtTvViewTestActivity.java` y la declaración de actividad al `AndroidManifest.xml`. No edita `MainActivity.java`, ni archivos de O!, ni firmware, ni canales. Revisa el diff antes de compilar.

Compila con el mismo proyecto y la misma firma que utiliza Codex para la APK ORB instalada. No hay APK compilada en esta rama.

## Pruebas con Bugjaeger después de instalar la APK actualizada

```sh
am start -n org.orbtv.mock203app/.TdtTvViewTestActivity
```

```sh
logcat -d -t 2500 -v brief | grep O_TDT_TVVIEW | tail -n 50
```

```sh
dumpsys tv_input | sed -n '/sessionStateMap: ITvInputSession/,/mainSessionToken:/p'
```

Los eventos `onVideoAvailable`, `onVideoUnavailable`, `onConnectionFailed`, `onDisconnected` y `onTuned` indican el resultado. OK reintenta; AZUL confirma que se ha recibido la tecla pero no activa HbbTV; ATRÁS sale y libera la sesión.

**Limitación:** esta prueba solo verifica TDT por TvView. No integra todavía el motor HbbTV ni LOVEStv. No se ha ejecutado aún sobre la TCL; el código y el instalador se han revisado sin disponer de la copia local de ORB ni de un Android SDK.
