#!/bin/sh
# Elige la configuración de Nginx al arrancar:
#  - sin certificado todavía: solo HTTP (y el camino para que Let's Encrypt valide el dominio)
#  - con certificado en /etc/letsencrypt: HTTPS, y HTTP redirige a HTTPS
set -e

if [ -n "$DOMAIN" ] && [ -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
  envsubst '${DOMAIN}' < /etc/nginx/site/https.conf.template > /etc/nginx/conf.d/default.conf
  echo 'add_header Strict-Transport-Security "max-age=31536000" always;' > /etc/nginx/snippets/hsts.conf
  echo "nginx: HTTPS activo para $DOMAIN"
else
  envsubst '${DOMAIN}' < /etc/nginx/site/http.conf.template > /etc/nginx/conf.d/default.conf
  : > /etc/nginx/snippets/hsts.conf
  echo "nginx: solo HTTP (aún no hay certificado)"
fi

# Cada 12 horas recarga la configuración para tomar certificados renovados
( while :; do sleep 12h; nginx -s reload >/dev/null 2>&1 || true; done ) &
