# Instrucciones para Codex — O! ONE ENTERTAINMENT

- Repositorio nuevo e independiente: no introducir marcas ni identificadores de proyectos anteriores.
- Aplicación **Android TV** en **Kotlin**, no una app de teléfono.
- Respetar control por **D-pad**, foco visible, botón Atrás y navegación a distancia.
- No simular canales, reproducción, EPG, grabaciones ni radio: implementar funciones reales y probarlas antes de marcarlas como terminadas.
- Identidad: O blanca sobre negro, punto celeste #4DDCF5, tipografía blanca y textos secundarios grises. Sin neón, 3D ni degradados.
- El opening es nativo y no necesita MP4. Mantener el punto sincronizado con la aparición gradual de ONE ENTERTAINMENT.
- No añadir audio de marca todavía. La intención aprobada es ruido de sintonización de TV analógica que desaparece; nada de campanas, notas musicales o cristal.
- Priorizar arquitectura sencilla, modular y testeable. Antes de implementar funciones complejas, documentar límites de Android TV y soporte del hardware.
- No mover código al repositorio principal sin compilar la APK de depuración en GitHub Actions.
- Mantener el README y ROADMAP actualizados cuando una función esté realmente probada.

## Política de identidad y dependencias

- La única identidad de aplicación de O! es `com.ayanabyte.one`; conservar esta identidad en código, recursos, Gradle, manifiestos, documentación y variantes de compilación.
- No incorporar archivos, paquetes, imports, fuentes ni parches de aplicaciones Android anteriores. La investigación de motores HbbTV de fabricante se realiza en un laboratorio independiente y no forma parte del código de producción.
- El acceso LOVEStv actualmente admitido desde O! es un Intent explícito a la app nativa `com.tcl.tv`, usando el canal real de `TvContract`. No invocar aplicaciones de demostración como dependencia de producto.
- Ejecutar `python3 .github/scripts/check_identity.py` antes de cada PR. La compilación de CI debe rechazar identificadores heredados.
