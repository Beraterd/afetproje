import { AxiosError } from 'axios';
import { ErrorResponse } from '@/types';

export interface ApiError {
    status: number;
    code: string;
    message: string;
    details?: { field: string; message: string }[];
}

export const parseApiError = (error: AxiosError<ErrorResponse>): ApiError => {
    if (error.response) {
        // Backend returned an error response
        return {
            status: error.response.status,
            code: error.response.data.error || 'UNKNOWN_ERROR',
            message: error.response.data.message || 'An unexpected server error occurred.',
            details: error.response.data.details,
        };
    } else if (error.request) {
        // Request made but no response received
        return {
            status: 0,
            code: 'NETWORK_ERROR',
            message: 'Unable to connect to the server. Please check your internet connection and try again.',
        };
    } else {
        // Something else happened while setting up the request
        return {
            status: 0,
            code: 'REQUEST_SETUP_ERROR',
            message: error.message || 'Error executing request.',
        };
    }
};
