# Thalia Streaming — App Android

App nativa que envuelve el sistema web de Thalia Streaming (login, portal de clientes, panel de proveedor y panel de administración) en una WebView.

## Antes de compilar

Abre `app/src/main/java/com/streamingthalia/app/MainActivity.java` y revisa:

```java
private static final String SERVER_URL = "http://144.126.137.93:8081/";
```

Cámbiala si tu servidor está en otra dirección, por ejemplo:

```java
private static final String SERVER_URL = "http://192.168.1.100:8081/";
```

## Cómo obtener el APK

1. Sube los cambios a la rama `main` de GitHub.
2. Ve a la pestaña **Actions**.
3. El workflow **Build APK** corre automáticamente al hacer push.
4. Cuando termine (3–5 minutos), entra a la corrida y descarga **StreamingThalia** en *Artifacts*.
5. Descomprime el ZIP: dentro está `app-debug.apk`.
6. Pásalo a tu celular e instálalo.

Para correrlo manualmente: Actions → Build APK → Run workflow → Run.

## Instalar en Android

1. Ajustes → Seguridad → Orígenes desconocidos → Activar.
2. Abre el APK desde el gestor de archivos.
3. Instalar → Abrir.

## Características

- **Pantalla de inicio animada** con el logo de Thalia Streaming: entra con un leve zoom, un brillo que respira detrás y una barra de carga; desaparece con un fundido en cuanto la página está lista (mínimo 0.9 s, máximo 10 s).
- **Barra de progreso** con degradado cian → violeta → rosa que avanza con suavidad y se desvanece al terminar.
- **Transición entre páginas**: la página actual se atenúa mientras carga la siguiente.
- **Barras del sistema que siguen el tema**: la barra de estado y la de navegación cambian de color (con transición) según el tema que elija el admin, incluidos los temas claros, con íconos oscuros para que se lean.
- **Pantalla sin conexión animada** con botón de reintentar y reintento automático a los 10 segundos.
- Respeta la opción del sistema "Quitar animaciones".
- Pantalla completa (sin barra del navegador).
- La sesión se mantiene (localStorage).
- Botón atrás = navegación web.
- Los enlaces externos (por ejemplo WhatsApp) se abren en su app.
