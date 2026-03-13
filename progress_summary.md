# Resumen de Progreso del Proyecto JChat

Este documento registra el progreso, las decisiones clave y los próximos pasos en el desarrollo de JChat, actuando como una fuente de verdad para el agente y el usuario.

## Última Actualización
- Fecha: March 13, 2026
- Estado: Pendientes críticos cerrados: bloqueo desde chat, preferencias persistentes y eliminación real de cuenta por RPC aplicada en remoto.

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

### Etapa 7: Conversación Avanzada Tipo App Top (Completada)

#### 7.1 Replies y Acciones por Mensaje
*   **Cambio:** Implementado `swipe-to-reply` para responder rápido desde cualquier burbuja.
*   **Cambio:** Implementado menú de acciones por long-press: Reply, Copy, Retry (si falló) y Delete (mensajes propios).
*   **Cambio:** Input con preview de respuesta citada y opción de cancelar antes de enviar.
*   **Cambio:** Renderizado visual de mensajes que responden a otro mensaje (bloque de cita).

#### 7.2 Dominio y Persistencia
*   **Cambio:** Modelo `Message` ampliado con `replyToMessageId` y `replyPreview`.
*   **Cambio:** SQLDelight actualizado con columnas locales para replies y compatibilidad con instalaciones previas.
*   **Cambio:** Repositorio actualizado para enviar y reintentar mensajes preservando metadata de reply.

#### 7.3 Supabase Remoto
*   **Cambio:** Nueva migración aplicada: `20260313141651_add_message_reply_fields.sql`.
*   **Cambio:** Base remota sincronizada con columnas `reply_to_message_id` y `reply_preview`.

### Etapa 8: Ajustes Integrales Estilo Element X (Completada)

#### 8.1 Arquitectura de Settings
*   **Cambio:** Añadidos `SettingsScreen` y `SettingsViewModel` con patrón State/Intent para centralizar acciones de configuración.
*   **Cambio:** Integrada nueva ruta de navegación `settings` en el grafo principal.
*   **Decisión:** Mantener la edición profunda de perfil en pantalla dedicada (`ProfileScreen`) y usar Settings como hub principal.

#### 8.2 Personalización y Apariencia
*   **Cambio:** Implementado selector de tema con 3 modos (`Sistema`, `Claro`, `Oscuro`) y aplicación en caliente sobre toda la app.
*   **Cambio:** `JChatTheme` evolucionado para recibir `ThemeOption` en lugar de solo booleano de dark mode.
*   **Cambio:** `App.kt` ahora mantiene estado global de tema y lo propaga al `NavGraph`.

#### 8.3 Privacidad, Cuenta e Información
*   **Cambio:** Secciones de ajustes inspiradas en Element X: personalización, privacidad, cuenta, información y sesión.
*   **Cambio:** Cabecera de perfil enriquecida (avatar/inicial, display name y `@username`) con acceso directo a edición.
*   **Cambio:** Home menu actualizado para priorizar acceso a Ajustes y mantener cierre de sesión accesible.

#### 8.4 Sesión y Seguridad
*   **Cambio:** Flujo de cierre de sesión conectado desde Ajustes con estado visual de progreso.
*   **Cambio:** Añadido diálogo de confirmación para eliminación de cuenta (guardrail UX).
*   **Nota:** La eliminación real de cuenta queda pendiente de endpoint/backoffice seguro para evitar operaciones destructivas desde cliente.

### Etapa 9: Cierre de Funciones de Ajustes (Completada)

#### 9.1 Navegación y Pantallas Reales
*   **Cambio:** Añadidas rutas y pantallas para `Cambiar contraseña`, `Usuarios bloqueados`, `Reportar un problema` y `Acerca de`.
*   **Cambio:** `SettingsScreen` dejó de usar placeholders y ahora navega a flujos funcionales.

#### 9.2 Contraseña y Soporte
*   **Cambio:** Implementado flujo de actualización de contraseña vía `supabase.auth.updateUser`.
*   **Cambio:** Implementado formulario de reporte y persistencia en tabla `support_reports`.

#### 9.3 Privacidad (Bloqueados)
*   **Cambio:** Implementada lectura y desbloqueo de usuarios bloqueados desde tabla `blocked_users`.
*   **Cambio:** Añadidos métodos de dominio/datos (`getBlockedUsers`, `blockUser`, `unblockUser`).

#### 9.4 Backend Supabase
*   **Cambio:** Nueva migración `20260313193000_add_blocked_users_and_support_reports.sql` con tablas, índices y políticas RLS.
*   **Cambio:** Migración aplicada exitosamente en remoto y verificada en `migration list`.

### Etapa 10: Cierre Final de Pendientes UX/Seguridad (Completada)

#### 10.1 Bloqueo desde contexto de conversación
*   **Cambio:** Añadida acción `Bloquear usuario` en menú superior de `ConversationScreen`.
*   **Cambio:** Integrado flujo `ConversationIntent.BlockParticipant` para bloquear directamente al contacto del chat.

#### 10.2 Persistencia de preferencias
*   **Cambio:** Añadida tabla local `app_settings` en SQLDelight para persistir preferencias.
*   **Cambio:** `SettingsViewModel` ahora guarda y restaura tema + toggles (notificaciones/presencia).
*   **Cambio:** `App.kt` carga `ThemeOption` persistido al iniciar (aplica tema tras reinicio).

#### 10.3 Eliminación real de cuenta
*   **Cambio:** Implementada operación real de borrado de cuenta vía RPC `delete_my_account`.
*   **Cambio:** Nueva migración `20260313203000_add_delete_my_account_rpc.sql` aplicada y verificada en remoto.
*   **Cambio:** `SettingsViewModel` actualizado para ejecutar borrado real y limpiar sesión local.

---

## Próximos Pasos (Bloque Consolidado)

1.  **Persistencia de preferencias UX:**
    *   Extender persistencia a más preferencias de UX (orden de pestañas, densidad visual, etc.).
2.  **Bloqueo desde contexto de usuario:**
    *   Exponer acción de desbloqueo rápido también desde conversación/perfil de contacto.
3.  **Gestión de Archivos Avanzada:**
    *   Implementar un selector de archivos real (Galería/Cámara) para Android e iOS.
4.  **Calidad y Operación:**
    *   Añadir tests de integración para auth -> chat -> settings -> sign out.
    *   Automatizar validación en CI para SQLDelight + navegación + compilación shared.

---
