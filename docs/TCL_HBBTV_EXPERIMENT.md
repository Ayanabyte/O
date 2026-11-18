# O! — Prueba de HbbTV embebido en TCL (sin abrir TCL TV)

**Estado: experimental, NO verificado en un televisor.** Esta rama conserva la
base TDT de O!, elimina el acceso externo a `com.tcl.tv`, y prepara una variante
`tclHbbtv` de **32 bits** que intenta utilizar el motor HbbTV y el navegador
TCL **dentro de la propia Activity y sobre el mismo TvView de O!**.

## Procedencia exacta

El mecanismo proviene del código fuente histórico de Channel+ v5.3.14:
`ci/channelplus-5.3.7-overlay.zip` (SHA-256
`fea649552eb21f7a5df958a20dbb1cf6915b186b701a346aca498be434b8173b`).
El ZIP contiene `HbbTvPlayerView`, `HbbTvRuntimeService`,
`HbbTvActivityHost`, el JAR del runtime 1.45, bibliotecas JNI y fuentes.
El workflow aplica los parches históricos `fix_539_key_routing`,
`fix_539_reliability`, `fix_5312_direct_activity`,
`fix_5313_playback_controls` y `fix_5314_seek_live`, en ese orden.

La ruta 5.3.14 es un **punto de partida de código**, NO una afirmación de que
la APK 5.3.14/5.3.17/5.4.3 funcione hoy: esas versiones fallaron en pruebas
posteriores del usuario. El éxito del botón verde presupone que HbbTV arranque.

## Aislamiento y compilación

- `standard`: sin JNI privada, sin HbbTV, mantiene TvView.
- `tclHbbtv`: utiliza `armeabi-v7a`, proceso propio llamado `com.tcl.tv`,
  dependencias opcionales del fabricante, una sola sesión TvView y un navegador
  transparente montado en la Activity.
- La variante experimental usa `com.ayanabyte.one.hbbtvtest`, por lo que **no
  sobrescribe O!** ni sus datos.

En GitHub Actions ejecuta **O! · TCL embedded HbbTV experiment**.
El workflow obtiene el código de Channelplus v5.3.14, prepara las fuentes y
compila `:app:assembleTclHbbtvDebug`. Para compilación local, primero
descarga ese repositorio y ejecuta
`python scripts/prepare_tcl_hbbtv.py <ruta-Channelplus>`.

## Validación en la TCL

1. Instalar la APK experimental sin borrar la aplicación O! estable.
2. Abrir una TDT real y comprobar que sintoniza y mantiene audio/vídeo.
3. Sin cambiar de app, pulsar Azul; comprobar que el navegador oficial aparece
   en O!, navega con D-pad/OK, regresa a la TDT y puede reabrirse.
4. Comprobar RTVE, Antena 3, Telecinco, cambio rápido de canal y Radio TDT.
5. Recoger `adb logcat -s ChannelplusHbb:V OneTdt:V '*:S'`.
6. Si el motor TCL deniega acceso, registrar la llamada exacta que falla.
   **No** sustituir por `ACTION_VIEW` ni afirmar éxito con logs de init/start.

**No se fusionará en main ni se publicará como versión estable sin una
prueba visual real en la TCL y pruebas de regresión de TV/radio.**

Nota: el JS histórico de salto de anuncios depende de una ruta
`/lovestv3-4.0.0/`; la ruta actual observada fue `/lovestv3-4.0.1/`.
Esa compatibilidad debe validarse por separado una vez que el motor abra HbbTV.

## Diagnóstico visual (versión experimental)

La variante `tclHbbtv` muestra en la esquina superior derecha los estados
reales `enabled`, `video`, `jar`, `overlay`, `surface`, `bound`,
`messenger`, `attached`, `started`, `failed` y `error`, además del
identificador de entrada TCL. Una captura al sintonizar Antena 3 ayuda a
localizar el bloqueo sin depender de una terminal ADB. El indicador no
reemplaza la validación real del aviso LOVEStv, que lo genera la emisora.

