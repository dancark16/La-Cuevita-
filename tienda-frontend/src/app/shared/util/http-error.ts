import { HttpErrorResponse } from '@angular/common/http';

interface ApiErrorBody {
  message?: string;
  details?: string[];
}

export function httpErrorMessage(
  error: unknown,
  fallback = 'Ocurrió un error inesperado. Intenta de nuevo.'
): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }
  if (error.status === 0) {
    return 'No se pudo conectar con el servidor.';
  }
  if (error.status === 403) {
    return 'No tienes permiso para realizar esta acción.';
  }
  const body = error.error as ApiErrorBody | null;
  if (!body?.message) {
    return fallback;
  }
  const details = body.details?.length ? ` (${body.details.join('; ')})` : '';
  return `${body.message}${details}`;
}
