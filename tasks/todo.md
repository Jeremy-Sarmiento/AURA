# Tareas de estabilización

- [ ] **1. Escaneo y metadatos** — LocalFileManager, Models y pruebas de almacenamiento: enumeración parcial falla, LRC por SAF, identidad canónica, metadatos desconocidos sin inventarlos y color válido para Int.MIN_VALUE.
- [ ] **2. Persistencia segura** — Entities, Daos, AuraDatabase y pruebas: migración v1→v2 sin pérdida, claves foráneas, índices, borrados grandes y fusión de alias. Depende del contrato de 1.
- [ ] **3. Sincronización e importación** — AuraViewModel y pruebas: carpetas solapadas, permisos, límites de concurrencia y conservación de personalización. Depende de 1–2.
- [ ] **Punto de control:** probar almacenamiento/persistencia y compilar.
- [ ] **4. Reproducción y sesión** — AudioPlayerManager, MusicPlaybackService y pruebas: parada durante preparación, controles idempotentes, foco diferido, ciclos shuffle y seek remoto.
- [ ] **5. Interfaz y bloqueo** — AuraApp, QueueScreen, LibraryScreen, MainActivity y pruebas: filtro exacto de artista, listas automáticas coherentes, cola aleatoria y entrega de resultados protegida sin error Lint.
- [ ] **6. Letras y documentación técnica** — parser LRC, presentación de calidad y README: multietiqueta, orden temporal y capacidades reales. Depende de 1.
- [ ] **Punto de control:** ejecutar todas las pruebas sin omitir ninguna.
- [ ] **7. Compilación y privacidad** — Gradle Wrapper, dependencias, CI, .gitignore y reglas de backup: compilación reproducible, sin integraciones de red innecesarias y claves fuera de Git.
- [ ] **Verificación final:** assembleDebug, testDebugUnitTest y lintDebug; revisar cambios y registrar commits sin publicar.
- [ ] **Hardware:** validar en teléfono biometría, SAF externo, Bluetooth, suspensión y salida DAC (no disponible en esta sesión).
