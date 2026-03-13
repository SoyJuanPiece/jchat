# Resumen de Progreso del Proyecto JChat

Este documento registra el progreso, las decisiones clave y los próximos pasos en el desarrollo de JChat, actuando como una fuente de verdad para el agente y el usuario.

## Última Actualización
- Fecha: March 13, 2026
- Estado: UX tipo mensajería + capa de fiabilidad avanzada (reintentos de envío y sincronización de lectura en tiempo real).

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

### Etapa 5: Refactor Visual Integral + Migraciones Supabase (Completada)

#### 5.1 Diseño y Consistencia de Producto
*   **Cambio:** Creado tema visual global (`JChatTheme`) con paleta tipo mensajería (verdes, superficies limpias y contraste mejorado).
*   **Cambio:** Refactorizadas pantallas clave (`Auth`, `Home`, `ChatList`, `Conversation`, `Calls`, `Updates`) para mantener un lenguaje visual coherente.
*   **Cambio:** Home ahora usa cabecera branded, navegación inferior refinada y experiencia más cercana a apps de chat líderes.

#### 5.2 Supabase CLI y Flujo de Migraciones
*   **Cambio:** Inicializado proyecto Supabase CLI en el repo (`supabase init`).
*   **Cambio:** Creada migración inicial `supabase/migrations/20260313134449_init_jchat_schema.sql` con:
    *   tablas (`profiles`, `chats`, `chat_participants`, `messages`),
    *   índices,
    *   políticas RLS,
    *   triggers,
    *   publicación realtime,
    *   configuración de bucket `chat-media`.
*   **Cambio:** Añadida guía operativa en `supabase/README.md` para link + `db push`.

### Etapa 6: Fiabilidad de Mensajería (Completada)

#### 6.1 Reintento de Mensajes Fallidos
*   **Cambio:** Añadida acción `Retry` directamente en burbujas fallidas del usuario.
*   **Cambio:** Nuevo intent y flujo en `ConversationViewModel` para reintentar envíos sin salir del chat.
*   **Cambio:** Nuevo contrato `retryFailedMessage` en repositorio, con transición de estados `FAILED -> SENDING -> SENT/FAILED`.

#### 6.2 Consistencia de Estados Leído/No Leído
*   **Cambio:** Corrección de preview de último mensaje para evitar incremento de `unread_count` en mensajes enviados por el usuario local.
*   **Cambio:** Al marcar chat como leído, ahora también se sincroniza estado remoto en Supabase (`READ`).

#### 6.3 Realtime de Actualizaciones
*   **Cambio:** Añadido stream de eventos `UPDATE` de mensajes en Realtime para reflejar cambios de estado (incluyendo lecturas) en vivo.
*   **Cambio:** Persistencia local automática de updates remotos para mantener UI y cache alineados.

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
4.  **Operación Supabase:**
    *   Ejecutar `supabase link --project-ref ... --password ...` y `supabase db push` desde credenciales reales del proyecto.

---
