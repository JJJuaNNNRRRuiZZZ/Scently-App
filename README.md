# Scently - App Móvil de Gestión e Inventario de Perfumería

Proyecto desarrollado en Android Studio para la entrega del Momento 3. La aplicación gestiona el inventario de una boutique de fragancias integrando autenticación, base de datos en la nube con Firebase, captura multimedia, notificaciones y geolocalización de productos en mapa.

---

## Funcionalidades Implementadas

### 1. Autenticación y Control de Roles (RBAC)
* Inicio de sesión y registro de usuarios mediante **Firebase Authentication**.
* Control de acceso según el rol del usuario (`Administrador` o `Vendedor`) almacenado en Firestore:
  * **Administrador:** Acceso completo para registrar productos, editar información, eliminar del inventario y ver el mapa.
  * **Vendedor:** Modo consulta para revisar el catálogo, realizar búsquedas y ver detalles sin permisos de edición.
* Reglas de seguridad configuradas directamente en la consola de Firestore para impedir escrituras no autorizadas desde el cliente.

### 2. Catálogo e Inventario (Firestore, Storage y API Externa)
* **CRUD en tiempo real:** Sincronización continua de productos mediante `addSnapshotListener`.
* **Cálculo de precios y ofertas:** Manejo de descuentos porcentuales, visualización del valor original y precio final rebajado.
* **Búsqueda y filtros:** Búsqueda en vivo por nombre o código (ej. `PERF-01`) combinable con filtros por estado de oferta.
* **Manejo de imágenes:** Captura desde cámara o selección en galería. La imagen se almacena en **Firebase Storage** y se renderiza en la lista usando la librería **Glide** (con respaldo de decodificación en Base64).
* **Integración API REST:** Botón de sugerencia conectado al endpoint de fragancias de `DummyJSON` para autocompletar datos de prueba.

### 3. Ubicación y Mapas
* Integración de mapa interactivo utilizando **OSMDroid**.
* Captura de coordenadas GPS en tiempo real mediante `FusedLocationProviderClient` al momento de guardar o editar una fragancia.
* Marcadores en el mapa con el nombre y precio de cada producto registrado.
* Botón para centrar la vista en la ubicación del dispositivo y alternador de capas (**Calles** y vista satelital con **Esri World Imagery**).

### 4. Notificaciones
* Notificaciones locales en la barra de estado al registrar o actualizar productos, con canal dedicado y soporte para Android 13+ (`POST_NOTIFICATIONS`).
* Configuración base de servicio en segundo plano para Firebase Cloud Messaging (FCM).

---

## Cuentas de Prueba para Calificación

Para validar la diferencia de permisos entre roles:

| Rol | Correo | Contraseña | Permisos |
| :--- | :--- | :--- | :--- |
| **Administrador** | `admin@scently.com` | *(tu contraseña aquí)* | CRUD total, fotos, mapa y administración |
| **Vendedor** | `jersy@scently.com` | *(tu contraseña aquí)* | Solo lectura y consulta del catálogo |

---

## Tecnologías Utilizadas
* **Lenguaje:** Java
* **IDE:** Android Studio
* **Backend:** Firebase (Authentication, Cloud Firestore, Cloud Storage, FCM)
* **Librerías principales:**
  * OSMDroid (mapas OpenStreetMap)
  * Glide (carga y caché de imágenes)
  * Google Play Services Location (GPS / Fused Location)
