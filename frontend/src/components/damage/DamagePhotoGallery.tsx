import React from 'react';
import { AuthenticatedImage } from '@/components/shared/AuthenticatedImage';

interface DamagePhotoGalleryProps {
    title: string;
    photoUrls: string[];
    altPrefix: string;
}

/** Zaten yüklenmiş fotoğrafları gösteren salt-okunur galeri — hasar detayında bildirim/saha/
 *  fallback fotoğrafları için 3 kez kullanılır. */
export const DamagePhotoGallery: React.FC<DamagePhotoGalleryProps> = ({ title, photoUrls, altPrefix }) => {
    if (photoUrls.length === 0) return null;

    return (
        <div className="border-t border-gray-100 pt-4">
            <p className="text-sm font-medium text-gray-700 mb-3">
                {title} ({photoUrls.length})
            </p>
            <div className="grid grid-cols-3 gap-2">
                {photoUrls.map((url, i) => (
                    <AuthenticatedImage
                        key={i}
                        photoUrl={url}
                        alt={`${altPrefix} ${i + 1}`}
                        className="w-full h-24 object-cover rounded-lg border border-gray-200"
                    />
                ))}
            </div>
        </div>
    );
};
