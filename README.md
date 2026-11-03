#  Scently - Boutique de Fragancias Exclusivas
> **Aplicación Móvil Android - Momento 3**  
> Proyecto desarrollado con arquitectura nativa en Java, Firebase y OpenStreetMap.

---

## Descripción del Proyecto
**Scently** es una solución móvil integral diseñada para la gestión de catálogo, inventario y ventas de perfumería fina. Combina una experiencia de usuario bajo una estética *Boutique* (paleta Ciruela `#4A1E35` y Champagne `#D4A373`) con un backend serverless, capacidades de geolocalización en tiempo real y comunicación mediante notificaciones push y locales.

---

## Cumplimiento de los 5 Pilares Arquitectónicos

### A. Autenticación y Control de Acceso por Roles (RBAC)
- **Firebase Authentication:** Gestión de sesiones seguras por correo/contraseña, persistencia en `SharedPreferences` y cierre de sesión seguro (`signOut`).
- **Seguridad y Roles:**
  - `Administrador`: Acceso total para crear, editar, eliminar y consultar inventario, mapas y configuraciones.
  - `Vendedor`: Vista en modo solo lectura del catálogo, sin acceso a formularios ni botones de modificación.
- **Reglas de Seguridad Server-side:** Reglas activas en Cloud Firestore que restringen las operaciones de escritura y eliminación exclusivamente a usuarios con rol `Administrador`.

### B. Cloud Firestore, CRUD Completo e Imágenes
- **Sincronización Reactiva:** Escucha en tiempo real mediante `addSnapshotListener` para mantener el catálogo actualizado al instante.
- **Operaciones CRUD:**
  - Registro con código alfanumérico, descripción, precio formateado (`$ #,###`) e indicador de ofertas.
  - Edición y eliminación vinculadas directamente por documento en Firestore.
  - Filtro reactivo para aislar productos en promoción.
- **Procesamiento de Imágenes:** Captura con cámara y galería, compresión y almacenamiento en Firebase Storage con renderizado optimizado en Glide y respaldo en Base64.
- **Consumo de API REST Externa:** Integración con la categoría de fragancias de **DummyJSON** (`dummyjson.com/products/category/fragrances`) para la sugerencia y autocompletado inteligente de productos.

### C. Geolocalización y Mapas en Tiempo Real
- **OpenStreetMap (OSMDroid):** Motor de mapas independiente de alta disponibilidad con capas de calles y satelital mundial (**Esri World Imagery**).
- **GPS en Vivo:** Seguimiento en tiempo real con `MyLocationNewOverlay` y botón de centrado.
- **Coordenadas asociadas al inventario:** Captura de ubicación precisa con `FusedLocationProviderClient` al guardar o editar, mostrando marcadores interactivos en el mapa con título y precio.

### D. Sistema de Notificaciones
- **Notificaciones Locales:** Canales dedicados con prioridad alta que alertan en la bandeja del sistema al crear o actualizar productos (compatible con Android 13+).
- **Firebase Cloud Messaging (FCM):** Servicio en segundo plano (`MiServicioFCM`) que procesa cargas útiles remotas y construye alertas visibles en pantalla con `NotificationCompat.Builder`.

### E. Diseño, Usabilidad y UX Adaptativa
- **Identidad Visual:** Tema claro forzado, paleta Ciruela y Champagne, formularios con `TextInputLayout`, layouts responsivos y soporte para orientación horizontal (`layout-land`).

---

##  Credenciales de Prueba para Evaluación

| Rol | Correo Electrónico | Contraseña | Permisos |
| :--- | :--- | :--- | :--- |
| **Administrador** | `admin@scently.com` | `123456` | CRUD total, carga de imágenes, mapas y configuración |
| **Vendedor** | `jersy@scently.com` | `123456` | Solo lectura del catálogo, consulta de inventario |

*(Nota: actualiza la columna de contraseñas con las claves reales asignadas en Firebase Auth).*

---

## Stack Tecnológico
- **Lenguaje:** Java
- **Plataforma:** Android SDK
- **Backend:** Firebase (Auth, Firestore, Storage, Cloud Messaging)
- **Mapas:** OSMDroid, Esri World Imagery, Fused Location Provider
- **Redes / API REST:** DummyJSON Fragrances API
