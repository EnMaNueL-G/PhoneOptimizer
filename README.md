# PhoneOptimizer

**Desarrollado por Enmanuel Gil**
Versión 1.5.0 | Android 8.0+ (API 26) | Sin dependencias externas

Aplicación Android de optimización avanzada de rendimiento y temperatura. Analiza, detecta y corrige automáticamente los problemas de sobrecalentamiento y lentitud del dispositivo en tiempo real.

---

## Características

### Panel de Control en Tiempo Real
- Métricas circulares animadas: CPU, RAM, Temperatura
- Estado térmico con alertas visuales (Normal → Tibio → Caliente → Crítico → Emergencia)
- Uso de almacenamiento y memoria virtual (Swap)
- Contador de procesos activos en el sistema

### Perfiles de Optimización
| Perfil | Uso recomendado |
|--------|----------------|
| **Recomendado** | Uso diario — balance rendimiento/batería |
| **Rendimiento** | Juegos o apps exigentes |
| **Ahorro de Batería** | Extender duración al máximo |
| **Protección Térmica** | Corregir sobrecalentamiento activo |
| **Personalizado** | Configuración manual de cada ajuste |

### Optimizaciones aplicadas
- Detener procesos en segundo plano (RAM)
- Reducir animaciones del sistema (0.5x)
- Restringir datos en segundo plano por app
- Desactivar WiFi scan pasivo y BLE scan
- Pausar sincronización automática de Google
- Activar modo Doze profundo (ahorro energético)
- Garbage Collection y trim de memoria
- Limpiar caché DNS del sistema
- Reiniciar cuota de tareas diferidas (JobScheduler)

### Bloqueo de Anuncios — DNS Privado

PhoneOptimizer incluye un **filtro de anuncios a nivel de sistema** que no requiere instalar ninguna app adicional. Funciona configurando el DNS privado del dispositivo al servidor de AdGuard.

**¿Cómo funciona?**
Cuando activás el toggle en Ajustes → "Bloquear anuncios del sistema", la app ejecuta:
```
Settings.Global → private_dns_mode = "hostname"
Settings.Global → private_dns_specifier = "dns.adguard.com"
```
El sistema operativo redirige **todas las consultas DNS** de todas las apps a través del servidor de AdGuard, que filtra dominios de anuncios y rastreadores conocidos antes de que lleguen al dispositivo.

**¿Qué bloquea?**
- Anuncios en browsers (Chrome, Firefox, Samsung Internet)
- Anuncios en apps (YouTube, apps de noticias, juegos)
- Rastreadores de datos de terceros (Meta Pixel, Google Analytics, etc.)
- Dominios de malware conocidos

**Requisitos:**
- Permisos avanzados activos (comando ADB)
- Conexión a internet (para que el DNS resuelva consultas legítimas)

**Para desactivar:** vuelve a tocar el toggle. La app restaura el modo DNS por defecto (`opportunistic`).

**Privacidad del servidor DNS:** AdGuard no registra consultas DNS. Política oficial en: https://adguard-dns.io/es/privacy.html

> **Alternativa manual** (sin la app): Ajustes → Conexiones → Más ajustes de conexión → DNS privado → Hostname: `dns.adguard.com`

### Panel de Aplicaciones (Top Apps)

La pestaña **Apps** muestra en tiempo real cuáles apps están consumiendo más RAM, usando dos métodos combinados:
1. `ActivityManager.getRunningAppProcesses()` — API estándar de Android
2. Lectura directa de `/proc/<pid>/status` (VmRSS) — cubre Android 12+ donde la API está restringida

Cada app muestra su consumo en MB con barra de progreso coloreada:
- 🟢 Bajo consumo (< 150 MB)
- 🟠 Consumo moderado (150–300 MB)
- 🔴 Alto consumo (> 300 MB)

### Monitor Térmico en Background
- Monitoreo cada 10 segundos
- Notificación persistente con temperatura en tiempo real
- Alerta automática al superar 45°C
- Auto-optimización al detectar temperatura severa
- Se inicia automáticamente al encender el dispositivo

---

## Instalación

### Requisitos mínimos
- Android 8.0 (API 26) o superior
- 5 MB de espacio libre
- **No requiere root**
- **No requiere apps de terceros**

### Compatibilidad con Tablets

PhoneOptimizer detecta automáticamente si se está ejecutando en una tablet y adapta su interfaz:

| Dispositivo | Navegación | Diseño |
|-------------|-----------|--------|
| **Teléfono** | Barra inferior | Pantalla completa vertical |
| **Tablet / pantalla grande** | Rail lateral izquierdo | Aprovecha el ancho extra |

No hace falta configurar nada — la app se adapta sola al tamaño de pantalla. Funciona en tablets Samsung, Xiaomi Pad, Lenovo, Huawei MatePad y cualquier Android con pantalla mediana o grande.

