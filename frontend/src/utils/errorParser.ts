import { AxiosError } from 'axios';
import { ErrorResponse } from '@/types';

export interface ApiError {
    status: number;
    code: string;
    message: string;
    requestId?: string;
    details?: { field: string; message: string }[];
}

export const parseApiError = (error: AxiosError<ErrorResponse>): ApiError => {
    if (error.response) {
        // Backend returned an error response
        return {
            status: error.response.status,
            code: error.response.data.error || 'UNKNOWN_ERROR',
            message: error.response.data.message || 'Beklenmeyen bir sunucu hatası oluştu.',
            requestId: error.response.data.requestId,
            details: error.response.data.details,
        };
    } else if (error.request) {
        // Request made but no response received
        return {
            status: 0,
            code: 'NETWORK_ERROR',
            message: 'Sunucuya bağlanılamadı. İnternet bağlantınızı kontrol edip tekrar deneyin.',
        };
    } else {
        // Something else happened while setting up the request
        return {
            status: 0,
            code: 'REQUEST_SETUP_ERROR',
            message: error.message || 'İstek gönderilirken bir hata oluştu.',
        };
    }
};

function isApiError(error: unknown): error is ApiError {
    return typeof error === 'object' && error !== null && 'status' in error && 'message' in error;
}

/**
 * Herhangi bir yakalanmış hatadan (axiosInstance interceptor'ı zaten {@link parseApiError}
 * ile normalize ediyor, ama defansif olarak ham Error/unknown da kabul edilir) kullanıcıya
 * gösterilecek TEK bir Türkçe mesaj döndürür. Componentlerde tekrar eden
 * `err?.response?.data?.message || err.message || 'fallback'` deseni yerine bunu kullanın.
 */
export function getApiErrorMessage(error: unknown, fallback = 'Beklenmeyen bir hata oluştu.'): string {
    if (isApiError(error)) {
        return error.message || fallback;
    }
    if (error instanceof Error && error.message) {
        return error.message;
    }
    return fallback;
}

/** Validation hatalarında field adı → mesaj eşlemesi. Form alanlarını inline işaretlemek için. */
export function getFieldErrors(error: unknown): Record<string, string> | undefined {
    if (!isApiError(error) || !error.details || error.details.length === 0) {
        return undefined;
    }
    return Object.fromEntries(error.details.map((d) => [d.field, d.message]));
}

/** Yalnızca ciddi/beklenmeyen hatalarda (5xx, network) teknik destek için gösterilecek kod. */
export function getRequestId(error: unknown): string | undefined {
    return isApiError(error) ? error.requestId : undefined;
}

export function isRateLimitError(error: unknown): boolean {
    return isApiError(error) && error.status === 429;
}

export function isConflictError(error: unknown): boolean {
    return isApiError(error) && error.status === 409;
}
