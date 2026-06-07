# Guía paso a paso: Publicar PhoneOptimizer en Google Play

## ANTES DE EMPEZAR — Requisitos

- Cuenta de Google (Gmail)
- Tarjeta de crédito/débito para el pago único de registro (USD $25)
- APK firmada con una keystore de producción (NO la debug.keystore)
- Tiempo estimado: 2-4 horas para configuración inicial + 1-7 días de revisión

---

## PASO 1 — Crear cuenta de desarrollador en Google Play Console

1. Ve a https://play.google.com/console
2. Inicia sesión con tu cuenta de Google
3. Haz clic en "Comenzar"
4. Completa el formulario de registro:
   - Nombre de desarrollador (público, visible en la tienda): `Enmanuel Gil`
   - Correo electrónico de contacto
   - Número de teléfono
5. Acepta los Términos del Servicio
6. Paga el registro único de **USD $25** con tarjeta
7. Espera la confirmación por email (puede tardar 24-48 horas)

---

## PASO 2 — Generar keystore de producción (OBLIGATORIO)

La debug.keystore NO sirve para Google Play. Debes generar una keystore permanente.

**⚠️ GUARDA ESTA KEYSTORE PARA SIEMPRE — si la pierdes, no podrás actualizar la app.**

### Generar la keystore:
```bash
keytool -genkey -v -keystore phoneoptimizer.jks -keyalg RSA -keysize 2048 -validity 10000 -alias phoneoptimizer
```

Te pedirá:
- Contraseña del keystore (elige una fuerte, guárdala)
- Nombre y apellido
- Unidad organizativa (puedes poner "Personal")
- Organización (puedes poner tu nombre)
- Ciudad, Estado, País (CO para Colombia)
- Contraseña del alias (puede ser la misma)

Copia el archivo `phoneoptimizer.jks` a un lugar seguro (pendrive, nube privada).

### Configurar en build.gradle.kts:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../phoneoptimizer.jks")
            storePassword = "TU_CONTRASEÑA"
            keyAlias = "phoneoptimizer"
            keyPassword = "TU_CONTRASEÑA_ALIAS"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

### Compilar APK firmada de producción:
```bash
cd PhoneOptimizer
gradlew.bat assembleRelease
# APK en: app/build/outputs/apk/release/app-release.apk
```

---

## PASO 3 — Crear la app en Play Console

1. En Play Console → clic en **"Crear app"**
2. Completa:
   - **Nombre de la app:** `PhoneOptimizer`
   - **Idioma predeterminado:** Español (España) o Español (Latinoamérica)
   - **Tipo:** App
   - **Gratis o de pago:** Gratis
3. Acepta las políticas de desarrollador
4. Haz clic en **"Crear app"**

---

## PASO 4 — Configurar información de la app

### 4.1 Ficha de Play Store (Presencia en la tienda)

Ve a **Crecimiento → Ficha de Play Store**

**Título de la app (máx. 30 caracteres):**
```
PhoneOptimizer
```

**Descripción corta (máx. 80 caracteres):**
```
Optimiza RAM, temperatura y rendimiento de tu Android en un toque.
```

