# Leitner

App Android para estudiar con la **caja de Leitner** (repetición espaciada con tarjetas).

## Cómo funciona

- Cada tarjeta está en una caja. Por defecto hay 5 cajas, que se repasan cada 1, 2, 4, 8 y 16 días.
- Si aciertas, la tarjeta sube una caja. Si fallas, vuelve a la caja 1.
- El número de cajas y los intervalos son configurables.

## Modos

- **Peques** (modo niños): colores vivos, textos grandes, 3 cajas por defecto (1, 2, 4 días).
- **Clásico** (modo normal): diseño sobrio, 5 cajas por defecto.

Cada modo tiene sus propios mazos y su propia configuración.

## Estructura

- `core/`: lógica de Leitner en Kotlin puro, con tests.
- `app/`: app Android (Jetpack Compose + Room).

## Compilar

```
./gradlew :core:test assembleDebug
```

La CI de GitHub genera el APK de depuración en cada push.

## Plan

1. ✅ Proyecto base y base de datos
2. ✅ Crear y editar mazos y tarjetas
3. ✅ Pantalla de repaso diario y ajustes de cajas
4. ✅ Estadísticas sencillas
5. Extras: ✅ imágenes y audio · ✅ PIN modo niños · ✅ recordatorio diario · ✅ importar CSV

## Repaso

- Las tarjetas salen por caja (primero las de la caja 1) y al azar dentro de cada caja.
- Las falladas se repiten al final de la sesión hasta acertarlas. Solo cuenta el primer intento.
- **↶ Deshacer** anula la última respuesta.
- Con varios mazos pendientes aparece **Repasar todo**.
- **🗣 Leer** lee la tarjeta en voz alta. En Peques se lee sola.

## Tarjetas

- Pregunta y respuesta pueden llevar texto, imagen y audio.
- **Crear también al revés** (al crear una tarjeta) o menú ⋮ → **Crear tarjetas al revés** (todo el mazo).
- Con más de 5 tarjetas aparece un buscador en el mazo.
- Menú ⋮ → **Exportar CSV**: guarda las tarjetas con texto en formato `pregunta;respuesta`.

## Motivación

- **Pegatinas** (logros) por aciertos, rachas y días de estudio. Se ven en 🏆 / Estadísticas y se anuncian al terminar un repaso.
- **Widget** para la pantalla de inicio con las tarjetas pendientes de hoy. Al tocarlo abre la app.

## Traducciones y accesibilidad

- La app está en español e inglés (según el idioma del móvil). Los textos están en `app/src/main/res/values/strings.xml` (español) y `values-en/strings.xml` (inglés). Para otro idioma, copia el fichero a `values-XX/` (p. ej. `values-en/`) y tradúcelo.
- Imágenes, gráficos, pegatinas y botones de solo icono tienen descripción para el lector de pantalla (TalkBack).

## Tema

Ajustes → **Tema**: como el móvil, claro u oscuro.

## Importar CSV

En la pantalla de un mazo: menú ⋮ → **Importar CSV**. Una tarjeta por línea, `pregunta;respuesta`
(también vale coma o tabulador). La primera línea se salta si es una cabecera (`pregunta;respuesta`).
Se aceptan comillas para textos con separadores o saltos de línea, y ficheros guardados desde Excel.

## Copia de seguridad

Ajustes → **Copia de seguridad**: *Exportar* guarda un `.zip` con todos los mazos, tarjetas, imágenes,
audios, ajustes de cajas y estadísticas de los dos modos. *Importar* sustituye todos los datos actuales
por los de la copia (pide confirmación).

## Publicar (APK firmado)

Solo hay que hacerlo una vez:

1. Crear la clave (guárdala bien: sin ella no se pueden publicar actualizaciones):
   ```
   keytool -genkeypair -v -keystore leitner.jks -alias leitner -keyalg RSA -keysize 2048 -validity 10000
   ```
2. En GitHub → Settings → Secrets and variables → Actions, crear estos secretos:
   - `LEITNER_KEYSTORE_BASE64`: el resultado de `base64 -w0 leitner.jks`
   - `LEITNER_KEYSTORE_PASSWORD`: la contraseña del keystore
   - `LEITNER_KEY_ALIAS`: `leitner`
   - `LEITNER_KEY_PASSWORD`: la contraseña de la clave

Para sacar una versión: crear una etiqueta `v1.0.0` (o lanzar el workflow **Release** a mano).
El APK y el AAB (para Google Play) aparecen en GitHub Releases.