### Pasos de instalación
1. Descarga `PhoneOptimizer-v1.1.0.apk`
2. En el teléfono: **Ajustes → Seguridad → Instalar apps de origen desconocido → Activar**
3. Abre el archivo APK desde el administrador de archivos
4. Toca "Instalar"
5. Abre la app

**La app funciona inmediatamente** — libera RAM y optimiza procesos sin ninguna configuración extra.

---

## Optimización Avanzada (Opcional)

Para desbloquear animaciones, WiFi scan, Doze y restricción de datos, activá los permisos avanzados **una sola vez**. Hay dos métodos:

---

### Método 1 — Autoinstalador (recomendado para la mayoría de usuarios)

Descargá el archivo `Activar_Optimizacion_Avanzada.bat` desde la sección [Releases](https://github.com/EnMaNueL-G/PhoneOptimizer/releases).

**¿Qué hace el .bat?**
- Busca ADB automáticamente en el PC
- Si no lo encuentra, lo descarga de los servidores oficiales de Google
- Espera a que conectes el teléfono con USB
- Detecta automáticamente si el dispositivo es MIUI V14 (bloqueado por el fabricante)
- Ejecuta el comando de activación y muestra el resultado en español claro

**Pasos:**
1. Descargá `Activar_Optimizacion_Avanzada.bat`
2. Doble clic para ejecutarlo
3. Activá la depuración USB en el teléfono (ver instrucciones abajo)
4. Conectá el teléfono al PC con cable USB
5. En el teléfono, tocá **"Permitir"** cuando aparezca el mensaje de depuración USB
6. Esperá el mensaje de confirmación ✓

#### Cómo activar la Depuración USB (paso a paso)

> Si ya la tenés activa, saltate este paso.

**Android estándar (Samsung, Motorola, Pixel, OnePlus):**
1. Abrí **Ajustes** → **Acerca del teléfono**
2. Tocá **"Número de compilación"** 7 veces seguidas hasta ver "Ya eres desarrollador"
3. Volvé a **Ajustes** → **Opciones de desarrollador**
4. Activá el toggle principal (arriba de todo)
5. Buscá **"Depuración USB"** y activala
6. Conectá el teléfono al PC — aparecerá un popup: tocá **"Permitir"**

**Xiaomi (MIUI / HyperOS):**
1. **Ajustes** → **Mi teléfono** → tocá **"Versión de MIUI"** 7 veces
2. Volvé a **Ajustes** → **Ajustes adicionales** → **Opciones de desarrollador**
3. Activá **"Depuración USB"**

**¿No aparece el popup en el teléfono?**
Desconectá y reconectá el cable — asegurate de que el modo de conexión sea **"Transferencia de archivos (MTP)"** y no solo "Carga".

---

### Método 2 — Comando ADB manual (para usuarios avanzados)

Si ya tenés ADB instalado y preferís hacerlo directamente:

#### Requisitos
- PC con ADB instalado (Android SDK Platform Tools)
- Cable USB + depuración USB activa en el teléfono

#### Comando único
Con el teléfono conectado al PC:

```bash
adb shell pm grant com.enmanuelgil.optimizer android.permission.WRITE_SECURE_SETTINGS
```

**Este permiso es permanente** — no se pierde al reiniciar el teléfono.

---

### Dispositivos compatibles con optimización avanzada
- Samsung Galaxy (todos los modelos) ✅
- Xiaomi con MIUI estándar ✅
- Xiaomi con MIUI V14 / HyperOS ⚠️ *(pm grant bloqueado por el fabricante)*
- Motorola ✅
- Google Pixel ✅
- OnePlus / realme ✅

> **Nota MIUI V14:** Xiaomi con HyperOS bloquea `pm grant` desde ADB.
> La optimización básica (RAM + procesos) funciona igual sin el comando.

---

## Compilar desde el código fuente

### Requisitos
- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17 (incluido en Android Studio)
- Android SDK API 34

### Compilar APK debug
```bash
cd PhoneOptimizer
gradlew.bat assembleDebug
# APK en: app/build/outputs/apk/debug/app-debug.apk
```

### Variables de entorno necesarias
```
JAVA_HOME = C:\Program Files\Android\Android Studio\jbr
ANDROID_HOME = C:\Users\<usuario>\AppData\Local\Android\Sdk
```

---

## Errores conocidos por fabricante

### ❌ Xiaomi MIUI V14 / HyperOS — el comando ADB falla

**Síntoma:** al ejecutar el comando de activación aparece:
```
Exception: grantRuntimePermission: Neither user 2000 nor current process
has android.permission.GRANT_RUNTIME_PERMISSIONS.
```

**Causa:** Xiaomi bloquea en MIUI V14 y HyperOS la capacidad de que ADB shell otorgue permisos avanzados. No es un fallo de la app ni del comando — es una restricción impuesta por el fabricante.

**Impacto:** las funciones avanzadas (animaciones, WiFi scan, Doze, sync) no se pueden activar en estos modelos. La optimización básica (RAM, procesos, GC) funciona sin ningún problema.

**No tiene solución** sin root o sin cambiar la ROM del dispositivo.

**Dispositivos confirmados SIN este problema:**
- Samsung Galaxy (todos los modelos, Android 8–15) ✅
- Motorola ✅
- Google Pixel ✅
- OnePlus / realme ✅
- Xiaomi con MIUI estándar (versiones anteriores a V14) ✅

---

## Resolución de problemas

### La app se cierra al abrir (crash)
- **Causa en Android 14+:** El servicio no tiene permiso para `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`
- **Solución:** Corregido en v1.0.0 — usa `dataSync` compatible con todas las versiones

### Las notificaciones no aparecen (Samsung)
- Ve a **Ajustes → Aplicaciones → PhoneOptimizer → Notificaciones → Activar todas**

### El servicio de monitor se detiene solo (Samsung/Xiaomi)
- **Ajustes → Mantenimiento del dispositivo/Batería → PhoneOptimizer → Sin restricciones**
- En Xiaomi: Ajustes → Apps → PhoneOptimizer → Batería → Sin restricciones → Activar inicio automático

---

## Arquitectura del proyecto

```
PhoneOptimizer/
├── app/src/main/java/com/enmanuelgil/optimizer/
│   ├── MainActivity.kt              — Actividad principal + navegación
│   ├── OptimizerApp.kt              — Application class
│   ├── core/
│   │   ├── SystemMonitor.kt         — Lectura de CPU, RAM, temperatura, batería
│   │   ├── OptimizationEngine.kt    — Motor de todas las optimizaciones
│   │   └── PrivilegedHelper.kt      — Acceso a Settings.Global + shell exec
│   ├── model/
│   │   ├── DeviceStats.kt           — Modelo de métricas del dispositivo
│   │   └── OptimizationProfile.kt   — Perfiles de configuración
│   ├── service/
│   │   ├── ThermalMonitorService.kt — Servicio foreground de monitoreo
│   │   └── BootReceiver.kt          — Auto-inicio al encender
│   ├── viewmodel/
│   │   └── MainViewModel.kt         — Lógica de negocio y estado UI
│   └── ui/
│       ├── theme/Theme.kt           — Tema oscuro personalizado
│       └── screens/
│           ├── DashboardScreen.kt   — Panel de métricas en tiempo real
│           ├── OptimizeScreen.kt    — Perfiles y botón de optimización
│           └── SettingsScreen.kt    — Configuración y comando ADB
├── gradle.properties                — AndroidX, JVM config
└── README.md                        — Este archivo
```

---

## Compatibilidad probada

| Dispositivo | Android | Básico | Avanzado |
|-------------|---------|--------|----------|
| Samsung Galaxy S21 | Android 15 (API 35) | ✅ | ✅ |
| Samsung Galaxy J7 Prime | Android 8.1 (API 27) | ✅ | ✅ |
| Xiaomi Redmi Note 12 | Android 13 (API 33) | ✅ | ⚠️ MIUI V14 |
| Xiaomi Redmi Note 12 | Android 14 (API 34) | ✅ | ✅ |

---

## Seguridad y Privacidad

- **No recopila ningún dato personal**
- **No requiere conexión a internet**
- **No modifica archivos de usuario** (fotos, documentos, contactos)
- **Sin dependencias de terceros** — 100% APIs nativas de Android
- Solo accede a métricas del sistema (CPU, RAM, temperatura, batería)

---

## Permisos

| Permiso | Por qué | Sin él |
|---------|---------|--------|
| `KILL_BACKGROUND_PROCESSES` | Liberar RAM | Funciones básicas limitadas |
| `FOREGROUND_SERVICE` | Monitor en background | Sin monitoreo continuo |
| `FOREGROUND_SERVICE_DATA_SYNC` | Tipo de servicio Android 14+ | Crash en Android 14+ |
| `POST_NOTIFICATIONS` | Alertas de temperatura | Sin alertas |
| `RECEIVE_BOOT_COMPLETED` | Auto-inicio | Hay que abrir la app manualmente |
| `WRITE_SECURE_SETTINGS` *(ADB, opcional)* | Animaciones, WiFi scan, Doze | Optimización básica únicamente |

---

## Apoya el Proyecto

PhoneOptimizer es **gratuita, sin anuncios y de código abierto**. Si mejoró el rendimiento de tu dispositivo, podés apoyar su desarrollo:

**Binance Pay ID: `1140153333`**
Pasos: abre Binance → Pagar → Buscar → pegá el Pay ID.

**Cripto directo — BSC BEP20 (Binance Smart Chain):**
`0x0a9a0d8d816ede885d1d4a5c94369a72ef86b3c1`
Compatible con BNB, USDT, USDC y cualquier token BEP20.

---

## Créditos

**Desarrollado por:** Enmanuel Gil
**UI:** Jetpack Compose con Material Design 3 — tema oscuro personalizado
**Compatibilidad:** Android 8.0 — Android 15

---

*PhoneOptimizer v1.5.0 — Sin dependencias, sin compromisos*
