# Despliegue de La Cuevita en un servidor

Todo corre con Docker en un solo servidor Linux:

| Servicio | Qué hace |
|---|---|
| `web` | Nginx: sirve la tienda, reenvía `/api` al backend y maneja HTTPS |
| `backend` | La aplicación (Spring Boot) |
| `db` | PostgreSQL con su volumen |
| `backup` | Copia diaria de la base de datos y de las fotos a `./backups` |
| `certbot-renew` | Renueva el certificado HTTPS solo |

## Lo que necesitas antes

- Un servidor Linux (Ubuntu 22.04 o 24.04) con **2 GB de memoria** como mínimo y unos 20 GB de disco.
- Un **dominio** con un registro `A` apuntando a la IP del servidor.
- Los puertos **80** y **443** abiertos. Los demás (5432, 8080) **no** deben abrirse: la base y el backend no se exponen.
- Docker y Docker Compose instalados en el servidor.

## 1. Instalar

```bash
git clone <URL-del-repositorio> lacuevita
cd lacuevita
cp .env.prod.example .env
nano .env        # llena DOMAIN, las claves, el administrador y el correo
```

Genera claves largas con `openssl rand -base64 48` para `JWT_SECRET` y `POSTGRES_PASSWORD`.
**Nunca subas `.env` a GitHub** (ya está ignorado).

Para no escribir `-f` cada vez:

```bash
echo "COMPOSE_FILE=docker-compose.prod.yml" >> .env
```

## 2. Arrancar (primero solo con HTTP)

```bash
docker compose up -d --build
docker compose ps        # todos deben quedar «Up» y backend «healthy»
```

El backend crea las tablas y el administrador (`ADMIN_EMAIL` / `ADMIN_PASSWORD`) la primera vez.
La tienda ya responde en `http://TU_DOMINIO`.

## 3. Activar HTTPS

```bash
docker compose --profile tools run --rm certbot-issue   # pide el certificado a Let's Encrypt
docker compose restart web                              # Nginx pasa solo a HTTPS
docker compose --profile tls up -d certbot-renew        # renovación automática
```

Comprueba `https://TU_DOMINIO`. Si algo falla, casi siempre es que el dominio aún no apunta al servidor.

## 4. Cargar el catálogo (opcional)

Si entregas la tienda con productos, copia la carpeta `entrega` generada en la máquina de desarrollo
(ver `ENTREGA.md`) y restaura el catálogo con `restaurar-catalogo`.
Si el cliente va a cargarlo todo desde el panel, no hace falta.

## 5. Configuración externa

- **Inicio de sesión con Google:** en Google Cloud, agrega `https://TU_DOMINIO` a los *orígenes de JavaScript autorizados*
  del cliente OAuth, y publica la pantalla de consentimiento.
- **Correo:** con Gmail necesitas una contraseña de aplicación. Para que las facturas no caigan en spam, el dominio
  debería tener sus registros SPF y DKIM; lo ideal es un servicio de correo transaccional.
- **Panel:** entra con el administrador y completa *Configuración* (WhatsApp, cuentas bancarias, datos de
  facturación y transportistas).

## Copias de seguridad

El servicio `backup` guarda cada día (03:00 hora de Ecuador) la base de datos y la carpeta de fotos y comprobantes
en `./backups`, y borra las de más de 14 días (`BACKUP_KEEP_DAYS`).

**Una copia que está en el mismo servidor no te protege si el servidor se pierde.** Copia `./backups` a otro lugar
cada día, por ejemplo con `rsync` o `rclone` hacia otro servidor o un almacenamiento en la nube, y activa los
*snapshots* del proveedor del servidor.

Hacer una copia ahora mismo:

```bash
docker compose exec backup sh /scripts/backup.sh
```

### Restaurar

```bash
ls backups                                   # elige el par db-... y uploads-... de la misma fecha
docker compose stop backend
docker compose --profile tools run --rm restore sh /scripts/restore.sh db-AAAAMMDD-HHMMSS.dump uploads-AAAAMMDD-HHMMSS.tar.gz
docker compose start backend
```

Esto **reemplaza** la base de datos y las fotos actuales. **Prueba la restauración** en otro servidor antes de
necesitarla de verdad.

## Actualizar a una versión nueva

```bash
git pull
docker compose up -d --build
```

Las migraciones de la base de datos se aplican solas al arrancar. Haz una copia antes de actualizar.

## Comandos útiles

```bash
docker compose logs -f backend      # ver lo que pasa en el backend
docker compose logs --tail 100 web
docker compose restart backend
docker compose down                 # apaga todo (los datos se conservan en los volúmenes)
```

`docker compose down -v` **borra los volúmenes** (base de datos y fotos). No lo uses salvo que quieras empezar de cero.

## Lista antes de abrir al público

- [ ] `DOMAIN` y HTTPS funcionando, y `COOKIE_SECURE=true`.
- [ ] Claves propias en `JWT_SECRET`, `POSTGRES_PASSWORD` y `ADMIN_PASSWORD` (el backend no arranca con las de ejemplo).
- [ ] `ORDERS_ALLOW_DELETE=false`.
- [ ] Correo configurado y probado con «Enviar por correo» en un pedido.
- [ ] Google Login con el dominio autorizado.
- [ ] Copias fuera del servidor y una restauración probada.
- [ ] Datos de facturación, WhatsApp y cuentas bancarias llenados en *Configuración*.
- [ ] Una compra de prueba completa: pedido, comprobante, confirmación, envío y factura.
