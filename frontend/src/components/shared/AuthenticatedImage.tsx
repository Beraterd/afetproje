import { useEffect, useRef, useState } from 'react';
import axiosInstance from '@/api/axiosInstance';
import { ImageOff } from 'lucide-react';

interface AuthenticatedImageProps {
    /** Full photo URL returned by the backend (e.g. http://localhost:8080/api/damage-assessments/photos/{token}) */
    photoUrl: string;
    alt: string;
    className?: string;
}

/**
 * Fetches a server-protected photo using the axios instance (which attaches the
 * Authorization header), converts the blob to an object URL, and renders it as
 * a normal <img>. The object URL is revoked on unmount to avoid memory leaks.
 */
export const AuthenticatedImage: React.FC<AuthenticatedImageProps> = ({ photoUrl, alt, className }) => {
    const [objectUrl, setObjectUrl] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);
    // Track the latest url ref so the cleanup only revokes the current blob
    const blobRef = useRef<string | null>(null);

    useEffect(() => {
        setLoading(true);
        setError(false);
        setObjectUrl(null);

        // Extract the download token — last path segment of the full URL
        const token = photoUrl.split('/').pop();
        if (!token) {
            setError(true);
            setLoading(false);
            return;
        }

        let active = true;
        axiosInstance
            .get(`/damage-assessments/photos/${token}`, { responseType: 'blob' })
            .then(res => {
                if (!active) return;
                const url = URL.createObjectURL(res.data as Blob);
                blobRef.current = url;
                setObjectUrl(url);
            })
            .catch(() => {
                if (active) setError(true);
            })
            .finally(() => {
                if (active) setLoading(false);
            });

        return () => {
            active = false;
            if (blobRef.current) {
                URL.revokeObjectURL(blobRef.current);
                blobRef.current = null;
            }
        };
    }, [photoUrl]);

    if (loading) {
        return <div className={`${className ?? ''} bg-gray-200 animate-pulse rounded-lg`} />;
    }

    if (error || !objectUrl) {
        return (
            <div className={`${className ?? ''} bg-gray-100 rounded-lg flex items-center justify-center`}>
                <ImageOff className="h-5 w-5 text-gray-400" />
            </div>
        );
    }

    return <img src={objectUrl} alt={alt} className={className} />;
};
