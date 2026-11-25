# Entrega: instalación limpia de La Cuevita

Pasos para dejar la tienda funcionando en una máquina nueva (o en el servidor) con el catálogo cargado
y **sin** pedidos, clientas ni datos de prueba.

## 1. Preparar el catálogo (en la máquina de desarrollo)

```powershell
cd tienda-backend
.\scripts\exportar-catalogo.ps1
```

Genera la carpeta `tienda-backend\entrega` con `catalogo.sql` y las fotos de productos.
No incluye clientas, pedidos, comprobantes de pago, cuentas bancarias ni usuarios.

## 2. Instalar en la máquina nueva

1. Copiar el proyecto y la carpeta `entrega`.
2. Levantar la base de datos: `docker compose up -d` (dentro de `tienda-backend`).
3. Definir las variables de entorno del backend (ver la tabla) y arrancarlo **una vez**:
   crea las tablas y el administrador inicial.
4. Cargar el catálogo:
   ```powershell
   .\scripts\restaurar-catalogo.ps1
   ```
5. Reiniciar el backend.
6. Entrar al panel con `ADMIN_EMAIL` y `ADMIN_PASSWORD`, y completar **Configuración**:
   WhatsApp, Instagram, cuentas bancarias, datos de facturación y transportistas.

## Variables de entorno del backend

| Variable | Para qué | Obligatoria |
|---|---|---|
| `POSTGRES_PASSWORD` | Clave de la base de datos | Sí |
| `JWT_SECRET` | Clave para firmar las sesiones (mínimo 32 caracteres, única) | Sí |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Crean el administrador la primera vez (clave de 10 caracteres o más) | Sí, en la primera instalación |
| `UPLOAD_DIR` | Carpeta de fotos (hay que respaldarla) | No (`uploads`) |
| `FRONTEND_URL` | Dirección pública de la tienda, para los botones de los correos | En producción |
| `CORS_ORIGINS` | Orígenes del frontend si no está en el mismo dominio | No |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Correo para facturas y avisos de envío | Para enviar correos |
| `ORDERS_ALLOW_DELETE` | `false` en producción: no permite borrar pedidos | En producción |
| `ORDER_RESERVATION_HOURS` | Horas que se aparta el stock de un pedido sin pagar (48; `0` desactiva) | No |
| `GOOGLE_CLIENT_ID` | Inicio de sesión con Google | Si se usa |

## Notas

- Las fotos se guardan con dirección relativa (`/api/files/...`), así que no dependen del dominio.
  El frontend y el backend deben servirse bajo el mismo dominio (Nginx envía `/api` al backend).
  En desarrollo, `ng serve` usa `proxy.conf.json` para lo mismo.
- La carpeta de fotos (`uploads`) y la base de datos deben tener copias de seguridad.

## Servidor de producción

Para instalar todo en un servidor (Docker, HTTPS, copias de seguridad), sigue **`DESPLIEGUE.md`**.
