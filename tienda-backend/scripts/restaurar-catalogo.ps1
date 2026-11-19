<#
  Carga el catálogo exportado con exportar-catalogo.ps1 en una instalación nueva.
  Antes: la base de datos debe estar levantada y el backend debe haber arrancado al menos una vez
  (así Flyway crea las tablas). Después de cargar, reinicia el backend.

  Uso:   .\scripts\restaurar-catalogo.ps1
#>
param(
    [string]$Entrega = (Join-Path $PSScriptRoot '..\entrega'),
    [string]$Contenedor = 'tienda-postgres',
    [string]$Usuario = 'tienda_user',
    [string]$BaseDatos = 'tienda',
    [string]$CarpetaFotos = (Join-Path $PSScriptRoot '..\uploads'),
    [switch]$Forzar
)

$ErrorActionPreference = 'Stop'
$sql = Join-Path $Entrega 'catalogo.sql'
if (-not (Test-Path $sql)) { throw "No encuentro $sql. Primero ejecuta exportar-catalogo.ps1 o copia la carpeta de entrega." }

$existentes = docker exec $Contenedor psql -U $Usuario -d $BaseDatos -Atc 'select count(*) from products'
if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar la base de datos. ¿Arrancó el backend al menos una vez?' }
if ([int]$existentes -gt 0 -and -not $Forzar) {
    throw "La base ya tiene $existentes productos. Si de verdad quieres cargar encima, usa -Forzar (puede dar errores de duplicados)."
}

docker cp $sql "${Contenedor}:/tmp/catalogo.sql" | Out-Null
docker exec $Contenedor psql -U $Usuario -d $BaseDatos -v ON_ERROR_STOP=1 -q -f /tmp/catalogo.sql
if ($LASTEXITCODE -ne 0) { throw 'La carga del catálogo falló.' }

$fotos = Join-Path $Entrega 'uploads'
if (Test-Path $fotos) {
    New-Item -ItemType Directory -Force -Path $CarpetaFotos | Out-Null
    Copy-Item -Path (Join-Path $fotos '*') -Destination $CarpetaFotos -Recurse -Force
}

$productos = docker exec $Contenedor psql -U $Usuario -d $BaseDatos -Atc 'select count(*) from products'
Write-Host "Listo: $productos productos cargados. Reinicia el backend."
