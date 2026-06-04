/**
 * Türkiye cep telefonu doğrulaması (backend PhoneNumberUtil ile uyumlu).
 * Kabul edilen formatlar: +905XXXXXXXXX, 905XXXXXXXXX, 05XXXXXXXXX, 5XXXXXXXXX
 * (aralarda boşluk olabilir). Backend gönderimde E.164'e (+905XXXXXXXXX) normalize eder.
 */
export function isValidTurkishMobile(value: string | null | undefined): boolean {
    if (!value) return false;
    const digits = value.replace(/\D/g, '');
    return (
        (digits.length === 12 && digits.startsWith('90') && digits[2] === '5') ||
        (digits.length === 11 && digits.startsWith('0') && digits[1] === '5') ||
        (digits.length === 10 && digits[0] === '5')
    );
}

/** Kullanıcıya gösterilecek standart örnek format. */
export const PHONE_PLACEHOLDER = '+905XXXXXXXXX';

/** Form alanları için yardımcı ipucu metni. */
export const PHONE_HINT = '+905XXXXXXXXX veya 05XXXXXXXXX';
