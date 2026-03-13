# Resumen de Progreso del Proyecto JChat

Este documento registra el progreso, las decisiones clave y los próximos pasos en el desarrollo de JChat, actuando como una fuente de verdad para el agente y el usuario.

## Última Actualización
- Fecha: March 13, 2026
- Estado: Integración total con **Supabase 3.0.2** + mejora visual premium de conversación + cierre de sesión robusto con limpieza local completa.

---

## Log de Cambios y Decisiones Clave

### Etapa 3: Integración Real con Supabase (Completada)

#### 3.1 Infraestructura y Credenciales
*   **Cambio:** Configurado `SupabaseClient` con URL y Anon Key reales del proyecto.
*   **Cambio:** Actualizado `RemoteDataSource.kt` para ser compatible con la API de **Supabase Kotlin SDK 3.0.2**.
*   **Decisión:** Uso de `sessionStatus` (Flow) para manejar el estado de la sesión de forma reactiva en toda la app.

#### 3.2 Autenticación y Perfiles
*   **Cambio:** Implementada `AuthScreen` y `AuthViewModel` para Sign In y Sign Up reales.
*   **Cambio:** Sincronización bidireccional del perfil (Display Name y Avatar URL) con la tabla `profiles` de Supabase.
*   **Cambio:** Implementada función de **Cerrar Sesión (Sign Out)** funcional desde el perfil.

#### 3.3 Mensajería Real y Offline-First
*   **Cambio:** Conectado `ChatRepositoryImpl` con Supabase Realtime para recibir mensajes al instante.
*   **Cambio:** Implementada sincronización de historial al abrir un chat (`syncMessages`).
*   **Cambio:** Implementada carga real de archivos a Supabase Storage (bucket `chat-media`).
*   **Decisión:** Se mantiene la estrategia de "escritura optimista" (el mensaje aparece inmediatamente en local y se confirma después con el servidor).

#### 3.4 Funcionalidades de Usuario
*   **Cambio:** Implementada **Búsqueda de Usuarios** por `@username` para iniciar nuevos chats.
*   **Cambio:** Implementado **Filtrado de Chats** mediante la lupa en la pantalla principal.
*   **Cambio:** Limpieza total de UI: Eliminados "Contactos 1, 2, 3" y bots de respuesta automáticos.

### Etapa 4: UX Premium + Robustez de Sesión (Completada)

#### 4.1 Conversación Rediseñada
*   **Cambio:** Rediseño de pantalla de conversación con fondo degradado y cabecera enriquecida (avatar + estado).
*   **Cambio:** Implementados separadores por día (Today/Yesterday/fecha corta) para mejorar legibilidad histórica.
*   **Cambio:** Implementadas burbujas agrupadas por emisor y ventana temporal para un look más natural y fluido.
*   **Cambio:** Refinados indicadores de estado de mensaje (sending/sent/read) con check azul para lectura.
*   **Cambio:** Añadido estado vacío más claro cuando una conversación aún no tiene mensajes.

#### 4.2 Experiencia de Lista de Chats
*   **Cambio:** Añadido indicador visual de presencia online en el avatar del contacto.

#### 4.3 Seguridad y Coherencia de Sign Out
*   **Cambio:** Implementada limpieza total de base local al cerrar sesión (messages, chats, profiles).
*   **Cambio:** Añadidas queries SQLDelight `clearMessages`, `clearChats` y `clearProfiles` para wipe controlado.
*   **Cambio:** Cancelación de suscripciones realtime activas al cerrar sesión para evitar fugas y eventos fantasma.
*   **Cambio:** Unificado flujo de cierre de sesión desde Home para usar repositorio y respetar la estrategia offline-first.

---

## Próximos Pasos (Bloque Consolidado)

1.  **Gestión de Archivos Avanzada:**
    *   Implementar un selector de archivos real (Galería/Cámara) para Android e iOS.
2.  **Seguridad y Estabilidad:**
    *   Manejo de errores de conexión más amigable para el usuario.
    *   Añadir estrategia de reintento/reenvío para mensajes en estado `FAILED`.
3.  **Calidad de Producto:**
    *   Añadir tests de integración para flujo auth -> chat -> sign out (incluyendo limpieza local).
    *   Automatizar validación en GitHub Actions para SQLDelight + navegación base.

---
