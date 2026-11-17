# Televisión digital terrestre

## Hardware y acceso observados

- TCL `Smart TV`, dispositivo `G07`, producto `G07_4K_GB_NF`. Android 11 (API 30), compilación `RP1A.200622.001`, firmware identificado por el fingerprint `AR20`, propiedad incremental `AR07`, parche de seguridad `2025-12-05`.
- `TvInputManager` registra la entrada conectada `com.tcl.tvinput/.TDTVInputService/HW1413742848`. `com.tcl.tvinput` (`a2d5058.275`) publica `TDTVInputService` y `TvPassThroughService`. Este último expone la entrada compuesta `HW1413743104` y tres HDMI (`HW1413744128`, `HW1413744384`, `HW1413744640`). El gestor también enumera cinco entradas de software de Google TV, VLC, Pluto TV y el servicio IPTV de TCL. `com.tcl.tv` (`2.05.07_5ea8bb8_240719`) es la aplicación de TV del fabricante.
- `TvContract` consultado por el usuario de ADB devolvió cero filas. El permiso de ejecución `android.permission.READ_TV_LISTINGS`, declarado `dangerous` en la TCL, permitió que la consulta de O! leyera el catálogo desde su propia identidad.
- La consulta de diagnóstico encontró 104 filas navegables de tipo `TYPE_DVB_T`. La columna pública `COLUMN_SERVICE_TYPE` identifica 57 como `EN_TCL_DTV`, 38 como `EN_TCL_RADIO` y 9 como `EN_TCL_ATV`. O! clasifica esos valores únicamente para entradas de `com.tcl.tvinput`. Para otros proveedores usa `SERVICE_TYPE_AUDIO` y los tipos terrestres estándar. Las filas analógicas, de cable, satélite y red quedan fuera de la lista TDT.
- El proveedor no devolvió datos de logo en `TvContract.buildChannelLogoUri()` para los 95 servicios digitales de antena de esta TCL. O! muestra nombre y número y utiliza el logo si el proveedor lo ofrece.
- La lectura de toda la base requiere `com.android.providers.tv.permission.ACCESS_ALL_EPG_DATA` (`signature|privileged`). El acceso directo al hardware con `android.permission.TV_INPUT_HARDWARE` o `android.permission.ACCESS_TV_TUNER` también es privilegiado. O! usa las filas que el proveedor le deja leer y solicita sesiones mediante `TvView`.
- Si otro firmware oculta las filas o rechaza una sesión de terceros, la alternativa disponible al usuario es la aplicación de TV del fabricante. La activación de canales y el escaneo se gestionan allí. La TCL resuelve `ACTION_VIEW` con una URI de canal y el MIME `vnd.android.cursor.item/channel` a la aplicación TCL y a otra actividad de TV del sistema.

## Validación de reproducción

- El sintonizador notificó `onVideoAvailable` y seleccionó una pista de audio en 24h HD (1), 3CatInfo (2), A3 UHD (3) y Antena 3 (5). `SurfaceFlinger` registró composición `SIDEBAND` en la superficie de O!. `screencap` devolvió un archivo de cero bytes durante la reproducción. La imagen en el panel y el sonido audible requieren una comprobación presencial.
- En **Radio TDT**, se sintonizaron 3CatInfo (1001) y 95.2 Terrassa (1002) y se registró la selección de una pista de audio en ambas. La segunda se volvió a sintonizar al recuperar O! desde el lanzador.
- Se comprobó mediante eventos del mando por ADB el cambio con el D-pad y Canal +/−, la vuelta a la lista con el foco en el último servicio y la entrada posterior. `dumpsys tv_input` mostró una sesión durante la reproducción y cero después de Atrás y de enviar O! al fondo con Home.
- Los recorridos anteriores se completaron sin errores de sesión ni cierres de O! en logcat. La recepción débil, la pérdida del sintonizador y el bloqueo parental tienen estados en la interfaz. Esas condiciones quedan pendientes de verificar físicamente.

Referencias [TvInputInfo](https://developer.android.com/reference/android/media/tv/TvInputInfo), [TvContract](https://developer.android.com/reference/android/media/tv/TvContract), [TvView](https://developer.android.com/reference/android/media/tv/TvView) y [TV Input Framework](https://source.android.com/docs/devices/tv).
