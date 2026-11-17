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
