# PostgreSQL

PostgreSQL se levanta mediante `../docker-compose.yml` como tres instancias independientes: usuarios, conversaciones y mensajes. Cada instancia ejecuta únicamente los scripts de su carpeta en `base-datos/` al crear el volumen por primera vez.