**Descripción completa (máx. 4000 caracteres):**
```
PhoneOptimizer es la app de optimización más completa para Android, sin root, sin publicidad y completamente gratuita.

🔥 PROBLEMAS QUE RESUELVE:
• Teléfono lento o con lag
• Sobrecalentamiento constante
• Poca memoria RAM disponible
• Apps que se cierran solas
• Batería que dura poco

⚡ FUNCIONES PRINCIPALES:

Panel de Control en tiempo real
• Métricas circulares animadas de CPU, RAM y temperatura
• Estado térmico con alertas visuales (Normal → Tibio → Caliente → Crítico)
• Uso de almacenamiento y memoria virtual (Swap)
• Contador de procesos activos

5 Perfiles de Optimización
• Recomendado: balance óptimo para uso diario
• Rendimiento: máxima fluidez para juegos
• Ahorro de Batería: extiende la duración al máximo
• Protección Térmica: enfría el dispositivo activamente
• Personalizado: configura cada ajuste manualmente

Monitor Térmico en Background
• Monitoreo cada 10 segundos
• Alerta automática al superar 45°C
• Auto-optimización al detectar temperatura severa
• Inicio automático al encender el teléfono

Panel de Apps
• Ve qué apps consumen más RAM en tiempo real
• Detén apps individuales con un toque
• Historial completo de todas tus optimizaciones

Bloqueo de Anuncios (con setup ADB)
• Configura DNS privado a AdGuard automáticamente
• Bloquea anuncios en TODAS las apps del sistema
• Sin instalar apps adicionales

🔒 100% SEGURA Y PRIVADA:
• No recopila ningún dato personal
• No requiere conexión a internet
• No modifica fotos, contactos ni archivos
• Código fuente abierto en GitHub
• Sin publicidad

📱 COMPATIBILIDAD:
• Android 8.0 o superior
• Samsung Galaxy ✅
• Motorola ✅
• Google Pixel ✅
• OnePlus / realme ✅
• Xiaomi (con limitaciones en MIUI V14/HyperOS)

Desarrollado por Enmanuel Gil
```

### 4.2 Capturas de pantalla

Necesitas mínimo **2 capturas** por tipo de dispositivo. Tamaños requeridos:
- **Teléfono:** mínimo 320px de ancho, máximo 3840px
- Formato: JPG o PNG, sin alpha transparente

Toma capturas del S21 mostrando:
1. Panel de Control con métricas
2. Pestaña Optimizar
3. Pestaña Apps (Top Apps)
4. Ajustes con botón de donación

Para tomar capturas via ADB:
```bash
adb -s R5CY93Z4X4E shell screencap /sdcard/screen1.png
adb -s R5CY93Z4X4E pull /sdcard/screen1.png C:\Users\usuario\Desktop\screen1.png
```

### 4.3 Ícono de la app
- Tamaño: **512 × 512 px**, PNG, sin transparencia en los bordes
- El ícono actual en `mipmap-xxxhdpi` es 192×192 — necesitás uno de 512×512

### 4.4 Banner de funciones (opcional pero recomendado)
- Tamaño: **1024 × 500 px**, JPG o PNG

---

## PASO 5 — Clasificación del contenido

Ve a **Política → Clasificación del contenido**

1. Completa el cuestionario:
   - Categoría: **Utilidades**
   - Violencia: No
   - Sexualidad: No
   - Lenguaje: No
   - Drogas: No
2. Envía y espera la calificación automática (PEGI 3 / Todos)

---

## PASO 6 — Público objetivo y contenido

Ve a **Política → Público objetivo y contenido**

- Edad mínima del público: **18+** (o 13+ si no hay contenido adulto)
- La app NO está dirigida a niños

---

## PASO 7 — Política de privacidad (OBLIGATORIO)

Google Play requiere una URL de política de privacidad.

### Opción A — Usar GitHub Pages (gratis):
1. Crea un archivo `PRIVACY_POLICY.md` en el repositorio
2. Ve a Settings → Pages → Enable GitHub Pages desde `main`
3. La URL será: `https://enmanuel-g.github.io/PhoneOptimizer/PRIVACY_POLICY`

