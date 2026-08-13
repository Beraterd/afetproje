import { describe, it, expect } from 'vitest';
import {
    getApiErrorMessage,
    getFieldErrors,
    getRequestId,
    isConflictError,
    isRateLimitError,
    ApiError,
} from '@/utils/errorParser';

/** axiosInstance'ın response interceptor'ı her hatayı parseApiError ile bu şekle normalize
 *  eder — component'lerin gördüğü gerçek hata objesi budur (ham AxiosError değil). */
function apiError(overrides: Partial<ApiError>): ApiError {
    return { status: 500, code: 'INTERNAL_ERROR', message: 'Beklenmeyen bir hata oluştu.', ...overrides };
}

describe('getApiErrorMessage', () => {
    it('returns the backend message for a parsed ApiError', () => {
        const err = apiError({ status: 409, code: 'STALE_VERSION', message: 'Stok başka bir işlem tarafından güncellendi. Lütfen tekrar deneyin.' });
        expect(getApiErrorMessage(err)).toBe('Stok başka bir işlem tarafından güncellendi. Lütfen tekrar deneyin.');
    });

    it('returns the backend message for a 429 rate-limit error', () => {
        const err = apiError({ status: 429, code: 'RATE_LIMITED', message: 'Çok fazla deneme yapıldı. Lütfen kısa süre sonra tekrar deneyin.' });
        expect(getApiErrorMessage(err)).toBe('Çok fazla deneme yapıldı. Lütfen kısa süre sonra tekrar deneyin.');
    });

    it('falls back to the provided default when the ApiError has no message', () => {
        const err = apiError({ message: '' });
        expect(getApiErrorMessage(err, 'Varsayılan mesaj')).toBe('Varsayılan mesaj');
    });

    it('falls back to the default for a plain unknown error', () => {
        expect(getApiErrorMessage(new Error(''), 'Varsayılan mesaj')).toBe('Varsayılan mesaj');
        expect(getApiErrorMessage(undefined, 'Varsayılan mesaj')).toBe('Varsayılan mesaj');
        expect(getApiErrorMessage(null, 'Varsayılan mesaj')).toBe('Varsayılan mesaj');
    });

    it('uses a plain Error message when not an ApiError', () => {
        expect(getApiErrorMessage(new Error('ağ hatası'))).toBe('ağ hatası');
    });
});

describe('getFieldErrors', () => {
    it('maps validation details to a field -> message record', () => {
        const err = apiError({
            status: 400,
            code: 'VALIDATION_ERROR',
            details: [{ field: 'quantity', message: "0'dan büyük olmalıdır" }],
        });
        expect(getFieldErrors(err)).toEqual({ quantity: "0'dan büyük olmalıdır" });
    });

    it('returns undefined when there are no field errors', () => {
        expect(getFieldErrors(apiError({}))).toBeUndefined();
        expect(getFieldErrors(new Error('x'))).toBeUndefined();
    });
});

describe('getRequestId', () => {
    it('extracts the request id for technical-support display on serious errors', () => {
        const err = apiError({ status: 500, requestId: '7fd12345' });
        expect(getRequestId(err)).toBe('7fd12345');
    });

    it('is undefined when the backend did not include one', () => {
        expect(getRequestId(apiError({}))).toBeUndefined();
    });
});

describe('status helpers', () => {
    it('isConflictError recognizes 409', () => {
        expect(isConflictError(apiError({ status: 409 }))).toBe(true);
        expect(isConflictError(apiError({ status: 400 }))).toBe(false);
    });

    it('isRateLimitError recognizes 429', () => {
        expect(isRateLimitError(apiError({ status: 429 }))).toBe(true);
        expect(isRateLimitError(apiError({ status: 500 }))).toBe(false);
    });
});
