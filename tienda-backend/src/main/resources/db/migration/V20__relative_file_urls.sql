-- Las fotos se guardan con dirección relativa (/api/files/{id}) para que no dependan del dominio.
-- Convierte las que se guardaron con el dominio completo (p. ej. http://localhost:8080/api/files/...).
UPDATE product_images
   SET url = regexp_replace(url, '^https?://[^/]+', '')
 WHERE url ~ '^https?://[^/]+/api/files/';

UPDATE categories
   SET image_url = regexp_replace(image_url, '^https?://[^/]+', '')
 WHERE image_url ~ '^https?://[^/]+/api/files/';
