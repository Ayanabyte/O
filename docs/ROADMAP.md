# O! ONE ENTERTAINMENT — Hoja de ruta

## Fase 0: base Android TV (0.1.0-alpha01)
- Kotlin, Jetpack Compose y navegación mediante mando D-pad, con foco inicial visible y retorno a la última tarjeta al cerrar una sección.
- Lanzador Leanback para Google TV / Android TV.
- Identidad O!: anillo blanco, punto celeste y fondo negro.
- Intro vectorial de unos 8 s, con aparición sincronizada de ONE ENTERTAINMENT.
- Pantalla inicial honesta: no muestra fuentes ni canales ficticios.
- Gradle Wrapper 8.13 y flujo de GitHub Actions para compilar, ejecutar lint y entregar una APK de depuración por ejecución.
- Flujo de distribución para `com.ayanabyte.one` con la misma firma entre versiones. Una etiqueta de versión genera un borrador de GitHub Release con la APK y su suma SHA-256 cuando los secretos de firma están configurados.
- Validado en una TCL con Google TV y Android 11 por ADB: lanzador Leanback, opening, inicio, foco en las cinco tarjetas y navegación con las teclas del mando. Atrás omite el opening o regresa a la tarjeta desde la sección. Desde el inicio, Atrás cierra la actividad.
- **No hay reproducción de contenidos todavía.**

## Fase 1: fuentes y reproducción reales

### Televisión TDT (`0.2.0-alpha01`)

- Detección y consulta mediante `TvInputManager` y `TvContract`. La TCL registra 57 canales de televisión y 38 emisoras digitales terrestres.
- Sintonización a pantalla completa, cambio por mando y liberación de la sesión mediante `TvView`. Los servicios, permisos y resultados de la TCL están en [Televisión digital terrestre](TDT.md).

### Fuentes de red pendientes

- Importar listas IPTV M3U/M3U8, con manejo de credenciales y pruebas de conexión.
- Radio IPTV y enlaces directos.
- Reproductor Media3 para fuentes IPTV, con pistas de audio y subtítulos.
- Agrupación de canales duplicados y prioridad de fuente TDT cuando funcione.
- Recuperación de fallos y cambio de fuente, sin pruebas periódicas innecesarias.

## Fase 2: guía y navegación
- XMLTV múltiple con prioridades, asignación automática fiable y edición manual.
- Guía EPG con preview cuando el sistema lo permita y controles D-pad.
- Favoritos, listas, grupos, ordenación y opciones por canal.
- LOVEStv verificado en la app nativa de TCL. O! ofrece un **acceso externo provisional** en los cuatro canales TCL probados mediante `ACTION_VIEW` y `TvContract`, libera su sesión y permite retomar el reproductor al volver desde el lanzador. HbbTV no está integrado en O! y el retorno no es automático. [Investigación, alcance y pruebas en Android 11](HBBTV.md).

## Fase 3: contenido bajo demanda
- IPTV VOD, USB y NAS en biblioteca unificada, con duplicados agrupados.
- Películas y series, colecciones y filtros, historial y continuar viendo.
- Trakt.tv, artwork externo y tráilers bajo demanda.

## Fase 4: TV avanzada
- Timeshift local o recuperación desde fuentes compatibles.
- Grabaciones, programación EPG/repetitiva y conflictos.
- Catch-up IPTV cuando el proveedor lo soporte.
- Accesibilidad, control parental y configuración avanzada.

## Diseño de marca aprobado
- Marca: **O!**
- Extensión: **ONE ENTERTAINMENT**
- Inglés: **All you love. One screen.**
- Español: **Todo lo que te gusta. Una sola pantalla.**
- Fondo puro negro; texto principal blanco y secundario gris; único acento celeste.
- No neón, cristales, gradientes, 3D ni paneles laterales permanentes.
- Logo original O! → la barra de exclamación se desvanece mientras el punto desciende hasta debajo de O. El nombre aparece en paralelo. El eslogan aparece después.
- Audio deseado (no integrado): estática de televisor analógico que se sintoniza progresivamente, terminando sin campanas ni música.

Las fases son objetivos, **no una promesa de soporte en todos los televisores**. Primero validar hardware y permisos de Android TV.
