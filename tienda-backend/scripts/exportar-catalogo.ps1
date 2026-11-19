<#
  Exporta el catálogo (categorías, colores, tallas, productos, variantes, stock y fotos) para entregarlo
  o llevarlo a otra instalación. NO incluye clientes, pedidos, comprobantes, cuentas bancarias ni usuarios.

  Uso:   .\scripts\exportar-catalogo.ps1
  Salida: carpeta "entrega" con catalogo.sql y uploads\product-images
#>
param(
    [string]$Salida = (Join-Path $PSScriptRoot '..\entrega'),
    [string]$Contenedor = 'tienda-postgres',
    [string]$Usuario = 'tienda_user',
    [string]$BaseDatos = 'tienda'
)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)

New-Item -ItemType Directory -Force -Path $Salida | Out-Null
$Salida = (Resolve-Path $Salida).Path

$tablas = 'categories', 'colors', 'sizes', 'products', 'product_variants', 'product_images', 'inventory', 'stored_files'
$tFlags = ($tablas | ForEach-Object { "-t $_" }) -join ' '

# El volcado se hace dentro del contenedor para no perder los acentos por la consola de Windows
# Los avisos de pg_dump (p. ej. claves circulares de categorías) son normales: van a un archivo aparte
docker exec $Contenedor sh -c "pg_dump -U $Usuario -d $BaseDatos --data-only --inserts $tFlags > /tmp/catalogo.sql 2> /tmp/pg_dump.err"
if ($LASTEXITCODE -ne 0) {
    docker exec $Contenedor cat /tmp/pg_dump.err
    throw 'No se pudo exportar la base de datos (¿está corriendo el contenedor?)'
}
docker cp "${Contenedor}:/tmp/catalogo.sql" (Join-Path $Salida 'catalogo_crudo.sql') | Out-Null

$lineas = [System.IO.File]::ReadAllLines((Join-Path $Salida 'catalogo_crudo.sql'), $utf8) |
    Where-Object { $_ -notmatch 'PAYMENT_RECEIPT' }   # los comprobantes de pago son privados

$final = @('SET session_replication_role = replica;') + $lineas + @(
    'UPDATE public.inventory SET quantity_reserved = 0;',
    'UPDATE public.stored_files SET uploaded_by = NULL;',
    "UPDATE public.product_images SET url = regexp_replace(url, '^https?://[^/]+', '') WHERE url ~ '^https?://[^/]+/api/files/';",
    "UPDATE public.categories SET image_url = regexp_replace(image_url, '^https?://[^/]+', '') WHERE image_url ~ '^https?://[^/]+/api/files/';",
    'SET session_replication_role = DEFAULT;'
)
[System.IO.File]::WriteAllLines((Join-Path $Salida 'catalogo.sql'), $final, $utf8)
Remove-Item (Join-Path $Salida 'catalogo_crudo.sql')

# Fotos de productos (las de comprobantes no se copian)
$fotos = Join-Path $PSScriptRoot '..\uploads\product-images'
$destino = Join-Path $Salida 'uploads\product-images'
if (Test-Path $fotos) {
    New-Item -ItemType Directory -Force -Path $destino | Out-Null
    Copy-Item -Path (Join-Path $fotos '*') -Destination $destino -Recurse -Force
}

$cantidadFotos = if (Test-Path $destino) { (Get-ChildItem $destino -Recurse -File).Count } else { 0 }
Write-Host "Listo. Carpeta: $Salida"
Write-Host "  catalogo.sql  y  $cantidadFotos fotos de productos"
