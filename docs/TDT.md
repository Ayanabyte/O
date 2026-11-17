# Televisión digital terrestre

## Hardware y acceso observados

- TCL `Smart TV`, dispositivo `G07`, producto `G07_4K_GB_NF`. Android 11 (API 30), compilación `RP1A.200622.001`, firmware identificado por el fingerprint `AR20`, propiedad incremental `AR07`, parche de seguridad `2025-12-05`.
- `TvInputManager` registra la entrada conectada `com.tcl.tvinput/.TDTVInputService/HW1413742848`. `com.tcl.tvinput` (`a2d5058.275`) publica `TDTVInputService` y `TvPassThroughService`. El segundo servicio representa conexiones externas. `com.tcl.tv` (`2.05.07_5ea8bb8_240719`) es la aplicación de TV del fabricante.
- `TvContract` consultado por el usuario de ADB devolvió cero filas. Logcat del fabricante confirmó que la entrada TDT tiene filas DVB-T. La consulta de O! se valida por separado con el permiso de ejecución `android.permission.READ_TV_LISTINGS`, que el proveedor de la TCL declara `dangerous`.
- La lectura de toda la base requiere `com.android.providers.tv.permission.ACCESS_ALL_EPG_DATA` (`signature|privileged`). El acceso directo al hardware con `android.permission.TV_INPUT_HARDWARE` o `android.permission.ACCESS_TV_TUNER` también es privilegiado. O! usa las filas que el proveedor le deja leer y solicita sesiones mediante `TvView`.
- Si otro firmware oculta las filas o rechaza una sesión de terceros, la alternativa disponible al usuario es la aplicación de TV del fabricante. La activación de canales y el escaneo se gestionan allí.

Referencias [TvInputInfo](https://developer.android.com/reference/android/media/tv/TvInputInfo), [TvContract](https://developer.android.com/reference/android/media/tv/TvContract), [TvView](https://developer.android.com/reference/android/media/tv/TvView) y [TV Input Framework](https://source.android.com/docs/devices/tv).
