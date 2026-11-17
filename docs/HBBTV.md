# LOVEStv y HbbTV en la TCL G07

## Historial verificado

- **5.3.17** (`85e6dec`) generaba su variante de 32 bits en CI aplicando un paquete de fuentes y parches. Esperaba a que `TvView` recibiera vídeo de antena, inicializaba el motor HbbTV y el navegador de TCL en la actividad del reproductor y registraba los eventos de difusión para que el canal presentara su propia aplicación. El botón azul de Android (`KEYCODE_PROG_BLUE`, 186) se traducía al mapa de teclas TCL y se pasaba al motor. [Versión 5.3.17](https://github.com/Ayanabyte/Channelplus/releases/tag/v5.3.17)
- El cambio `8931097` documenta el fallo de reapertura previo. Ocultar el navegador dejaba activo el motor con el estado de la aplicación anterior. Tras Atrás o el retorno al directo, se desvinculaba el servicio HbbTV para terminar la página y el motor, se devolvía el foco al reproductor y se volvía a vincular el servicio 1,8 s después. La sintonización TDT se conservaba. [Cambio de reapertura](https://github.com/Ayanabyte/Channelplus/commit/8931097b0b12bb863b9f842286af5c355b46b267)
- **5.3.18** (`a0df7d5`) interceptaba Atrás antes del navegador mientras HbbTV tenía el foco, consumía también la liberación de la tecla durante el reinicio y bloqueaba el foco de los descendientes del navegador hasta recibir Azul. GitHub Actions generó una APK candidata. La publicación falló y la referencia consultada carece de etiqueta `v5.3.18`. [Cambio de foco](https://github.com/Ayanabyte/Channelplus/commit/a0df7d5) · [Compilación](https://github.com/Ayanabyte/Channelplus/actions/runs/42090958915) · [Publicación](https://github.com/Ayanabyte/Channelplus/actions/runs/42090959051)
- **5.3.19** (`f7dd2a2`) volvió a generar el comportamiento de 5.3.17. La compilación de la reversión terminó en verde. El historial identifica el conjunto de cambios revertido. Para atribuir la regresión a un cambio concreto de foco o de Atrás haría falta reproducir las APK históricas en igualdad de condiciones. [Commit de reversión](https://github.com/Ayanabyte/Channelplus/commit/f7dd2a210ab56768518c636edb97590531768927) · [Compilación](https://github.com/Ayanabyte/Channelplus/actions/runs/42094544107)

## Límite de una APK normal

La TCL inspeccionada responde como `Smart TV` (`G07_4K_GB_NF`), Android 11/API 30, compilación `RP1A.200622.001`, fingerprint `AR20` y propiedad incremental `AR07`. El sistema registra `com.tcl.tv/.TVActivity`, `com.tcl.tvinput/.TDTVInputService/HW1413742848` y las bibliotecas del fabricante `com.tcl.hbbtv.HbbtvServer`, `com.tcl.tbrowser` y `tcl.webkit`. `com.tcl.tv` y `com.tcl.tvinput` tienen concedidos los permisos privilegiados de hardware y acceso completo al proveedor de TV.

`TvView` de API 30 sintoniza y presenta audio y vídeo con la entrada TDT. El firmware carece del servicio de sistema `tv_interactive_app` y de servicios `TvInteractiveAppService` resolubles. Android agregó `TvInteractiveAppManager`, `TvInteractiveAppView` y la notificación pública de AIT desde `TvInputService.Session` en API 33. Incluso con API 33 o superior la integración depende de que el fabricante ofrezca un servicio interactivo compatible. [API de TvInteractiveAppView](https://developer.android.com/reference/android/media/tv/interactive/TvInteractiveAppView) · [API de TvInputService.Session](https://developer.android.com/reference/android/media/tv/TvInputService.Session)

La inspección estática del artefacto de fuentes y de su biblioteca de ejecución confirma que la versión histórica llamaba al registro privado `systemAuthorized` del SDK TCL y accedía a métodos ocultos de su navegador. Incluía excepciones a la política de APIs ocultas y adaptaciones nativas para ese firmware. La presencia de una biblioteca en la lista de Android no concede la autorización del fabricante. O! mantiene su reproductor `TvView` y su permiso normal `READ_TV_LISTINGS`. La integración embebida queda pendiente de un acceso admitido por TCL o de un servicio público compatible. [Flujo histórico de compilación](https://github.com/Ayanabyte/Channelplus/blob/v5.3.17/.github/workflows/channelplus-537-lovestv.yml)

## Emisiones probadas en la app nativa

La entrada se abrió con `Intent.ACTION_VIEW`, la URI del canal de `TvContract` y el MIME `vnd.android.cursor.item/channel`. Tras sintonizar, se envió `KEYCODE_PROG_BLUE` por ADB. Logcat identificó el canal, el resultado de la tecla y la carga en el motor HbbTV de TCL.

| Grupo | Canal registrado | Resultado tras Azul |
| --- | --- | --- |
| RTVE | La 1 HD (24) | Se cargó el botón rojo de RTVE. TCL registró Azul como no atendido en ese intento. |
| RTVE | La 1 UHD (25) y La 2 HD (26) | Azul fue atendido y terminó la carga de la aplicación real de LOVEStv. |
| Atresmedia | Antena 3 (5) | Azul fue atendido. El acceso de Atresmedia cargó la aplicación real de LOVEStv. |
| Mediaset España | Telecinco (41) | Azul fue atendido. El acceso de Mediaset cargó la aplicación real de LOVEStv. Su navegador atendió las flechas izquierda y derecha. |

La disponibilidad observada depende de lo que señala cada canal. [Manual de LOVEStv](https://www.lovestv.es/manual-usuario/)

## Comparación directa con `TvView`

Una APK de diagnóstico temporal ejecutó `TvView` con una identidad de aplicación normal y `READ_TV_LISTINGS`. En Antena 3 recibió vídeo disponible y seleccionó una pista de audio. Con el foco puesto en `TvView`, se enviaron las dos fases de Azul a la sesión. `TvView.dispatchKeyEvent()` aceptó el envío, pero `OnUnhandledInputEventListener` recibió de vuelta tanto `ACTION_DOWN` como `ACTION_UP`. El servicio interactivo de Android devolvió `null`. El resultado separa la ruta de la tecla en Android de la capacidad de abrir LOVEStv.

La misma APK abrió Antena 3 mediante el `ACTION_VIEW` exportado de `com.tcl.tv` y liberó su `TvView` al perder el primer plano. Con `TVActivity` en primer plano, Azul quedó atendido y el navegador de TCL registró `TOS_TBROWSER_CBEVENT_TYPE_LOAD_PAGE_END` para la aplicación principal de LOVEStv. En la prueba de regreso, TCL siguió en primer plano tras Rojo y tres pulsaciones de Atrás. Al volver a abrir la APK de diagnóstico se seleccionó una pista de audio del canal. La recuperación visual quedó sin confirmar en esa prueba. La APK de diagnóstico se retiró y O! quedó en primer plano con cero sesiones TDT.

## Estabilidad observada

| Prueba | Evidencia en la TCL |
| --- | --- |
| A y D. Cerrar y repetir en el mismo canal | En Antena 3, el mismo proceso nativo atendió tres pulsaciones de Azul intercaladas con Rojo y Atrás. La primera carga quedó confirmada en logcat. Los eventos de tecla por sí solos no confirman el cierre visual ni la reapertura en los dos intentos posteriores. |
| B. Cambiar de canal y volver a abrir | La app TCL completó cargas de LOVEStv en Antena 3, Telecinco y de nuevo Antena 3 tras los cambios de canal. |
| C. Ver desde el inicio y recuperar el directo | La app TCL atendió Amarillo en Antena 3. El estado registrado seguía siendo `HBBTV_ON_NO_VOD`; quedaron pendientes el inicio del contenido y la recuperación de imagen y sonido tras ese contenido. |
| E. Volver a Inicio y entrar de nuevo | En O! se verificaron Antena 3, el cambio a A3Series, Atrás hasta Inicio y la reentrada para sintonizar 3CatInfo de Radio TDT. |
| F. Mando y sesiones | En O! funcionaron el cambio de canal después de Azul y la navegación a radio. `dumpsys tv_input` mostró cero sesiones tras salir de los reproductores de TV y radio. El estado interno de las capas HbbTV de TCL carece de una interfaz pública de inspección en este firmware. |

`screencap` en el reproductor nativo devolvió un error durante LOVEStv. Las pruebas A–D del flujo embebido en O! quedan pendientes de acceso a un motor HbbTV compatible. Se mantuvo instalada la versión TDT `0.2.0-alpha01`.

## Vías legítimas

- Usar LOVEStv en la aplicación TV de TCL. El flujo verificado permite abrir el canal desde otra aplicación con `ACTION_VIEW` y pulsar Azul cuando TCL está en primer plano. Es una derivación al reproductor nativo.
- En un televisor de pruebas sin tecla Azul, `adb shell input keyevent KEYCODE_PROG_BLUE` permite enviarla con la TV de TCL en primer plano. Una APK ordinaria carece de `android.permission.INJECT_EVENTS`, que el firmware declara `signature`, para enviar esa tecla a la ventana de otra aplicación. [Código de Android sobre inyección de eventos](https://android.googlesource.com/platform/frameworks/base/+/c7efcde0a81e74f63cff2072992cd500fe1fcf60/services/core/java/com/android/server/input/InputManagerService.java)
- Para integrarlo en el reproductor de O!, hace falta una interfaz de TCL admitida para terceros o un dispositivo que exponga `TvInteractiveAppService` junto con un proveedor compatible con HbbTV.

## Acceso externo provisional desde O! (`0.3.0-alpha01`)

Esta versión **no integra HbbTV en el reproductor de O!**. En la TCL probada, los controles del reproductor ofrecen **LOVEStv en TCL** únicamente para `La 1 UHD`, `La 2 HD`, `A3 UHD`, `Antena 3` y `Telecinco`, todos servicios TDT del `com.tcl.tvinput`. Radio y los demás canales no muestran el acceso. La lista se basa en nombres reales ya probados; un cambio de nombre del proveedor puede ocultarlo hasta revisar de nuevo el canal.

La secuencia por mando es **OK** para mostrar controles, **→** para enfocar el botón y **OK** para abrir TCL TV. O! verifica que `com.tcl.tv` resuelva el Intent antes de liberar la reproducción. Después envía un `Intent.ACTION_VIEW` con `TvContract.buildChannelUri(id)`, MIME `vnd.android.cursor.item/channel` y paquete `com.tcl.tv`. Resetea el `TvView` antes del cambio para soltar la sesión del sintonizador. Si no se encuentra o no puede abrirse la actividad, conserva el reproductor o vuelve a sintonizar el canal y muestra un mensaje. La consulta del paquete está declarada en `<queries>` para Android 11.

En TCL TV el botón Azul sigue perteneciendo a la aplicación del fabricante. O! no envía teclas a otras aplicaciones. Para regresar, el usuario vuelve a abrir O! desde Google TV. El retorno no es automático. Al reanudarse, el `TvView` de O! crea una sesión nueva para el canal guardado.

### Pruebas en esta actualización

La compilación instalada conserva `applicationId` `com.ayanabyte.one`, el certificado anterior y el permiso `READ_TV_LISTINGS`. La TCL identificó la APK instalada con la misma suma SHA-256 que el artefacto local. Las filas compatibles observadas fueron:

| Canal en O! | ID TvContract | Resultado en TCL TV | Azul en TCL |
| --- | ---: | --- | --- |
| A3 UHD | `10032309` | Abrió `TVActivity` en A3 UHD y liberó la sesión de O!. | El log de TCL cargó `lovestv3-4.0.1` desde Atresmedia. |
| La 2 HD | `10032332` | Abrió el canal elegido. | El log cargó `lovestv3-4.0.1` desde el lanzador RTVE. |
| Telecinco | `10032347` | Abrió Telecinco. | El log cargó `lovestv3-4.0.1` desde el lanzador Mediaset. |
| La 1 UHD | `10032331` | Abrió el canal elegido. | La primera pulsación mostró el lanzador rojo RTVE; otra pulsación Azul abrió `lovestv3-4.0.1`. |

Los resultados corresponden a las páginas y rutas que el navegador HbbTV TCL registró en `logcat`; las funciones internas disponibles pueden variar por canal y señalización. No se verificó el inicio desde el principio ni la reproducción de vídeo bajo demanda en esta tarea.

- El foco del botón **LOVEStv en TCL** apareció en la jerarquía de la interfaz para A3 UHD. En 24h HD, un canal fuera de la lista comprobada, el botón no apareció. En Radio TDT tampoco apareció.
- Tras cada traspaso se abrió O! manualmente desde el lanzador. `TvView` volvió a sintonizar y se registraron las pistas de audio y disponibilidad de vídeo. Se repitió el flujo en varios canales sin reiniciar el proceso de O!.
- La radio 95.2 Terrassa volvió a seleccionar una pista de audio. Atrás hasta Inicio generó `Session reset` y `dumpsys tv_input` dejó de mostrar una sesión de reproducción de O!.
- Un cambio rápido de canal en una compilación intermedia expuso un intento de adjuntar un `TvView` que aún tenía padre. Se corrigió manteniendo la misma vista Android y reiniciando su efecto TIF por canal. La build final cambió de canal sin repetir ese cierre.
- La captura de pantalla del vídeo TIF no está disponible por la composición SIDEBAND de esta TCL. Se verificaron callbacks de vídeo, selección de audio y carga de páginas de LOVEStv en `logcat`; no se declara una inspección de imagen mediante captura.

La integración sigue dependiendo de que el paquete nativo TCL permanezca instalado y resuelva la URI de `TvContract`. Este acceso es una derivación externa al servicio oficial de TCL, no una barra HbbTV propia de O!.