### Instalación del diagnóstico detallado
La compilación de diagnóstico usa temporalmente el identificador
`com.ayanabyte.one.hbbtvdiag` para coexistir con la prueba previa
`com.ayanabyte.one.hbbtvtest` y con O! estable. Esto evita conflictos
entre certificados debug generados por distintos runners de GitHub Actions.

## Identificación inequívoca de la revisión 2
La revisión DIAG 2 se instala como aplicación **O! LOVEStv DIAG 2**
(paquete `com.ayanabyte.one.hbbtvdiag2`), con el encabezado
`O! DIAG 2 · ERROR TCL REAL`. Es distinta de las pruebas anteriores:
si ves el título antiguo o el error genérico «Runtime initialization failed;
see preceding ChannelplusHbb log», no estás ejecutando esta revisión.

## Revisión DIAG 3 — corrección del archivo empaquetado

DIAG 2 seguía mostrando `TCL HbbTV failed without details` porque el
script copiaba el Java original a O! antes de inyectar el detalle. DIAG 3
vuelve a copiarlo tras la modificación y comprueba el archivo empaquetado.
APK separada `com.ayanabyte.one.hbbtvdiag3` y cabecera `O! DIAG 3`.

## Restricción de identidad de la biblioteca heredada

En \`ExperimentalAuthorization.class\` se encontró una comprobación literal
\`tv.own.owntv.tdt2\` que bloqueaba cualquier paquete nuevo antes de intentar
el registro HbbTV. DIAG 3 adapta **solo esa condición interna del código
heredado de Channel+** para \`com.ayanabyte.one.hbbtvdiag3\`. La comprobación
real del fabricante \`systemAuthorized\` continúa intacta; su aprobación y el
funcionamiento de LOVEStv no están garantizados. El diagnóstico mostrará la
última etapa de error sin revelar valores de autorización.


## DIAG 4 — permisos de conectividad TCL

DIAG 3 mostró una excepción remota de ConnectivityService.getAllNetworks() en enforceAccessPermission(). La variante tclHbbtv declara ahora ACCESS_NETWORK_STATE e INTERNET (permisos normales); muestra netStatePermission e internetPermission. La variante standard no recibe nuevos permisos. DIAG 4 utiliza com.ayanabyte.one.hbbtvdiag4 para instalarse separadamente.


## DIAG 5 — etapas del arranque HbbTV

Esta variante registra y presenta la última etapa emitida por el servicio en `lastStage`: registro de autorización, preparación del navegador del fabricante, inicialización de `HbbtvServer`, llamada a `Tbrowser.initTbrowser`, respuesta del navegador y `HbbtvServer.start`. Así se puede diferenciar un arranque todavía pendiente de un bloqueo. Conserva los permisos de DIAG 4, el reproductor TDT y el error detallado. Nombre: O! LOVEStv DIAG 5. Paquete separado `com.ayanabyte.one.hbbtvdiag5`.

## DIAG 6 — montaje de navegador y callback TCL

En DIAG 5, `Tbrowser.initTbrowser` se invocó, pero no notificó el resultado antes del timeout. DIAG 6 reproduce mejor la jerarquía de la prueba histórica 1.45: el navegador transparente se añade al contenedor Android `android.R.id.content` de la Activity ya existente, no a su `decorView`; el `TvView` TDT permanece intacto. Se instrumenta el registro del listener, la presencia de token de ventana y **todos los callbacks** que emita Tbrowser. No se fuerza a iniciar HbbTV sin callback. Paquete independiente `com.ayanabyte.one.hbbtvdiag6`.

## Resultado del intento DIAG 7

El usuario confirma que DIAG 7 se cierra aproximadamente 3 segundos después de sintonizar un canal. DIAG 7 omitía el puente experimental de CA (`NativeSection.prepareCertificates`), a diferencia de DIAG 6. La causa del cierre aún NO está identificada: puede ser una excepción Java, un fallo nativo o terminación del proceso. No repetir este cambio ni desactivar rutinas nativas hasta examinar el registro de fallos `logcat -b crash` y los eventos de `AndroidRuntime` / `ActivityManager`. Por seguridad se restaura la implementación DIAG 6 en la rama, que al menos no mostraba ese cierre inmediato. O! estable continúa sin cambios.

## Confirmación por Bugjaeger del cierre DIAG 7 (18/11/2026)

Log del dispositivo TCL G07 / Android 11, reproducido tres veces a las **00:39:40, 00:40:14 y 00:40:52** (hora de Madrid):

- **Proceso:** `com.tcl.tv`, nombre del proceso compartido *por configuración* de la APK experimental; esto **no demuestra** que se haya caído el paquete oficial del fabricante.
- **Hilo:** `hbbtv_api`, ABI arm (32 bits).
- **Error:** `SIGSEGV (SEGV_MAPERR)` al acceder a `0x0` (puntero nulo).
- **Pila:** `libc.so!__memcpy_a7` → `/product/lib/libsitatv.so!tos_system_decrypt_cert+424` → `/product/lib/libhbbtv-server.so!api_task_entry+932`.
- **Correlación:** DIAG 7 omitió `NativeSection.prepareCertificates` antes de iniciar el motor HbbTV, y las tres ejecuciones de esa variante se cerraron; el código DIAG 6 se ha restaurado. La causalidad es plausible pero no demostrada por un análisis de memoria.
- **No confundir:** `KoinApplication has not been started` (17/11 21:48, `tv.own.owntv.MainActivity`) y `motionclient` no son estos cierres.

**No publicar ni volver a ejecutar DIAG 7. No omitir ni eludir las rutinas de certificados, ni modificar claves/almacenes del firmware para sortear los fallos.** La siguiente investigación debe centrarse en por qué `NativeSection.installCertificates` devolvía `-1` en DIAG 6 y por qué `Tbrowser.initTbrowser` no producía callback en DIAG 5. Para ello se requieren logs previos al fallo, o análisis estático de fuentes y bibliotecas autorizadas. Evitar nuevos intentos nativos que puedan desestabilizar la TV sin diagnóstico previo.


## Forensic audit — native certificate bridge and browser startup (2026-11-18)

Source: Channelplus v5.3.14 historical overlay (SHA256
fea649552eb21f7a5df958a20dbb1cf6915b186b701a346aca498be434b8173b).
The inspected libsectionprobe.so SHA256 is
399d2ad33c4cb4ffa4e29c6b083f2e7f0af6313273761dafaa0bf73bfdad6cc6.

Verified static findings:
- NativeSection.prepareCertificates validates the vendor libraries, reads
  /proc/self/maps and invokes JNI NativeSection.installCertificates. This is
  an in-process native provider hook, not merely Android system CA trust.
- The ARM function at virtual address 0x1918 checks the local global
  'provider' at virtual address 0x4000. If this is nonzero, execution branches
  to return -1. Other precondition failures also return -1.
  Therefore DIAG 6's -1 may be caused by repeat initialization, but this
  cause is NOT yet proved for that particular attempt.
- The original HbbTvRuntimeService can attempt initialization after channel
  or session changes; cleanup does not necessarily end the Android process.
  Without a guard, repeated native installation is a genuine risk.
- In DIAG 7, removal of NativeSection.prepareCertificates correlated with
  three SIGSEGV crashes in libsitatv.so!tos_system_decrypt_cert called by
  libhbbtv-server.so!api_task_entry (user's 00:39/00:40 crash log).
  Never omit/bypass native CA initialization again.
- In DIAG 5, Tbrowser.initTbrowser was called but no callback arrived.
  No inspected evidence yet establishes WHY: activity/window context,
  vendor state or another native prerequisite could be involved.
  Do not assume that a timeout is equivalent to permission denial.

Fix in experimental branch ONLY:
- prepareNativeCertificatesOnce(Context) stores successful installation state
  for the lifetime of the process and reuses it.
- If installation failed, it refuses another native memory patch attempt in
  the same process.
- This does not disable TLS validation, modify vendor files or guarantee
  that Tbrowser will complete initialization.
- No install or run on TCL is authorized by CI success. Do not merge into
  stable until actual device validation.

Existing evidence can be read from Bugjaeger WITHOUT launching HbbTV:
run-as com.ayanabyte.one.hbbtvdiag6 cat files/lovestv-report.txt
run-as com.ayanabyte.one.hbbtvdiag5 cat files/lovestv-report.txt
logcat -d -v time -t 1200 -s ChannelplusHbb:V ChannelplusLoveProbe:V '*:S'

The stored report is written by Report.add in the historical runtime JAR.
It may be absent; that does not prove anything about TCL authorization.

## Registro de DIAG 7 recuperado con Bugjaeger (18/11/2026)

El informe privado lovestv-report.txt de com.ayanabyte.one.hbbtvdiag7 (2526 bytes) contiene tres ejecuciones, a las 00:39:38, 00:40:12 y 00:40:50.
En las tres secuencias se observa: verificación SHA256 del APK TCL; llamada systemAuthorized devolviendo true para el PID experimental; tcl.webkit accesible y constructor WebView(Context) visible; mensaje UA 1.45 'instalacion antes de init=0' con perfil RT51A/V138/HbbTV1.5.1.
No se observa respuesta exitosa de Tbrowser ni arranque de LOVEStv. Poco después, las tres trazas de crash registran SIGSEGV en libsitatv.so!tos_system_decrypt_cert+424 desde libhbbtv-server.so!api_task_entry+932.
systemAuthorized=true descarta rechazo en esa llamada, pero NO demuestra permisos efectivos para todas las funciones HbbTV. El valor 0 del log de UA no debe interpretarse sin contrato de la función. La disponibilidad de tcl.webkit no prueba renderizado.
La ausencia del puente de certificados en DIAG7 se correlaciona estrechamente con el fallo nativo, pero no demuestra por sí sola la causa exacta del puntero nulo. NO ejecutar nuevamente DIAG7, ni desactivar el puente.
DIAG5 y DIAG6 ya no están instaladas en el dispositivo, según pm list packages; sus archivos privados no son recuperables mediante run-as. DIAG7 fue revertida en la rama experimental; se añadió una guarda para evitar instalar el puente de CA dos veces en el mismo proceso.
Próximo dato útil sin repetir el arranque: logcat almacenado de ChannelplusHbb y trazas Tbrowser/NativeSection, si aún existen. La rama experimental permanece sin fusionar; funcionamiento real de LOVEStv no demostrado.

## Validación de APK antiguas y gestión de fuentes (18/11/2026)

- El usuario confirma: Channel+ 5.3.14 no abre LOVEStv y Channel+ 5.3.13 no permite añadir/configurar fuentes para probar canales. No presentarlas como versiones funcionales.
- La APK 5.3.13 procede de la ejecución de GitHub Actions 41752572973, commit bef9ebf5b27eb252b89c7fa6cae1c0f59f434ffc.
- Comparación por SHA Git de 5.3.13 y tag v5.3.17: ManageSourcesScreen.kt (79066ba6364e597555f09abab6419a67aa764901), AddSourceScreen.kt (51fd1f357007414de2d7e1c64ba476bc92d5c3ce), AddSourceChooserScreen.kt (9255b01de23a253aad7d6e88acc6f43e5749c570) son **idénticas** en ambos puntos del historial.
- El código sí incluye formularios M3U/Xtream/Stalker y sintonizador, pero su mera presencia no demuestra que se pueda entrar en ellos o guardar fuentes en el dispositivo. No hay diagnóstico suficiente para atribuir el fallo a un archivo en particular.
- El histórico fix_5313_playback_controls.py conserva el adaptador de controles lovestv_playback_145.js (botón verde). Este adaptador necesita un Tbrowser operativo. No demuestra que v5.3.13 arranque correctamente.
- Evitar ciclos de APK históricas; conservar O! estable como base TDT y tratar la integración TCL como rama experimental no fusionada, resolviendo primero la inicialización del navegador y el puente nativo.
