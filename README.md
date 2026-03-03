# 📱 Streaming Thalia - App Android

App nativa que envuelve tu sistema web de rentas en una WebView.

## ⚠️ Antes de compilar

Abre `app/src/main/java/com/streamingthalia/app/MainActivity.java` y cambia:

```java
private static final String SERVER_URL = "http://TU_IP:8081";
```

Por la IP real de tu servidor, ejemplo:

```java
private static final String SERVER_URL = "http://192.168.1.100:8081";
```

## 🚀 Cómo obtener el APK

1. Sube este proyecto a un repositorio en GitHub
2. Ve a la pestaña **Actions**
3. El workflow **Build APK** corre automáticamente al hacer push
4. Cuando termine (3-5 minutos), ve a la corrida y baja **StreamingThalia** en Artifacts
5. Descomprime el ZIP → dentro está el `app-debug.apk`
6. Pásalo a tu celular e instálalo

### Para correrlo manualmente:
- Ve a Actions → Build APK → Run workflow → Run

## 📲 Instalar en Android
1. Ajustes → Seguridad → Orígenes desconocidos → Activar
2. Abrir el APK desde el gestor de archivos
3. Instalar → Abrir

## Características
- Pantalla completa (sin barra del navegador)
- Tema oscuro cyberpunk en status bar y nav bar
- Sesión se mantiene (localStorage)
- Botón atrás = navegación web
- Pantalla de error si no hay conexión
- Barra de progreso al cargar
