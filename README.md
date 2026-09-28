# PhoneOptimizer

**Desarrollado por Enmanuel Gil · OptiSuite**
Versión 1.7.0 | Android 8.0+ (API 26) | Sin root | Sin anuncios | Gratis

PhoneOptimizer muestra con claridad el estado real de tu teléfono Android (memoria, almacenamiento, temperatura, batería) y te da recomendaciones concretas cuando algo necesita atención. Solo cambia ajustes del sistema que se pueden comprobar y deshacer, y siempre te dice qué hizo.

La app no borra datos ni archivos, no tiene permiso de internet y no hace nada automático sin que lo actives tú.

---

## Qué hace

### Panel
- RAM en uso y RAM disponible
- Almacenamiento usado y libre
- Temperatura de la batería
- Temperatura del procesador, si el fabricante permite leerla
- Nivel de batería y estado de carga
- Tiempo sin reiniciar (horas o días)
- Memoria comprimida (swap)
- Estado térmico del sistema (Android 10 o superior)

Incluye una nota que explica que **tener la RAM casi llena es normal en Android**: el sistema la usa para abrir las apps más rápido y la libera solo cuando hace falta.

### Recomendaciones automáticas
El Panel muestra avisos solo cuando detecta algo concreto:

| Situación | Qué te propone |
|-----------|----------------|
| Almacenamiento al 80 % o más (aviso más fuerte al 90 %) | Botón para abrir los Ajustes de almacenamiento |
| 7 días o más sin reiniciar | Instrucciones para reiniciar |
| Teléfono caliente, o caliente mientras carga | Consejos concretos y botón a las opciones de ahorro de batería |
| Salud de la batería mala | Aviso sobre el estado de la batería |
| Batería baja | Aviso para cargar |

