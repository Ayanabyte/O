# LOVEStv y HbbTV en O!

## Estado actual

La versión `0.3.0-alpha01` de O! usa exclusivamente el paquete Android
`com.ayanabyte.one`. Su reproductor de TDT utiliza las API públicas
`TvInputManager`, `TvContract` y `TvView`.

En la TCL G07 / RT51A con Android 11, O! reproduce y cambia canales TDT,
pero **no dispone de una integración HbbTV embebida verificada**. Por ello,
O! ofrece un acceso provisional a LOVEStv mediante la aplicación de
televisión nativa del fabricante.

La aplicación de demostración LOVEStv · Diseño 1.45 funciona en el televisor,
pero es una referencia experimental independiente: no es una dependencia,
un módulo ni una aplicación invocada por O!.

## Acceso funcional con la TV de TCL

En los canales TDT compatibles ya probados, durante la reproducción:
1. Pulsa **OK** para mostrar los controles.
2. Pulsa **Derecha** para enfocar **LOVEStv en TCL** y confirma con **OK**.
3. O! comprueba que el paquete `com.tcl.tv` pueda abrir el canal.
4. O! libera `TvView` y envía un `Intent.ACTION_VIEW` con la URI real
   `TvContract.buildChannelUri(id)` y tipo `vnd.android.cursor.item/channel`.
5. Una vez en la TV de TCL, pulsa **Azul** para abrir LOVEStv si el canal
   ofrece el servicio.

El control solo se muestra para los servicios terrestres verificados:
**La 1 UHD, La 2 HD, A3 UHD, Antena 3 y Telecinco**.
No aparece en Radio TDT ni en canales sin verificación. La identificación
se basa en los nombres devueltos por TCL y puede requerir revisión si cambian.

En este firmware la tecla Azul pertenece al motor HbbTV del fabricante.
Una aplicación Android ordinaria no puede inyectar teclas en la ventana
de otra aplicación mediante las API públicas. Para regresar a O!, hay
que abrirla desde el lanzador de Google TV; el retorno automático sigue
pendiente.

## Evidencia obtenida en el televisor

- `com.tcl.tv/.TVActivity` abre los canales reales a través del TIF
  `com.tcl.tvinput/.TDTVInputService/HW1413742848`.
- En A3 UHD, La 2 HD y Telecinco, el motor HbbTV nativo registró una
  carga de la aplicación principal de LOVEStv tras la tecla Azul.
- La 1 UHD mostró primero la experiencia interactiva de RTVE y posteriormente
  abrió LOVEStv mediante otra pulsación.
- Después del traspaso, volver a iniciar O! permite crear una nueva sesión
  `TvView`. Las pruebas anteriores verificaron vídeo o pistas de audio,
  cambio de canal y liberación de sesión al salir.
- No está confirmada en estas pruebas la reproducción de contenido bajo
  demanda ni el retorno automático a O!.

### Resultado del laboratorio HbbTV independiente

Un laboratorio separado, ejecutado como aplicación Android normal, consiguió
sintonizar La 1 y A3 UHD, recibir `onVideoAvailable`, inicializar
`HbbtvServer` y `Tbrowser` y ejecutar `HbbtvServer.start()`.

El laboratorio no activó LOVEStv: `key_set=0` y el botón Azul no se consumió.
Los registros muestran intentos de recepción de tablas DVB con expiraciones.
La sesión TIF pertenece al propio laboratorio; en un evento de reproducción
el motor recibe número de programa y PID de la PMT, pero el campo nativo
`ccid` aparece vacío. Reiniciar la sesión `TvView` y resintonizar
con HbbTV iniciado tampoco resolvió el fallo.

Estas observaciones **no prueban una causa única**: puede faltar contexto
DVB, autorización del fabricante u otra inicialización nativa.

## Restricciones de integración

Este televisor usa Android 11 (API 30) y no expone el servicio público
`tv_interactive_app`. Las interfaces `TvInteractiveAppManager` y
`TvInteractiveAppView` llegaron a Android en API 33; su presencia, por
sí sola, tampoco garantiza un servicio HbbTV compatible del fabricante.

Las implementaciones experimentales existentes contienen autorización
propietaria y preparación nativa específica de TCL. No existe un contrato
público confirmado para trasladarlas a una aplicación de terceros.
No se importarán clases, nombres de paquete, scripts de parcheo, claves
de autorización ni código procedente de aplicaciones anteriores.

**Decisión para O!:** conservar el acceso externo probado con
`com.tcl.tv` y mantener la investigación HbbTV embebida en un laboratorio
independiente hasta disponer de una interfaz compatible y verificable.

Referencias de plataforma:
- [Android TV Input Framework](https://source.android.com/docs/devices/tv)
- [TvInteractiveAppView](https://developer.android.com/reference/android/media/tv/interactive/TvInteractiveAppView)
- [TvInputService.Session](https://developer.android.com/reference/android/media/tv/TvInputService.Session)
