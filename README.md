# Leitner

App Android para estudiar con la **caja de Leitner** (repetición espaciada con tarjetas).

## Cómo funciona

- Cada tarjeta está en una caja. Por defecto hay 5 cajas, que se repasan cada 1, 2, 4, 8 y 16 días.
- Si aciertas, la tarjeta sube una caja. Si fallas, vuelve a la caja 1.
- El número de cajas y los intervalos son configurables.

## Modos

- **Modo niños**: colores vivos, textos grandes, 3 cajas por defecto (1, 2, 4 días).
- **Modo normal**: diseño sobrio, 5 cajas por defecto.

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
5. Extras: recordatorios, importar CSV, imágenes y audio en modo niños
