# LabHoras

App para registrar las horas a facturar en el laboratorio. Pensada para Android 16 (One UI 8.1).

## Qué hace

- Muestra todos los días del mes, incluidos fines de semana y feriados. Los feriados de Uruguay se marcan como referencia.
- Al lado de cada día se escriben las horas trabajadas. Acepta `8`, `7,5`, `7.5` o `7:30`.
- Suma las horas del mes (y de cada semana).
- Multiplica las horas por el valor hora (por defecto **$462**).
- El valor hora se puede cambiar desde **Ajustes** indicando desde qué fecha rige. Las horas anteriores conservan el valor que tenían.
- Al subtotal le suma el **IVA (22 %)**. El porcentaje también es editable.
- **Compartir** envía un resumen del mes (horas, subtotal, IVA, total y detalle por día) por WhatsApp, correo, etc.
- **Respaldo**: en Ajustes se puede copiar todo como texto y restaurarlo después.

### Reloj de marcar (solo en la app instalada)

- Botón **Entrada / Salida** arriba de todo: pide la huella (o el PIN del teléfono si la huella falla) y registra la hora.
- Al marcar la salida, las horas del día se cargan solas con el tiempo exacto al minuto (por ejemplo 3 h 17 min). Se factura ese tiempo exacto.
- Una entrada y una salida por día. Si cruza la medianoche, cuenta para el día de la entrada.
- Mientras estás adentro hay una notificación con el tiempo corriendo y un botón **Marcar salida**.
- A las 8 horas sin marcar la salida llega un recordatorio. Pasadas 16 horas, la app pide escribir a mano la hora de salida.
- Las marcas se corrigen tocando el horario del día (o **Corregir** en la tarjeta del reloj).
- Acceso rápido: mantener presionado el ícono → **Marcar**, o agregar el botón **Marcar** a los botones rápidos del panel de notificaciones (lápiz ✎ → arrastrar).

La huella confirma que quien marca es alguien registrado en el teléfono; la app no recibe la huella ni sabe la ubicación. Es un registro personal, no un comprobante.

Cálculo: `subtotal = Σ (horas del día × valor hora vigente ese día)`, `IVA = subtotal × 22 %`, `total = subtotal + IVA`, redondeado a centésimos.

## Instalar en el teléfono

1. En el teléfono, abrir la página del repositorio en GitHub → **Releases** → la última versión.
2. Descargar el archivo `LabHoras-1.0.N.apk`.
3. Abrirlo e instalar. La primera vez Android pide permitir instalar apps de esa fuente (Chrome o Mis archivos).

Cada cambio en el código genera una versión nueva automáticamente (GitHub Actions). Se instala encima de la anterior y los datos se conservan.

## Estructura

- `web/` — la app (HTML, CSS y JS en `index.html`, más la tipografía Figtree, licencia SIL OFL). Es la única fuente de la interfaz.
- `android/` — contenedor nativo (WebView) que empaqueta `web/` como assets. Java, minSdk 30, targetSdk 36.
- `.github/workflows/android.yml` — compila el APK firmado y lo publica en Releases.

Compilar localmente (requiere Android SDK): `cd android && ./gradlew assembleRelease`.

### Sobre la firma

`android/keystore/labhoras.jks` está en el repositorio para que todas las versiones salgan firmadas con la misma clave y se puedan instalar encima de la anterior. Es una clave de uso personal: no sirve para publicar en Google Play.
