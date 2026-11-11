import { HttpErrorResponse } from '@angular/common/http';
import { apiErrorMessage } from './api-error';

describe('apiErrorMessage', () => {
  it('uses the message sent by the API', () => {
    const error = new HttpErrorResponse({ status: 409, error: { message: 'Ya existe una categoría con ese nombre' } });

    expect(apiErrorMessage(error, 'fallback')).toBe('Ya existe una categoría con ese nombre');
  });

  it('falls back when the API sends no message', () => {
    expect(apiErrorMessage(new HttpErrorResponse({ status: 500, error: null }), 'No se pudo guardar')).toBe(
      'No se pudo guardar',
    );
    expect(apiErrorMessage(new HttpErrorResponse({ status: 500, error: { message: '  ' } }), 'No se pudo guardar')).toBe(
      'No se pudo guardar',
    );
  });

  it('explains a lost connection', () => {
    expect(apiErrorMessage(new HttpErrorResponse({ status: 0 }), 'fallback')).toContain('conectarnos');
  });

  it('falls back for anything that is not an HTTP error', () => {
    expect(apiErrorMessage(new Error('boom'), 'fallback')).toBe('fallback');
  });
});