### Pestaña Optimizar
Los perfiles cambian **solo ajustes reales del sistema** y necesitan el [modo avanzado](#modo-avanzado-opcional):

| Perfil | Qué cambia |
|--------|-----------|
| **Recomendado** | Animaciones a 0.5x |
| **Máxima agilidad** | Sin animaciones |
| **Ahorro de batería** | Animaciones a 0.5x y sin búsqueda de redes WiFi/Bluetooth cuando el WiFi está apagado |

Cada cambio se comprueba después de aplicarlo, y la app guarda el valor que tenías antes. El botón **"Restaurar animaciones y ajustes originales"** deja todo como estaba.

**Cerrar apps en segundo plano (solo Android 13 o anterior):** en esas versiones aparece un botón que pide al sistema cerrar las apps en segundo plano. Algunos fabricantes ignoran esa petición. Desde Android 14, Google no permite que ninguna app cierre otras apps (está en su documentación oficial), por eso en Android 14 o superior esta opción no aparece.

### Pestaña Apps
- Tiempo en pantalla de cada app en las últimas 24 horas. Necesita el permiso **"Acceso a datos de uso"**, que concedes tú desde los ajustes de Android.
- Al tocar una app se abre su ficha de Android, donde puedes forzar la detención, borrar su caché, restringir su uso de batería o desinstalarla.
- Historial de los perfiles que has aplicado.

### Ajustes

**Monitor de temperatura** (apagado por defecto)
- Revisa la temperatura cada minuto, solo mientras la pantalla está encendida.
- Si la batería pasa de 42 °C, te avisa con consejos.
- Mientras está activo muestra una notificación fija discreta.
- No hace nada automático: solo avisa.

**Revisión automática** (apagada por defecto)
- Puedes elegir cada 12 horas, cada día o cada 3 días.
- Solo te notifica si encuentra almacenamiento casi lleno, muchos días sin reiniciar o problemas de batería.
- No cierra apps ni cambia nada.

**Bloqueo de anuncios con DNS privado** (necesita el modo avanzado)
- Configura el DNS privado de Android con el servidor de AdGuard (`dns.adguard-dns.com`), que filtra muchos dominios de anuncios y rastreadores en navegadores y apps.
- **No quita los anuncios de YouTube.**
- Mientras está activo, las consultas DNS del teléfono pasan por AdGuard. Política de privacidad de AdGuard: https://adguard-dns.io/es/privacy.html
- En algunas redes (hoteles, empresas, wifi con página de inicio de sesión) puede dejarte sin internet. Si pasa, desactívalo con el mismo interruptor.
- Al desactivarlo, la app devuelve el DNS que tenías antes.

---

## Lo que ninguna app puede hacer sin root

Para que no haya confusiones, esto **no lo hace PhoneOptimizer ni ninguna otra app sin root**, aunque algunas lo prometan:

- Cerrar otras apps en Android 14 o superior
- Borrar la caché de otras apps (cada app se limpia desde su propia ficha en Ajustes)
- "Enfriar" el procesador
- Aumentar la memoria RAM

---

## Instalación

### Requisitos
- Android 8.0 (API 26) o superior
- No requiere root
- No requiere otras apps

### Pasos
1. Descarga el APK de PhoneOptimizer 1.7.0 desde https://optisuite.app
2. En el teléfono permite instalar apps de origen desconocido (Android te lo pedirá al abrir el archivo).
3. Abre el APK desde el administrador de archivos y toca **"Instalar"**.
4. Abre la app.

El identificador de la app es `com.enmanuelgil.optimizer`, así que **se instala encima de versiones anteriores** sin perder nada.

En Android 13 o superior, la app te pedirá permiso para mostrar notificaciones. Solo lo necesita si activas el Monitor de temperatura o la Revisión automática.

El Panel, las recomendaciones y la pestaña Apps funcionan sin configurar nada más. Los perfiles de la pestaña Optimizar y el bloqueo de anuncios necesitan el modo avanzado.

---

## Modo avanzado (opcional)

Los perfiles y el bloqueo de anuncios cambian ajustes del sistema, y para eso Android exige un permiso especial (`WRITE_SECURE_SETTINGS`) que solo se puede conceder **una vez desde un PC**. No hace falta root.

### Método 1 — Archivo automático (recomendado)

Usa el archivo `Activar_Optimizacion_Avanzada.bat` incluido con la app.

**Qué hace:**
- Busca ADB en el PC y, si no lo encuentra, lo descarga de los servidores oficiales de Google.
- Si hay varios teléfonos conectados, te pregunta cuál usar.
- Concede el permiso y comprueba que quedó concedido.

**Pasos:**
1. Activa la depuración USB en el teléfono (ver abajo).
2. Conecta el teléfono al PC con un cable USB.
3. Haz doble clic en `Activar_Optimizacion_Avanzada.bat`.
4. En el teléfono, toca **"Permitir"** cuando aparezca el aviso de depuración USB.
5. Espera el mensaje de confirmación.

#### Cómo activar la depuración USB

Si ya la tienes activa, puedes pasar al siguiente paso.

**Samsung, Motorola, Pixel, OnePlus y la mayoría de marcas:**
1. Abre **Ajustes → Acerca del teléfono** (en Samsung: **Información de software**).
2. Toca **"Número de compilación"** 7 veces seguidas hasta ver un mensaje de que ya eres desarrollador.
3. Vuelve a **Ajustes → Opciones de desarrollador**.
4. Activa **"Depuración USB"**.
5. Al conectar el teléfono al PC aparecerá un aviso: toca **"Permitir"**.

**Xiaomi, Redmi y POCO (MIUI / HyperOS):**
1. **Ajustes → Sobre el teléfono** → toca **"Versión de MIUI"** o **"Versión de HyperOS"** 7 veces.
2. Ve a **Ajustes → Ajustes adicionales → Opciones de desarrollador**.
3. Activa **"Depuración USB"**.
4. Activa además **"Depuración USB (ajustes de seguridad)"**. Sin esta opción, Xiaomi no deja conceder el permiso.

**¿No aparece el aviso en el teléfono?**
Desconecta y vuelve a conectar el cable. Comprueba que el modo USB sea **"Transferencia de archivos"** y no solo "Carga".

### Método 2 — Comando ADB manual

Si ya tienes ADB instalado, con el teléfono conectado:

```bash
adb shell pm grant com.enmanuelgil.optimizer android.permission.WRITE_SECURE_SETTINGS
```

El permiso se mantiene aunque reinicies el teléfono.

### Quitar el modo avanzado

Antes de quitarlo, usa **"Restaurar animaciones y ajustes originales"** y desactiva el bloqueo de anuncios si lo tenías activo. Después:

```bash
adb shell pm revoke com.enmanuelgil.optimizer android.permission.WRITE_SECURE_SETTINGS
```

---

## Privacidad

- La app **no tiene permiso de internet**: no puede enviar nada a ningún sitio.
- Sin anuncios y sin cuentas.
- No borra datos ni archivos (fotos, documentos, contactos).
- Solo lee métricas del sistema (memoria, almacenamiento, temperatura, batería) y, si lo concedes, el tiempo de uso de las apps.
- Si activas el bloqueo de anuncios, las consultas DNS del teléfono pasan por el servidor de AdGuard (ver arriba).

---

## Permisos

| Permiso | Para qué | Sin él |
|---------|----------|--------|
| `PACKAGE_USAGE_STATS` *(lo concedes tú en Ajustes)* | Tiempo en pantalla por app en la pestaña Apps | La pestaña Apps no muestra el uso |
| `QUERY_ALL_PACKAGES` | Mostrar el nombre de las apps en la pestaña Apps | — |
| `KILL_BACKGROUND_PROCESSES` | "Cerrar apps en segundo plano" (solo Android 13 o anterior) | Esa opción no funciona |
| `POST_NOTIFICATIONS` | Avisos del Monitor de temperatura y de la Revisión automática (Android 13+) | Sin avisos |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Monitor de temperatura mientras está activado | Sin monitor |
| `RECEIVE_BOOT_COMPLETED` | Retomar el monitor o la revisión tras reiniciar, **solo si los activaste** | Hay que abrir la app tras reiniciar |
| `WRITE_SECURE_SETTINGS` *(opcional, desde PC)* | Perfiles y bloqueo de anuncios | Solo Panel, recomendaciones y Apps |

---

## Novedades en 1.7.0

- Quitadas las acciones que fallaban y aun así se mostraban como hechas.
- El monitor de temperatura y la revisión automática ya no se encienden solos y gastan mucha menos batería.
- Nuevo Panel con recomendaciones automáticas.
- Pestaña Apps con el tiempo de uso real de cada app.
- Botón para restaurar las animaciones y los ajustes originales.
- Permiso de notificaciones en Android 13 o superior.
- El bloqueo de anuncios devuelve tu DNS anterior al desactivarlo.

---

## Resolución de problemas

### No llegan las notificaciones
- Comprueba que concediste el permiso de notificaciones: **Ajustes → Aplicaciones → PhoneOptimizer → Notificaciones**.

### El monitor de temperatura se detiene solo (Samsung, Xiaomi y otros)
- Samsung: **Ajustes → Aplicaciones → PhoneOptimizer → Batería → Sin restricciones**.
- Xiaomi: **Ajustes → Apps → PhoneOptimizer → Ahorro de batería → Sin restricciones**, y activa el inicio automático.

### Sin internet después de activar el bloqueo de anuncios
- Desactiva el interruptor de bloqueo de anuncios en Ajustes de la app. Se restaura el DNS que tenías antes.

### El archivo .bat dice que no se pudo conceder el permiso
- Revisa que la depuración USB esté activa y que tocaste "Permitir" en el teléfono.
- En Xiaomi/Redmi/POCO, activa también "Depuración USB (ajustes de seguridad)".

---

## Compilar desde el código fuente

### Requisitos
- Android Studio con JDK 17
- Android SDK API 34

### Compilar APK debug
```bash
cd PhoneOptimizer
gradlew.bat assembleDebug
# APK en: app/build/outputs/apk/debug/app-debug.apk
```

### Estructura del proyecto

```
PhoneOptimizer/
├── app/src/main/java/com/enmanuelgil/optimizer/
│   ├── MainActivity.kt
│   ├── core/
│   │   ├── SystemMonitor.kt         — Lectura de RAM, almacenamiento, temperatura, batería
│   │   ├── HealthAdvisor.kt         — Recomendaciones del Panel
│   │   ├── OptimizationEngine.kt    — Aplicar y restaurar perfiles
│   │   ├── PrivilegedHelper.kt      — Acceso a ajustes del sistema (modo avanzado)
│   │   ├── AdBlockManager.kt        — DNS privado de AdGuard
│   │   ├── AppUsageMonitor.kt       — Tiempo de uso por app
│   │   └── HistoryManager.kt        — Historial de perfiles aplicados
│   ├── model/                       — DeviceStats, OptimizationProfile, OptimizationRecord
│   ├── service/
│   │   ├── ThermalMonitorService.kt — Monitor de temperatura
│   │   ├── AutoMaintenance.kt       — Revisión automática
│   │   └── BootReceiver.kt          — Retomar tras reiniciar (si estaba activado)
│   ├── viewmodel/MainViewModel.kt
│   └── ui/
│       ├── theme/Theme.kt
│       └── screens/                 — Dashboard, Optimize, Apps, Settings
├── Activar_Optimizacion_Avanzada.bat
└── README.md
```

---

## Apoya el proyecto

PhoneOptimizer es **gratuita y sin anuncios**. Si te resulta útil, puedes apoyar su desarrollo:

**Binance Pay ID: `1165745950`**
Pasos: abre Binance → Pagar → Buscar → pega el Pay ID.

**Cripto directo — BSC BEP20 (Binance Smart Chain):**
`0xb6f6731a4ea87f8e1fd6f44f48b5bc4204571f08`
Compatible con BNB, USDT, USDC y cualquier token BEP20.

---

## Contacto y créditos

**Desarrollado por:** Enmanuel Gil · OptiSuite
**Web:** https://optisuite.app
**Soporte:** support@optisuite.app
**UI:** Jetpack Compose con Material Design 3

---

*PhoneOptimizer v1.7.0 — Enmanuel Gil · OptiSuite*