### Contenido de la política de privacidad:
```
POLÍTICA DE PRIVACIDAD — PhoneOptimizer

Última actualización: junio 2026

PhoneOptimizer ("la app") es desarrollada por Enmanuel Gil ("el desarrollador").

DATOS QUE NO RECOPILAMOS:
Esta app NO recopila, almacena, transmite ni comparte ningún dato personal.
No hay servidores propios. No hay analíticas. No hay publicidad.

PERMISOS UTILIZADOS:
• KILL_BACKGROUND_PROCESSES: Para liberar RAM
• FOREGROUND_SERVICE: Para el monitor térmico en background  
• POST_NOTIFICATIONS: Para alertas de temperatura
• RECEIVE_BOOT_COMPLETED: Para inicio automático
• WRITE_SECURE_SETTINGS (opcional, via ADB): Para animaciones, WiFi scan y DNS

DATOS LOCALES:
El historial de optimizaciones se guarda exclusivamente en el dispositivo del usuario
en SharedPreferences y se elimina al desinstalar la app.

CONTACTO:
Para preguntas sobre privacidad: [tu email]

Esta política puede actualizarse. Los cambios se publicarán en esta misma URL.
```

---

## PASO 8 — Subir el APK / App Bundle

Ve a **Lanzamiento → Producción → Crear nueva versión**

### Recomendado: App Bundle (AAB) en lugar de APK
```bash
cd PhoneOptimizer
gradlew.bat bundleRelease
# Bundle en: app/build/outputs/bundle/release/app-release.aab
```
Firma el bundle igual que el APK.

### Subir:
1. Arrastra el archivo `.aab` o `.apk` a la consola
2. Escribe las notas de la versión:

**Notas de versión v1.4.0 (español):**
```
• Nuevo: botón de Optimización Rápida en el Panel de Control
• Nuevo: soporte en inglés (cambia según el idioma del dispositivo)
• Fix: CPU ahora muestra el porcentaje real de uso
• Fix: Panel de Apps muestra todas las apps en Android 12+
• Fix: contador de procesos activos ahora refleja la realidad
• Bloqueo de anuncios DNS integrado
• Historial de optimizaciones
```

**Notas de versión v1.4.0 (inglés):**
```
• New: Quick Optimize button on the Control Panel
• New: English language support (follows device language)
• Fix: CPU now shows real usage percentage
• Fix: Apps tab shows all apps on Android 12+
• Fix: Process count now reflects actual running processes
• Built-in DNS ad blocking
• Optimization history
```

3. Haz clic en **"Guardar"** → **"Revisar versión"** → **"Comenzar lanzamiento en producción"**

---

## PASO 9 — Revisión de Google (1-7 días)

- Google revisará la app manualmente
- Recibirás un email con el resultado
- Si hay problemas, recibirás instrucciones específicas para corregirlos
- Una vez aprobada, aparece en la tienda en 1-2 horas

---

## POSIBLES RECHAZOS Y CÓMO EVITARLOS

| Razón común | Solución |
|-------------|----------|
| Sin política de privacidad | Añadir URL de política (Paso 7) |
| Permisos sin justificar | En la consola, justifica cada permiso |
| Ícono de baja calidad | Usar PNG 512×512 de alta resolución |
| Capturas de baja calidad | Mínimo 1080p, sin marcos de teléfono borrosos |
| Funcionalidad que requiere root | La app NO requiere root — mencionar esto |

---

## CHECKLIST FINAL

- [ ] Cuenta de desarrollador creada y pagada
- [ ] Keystore de producción generada y guardada en lugar seguro
- [ ] APK/Bundle firmado con keystore de producción
- [ ] Capturas de pantalla (mínimo 2, recomendado 4-6)
- [ ] Ícono 512×512 PNG
- [ ] Política de privacidad publicada en URL pública
- [ ] Descripción completa escrita
- [ ] Clasificación de contenido completada
- [ ] Notas de versión escritas en español e inglés
- [ ] App subida y enviada a revisión

---

## DESPUÉS DE LA PUBLICACIÓN

1. **Compartir el enlace:** `https://play.google.com/store/apps/details?id=com.enmanuelgil.optimizer`
2. **Actualizar DESCRIPTION_ES.md** con el nuevo enlace de Play Store
3. **Actualizar README.md** con el badge de Play Store
4. Para futuras actualizaciones: subir nueva versión con `versionCode` incrementado

---

*Tiempo total estimado: 3-5 horas de trabajo + hasta 7 días de revisión de Google*
