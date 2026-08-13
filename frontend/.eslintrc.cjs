/* ESLint 8.x classic config — bu proje ESLint 9+'a geçmediği için flat config kullanılmadı.
 * Amaç: gerçek bugları yakalayan minimum sağlam kural seti (TS parsing, React Hooks
 * kuralları, unused vars, React Refresh uyumu) — projeyi binlerce style hatasına boğmak değil. */
module.exports = {
    root: true,
    env: { browser: true, es2021: true, node: true },
    extends: [
        'eslint:recommended',
        'plugin:@typescript-eslint/recommended',
        'plugin:react-hooks/recommended',
    ],
    ignorePatterns: ['dist', 'node_modules', '.eslintrc.cjs'],
    parser: '@typescript-eslint/parser',
    parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
        ecmaFeatures: { jsx: true },
    },
    plugins: ['@typescript-eslint', 'react-refresh'],
    settings: {},
    rules: {
        // TypeScript zaten tanımsız tip/sembol hatalarını derleme aşamasında yakalıyor;
        // temel no-undef, TS ambient tipleri/global'lerde yanlış pozitif üretir.
        'no-undef': 'off',
        'no-unused-vars': 'off',
        '@typescript-eslint/no-unused-vars': ['warn', {
            argsIgnorePattern: '^_',
            varsIgnorePattern: '^_',
            caughtErrorsIgnorePattern: '^_',
        }],
        // Proje genelinde `any` zaten yaygın kullanılıyor (API response tipleme, event handler'lar) —
        // toplu yasaklamak yüzlerce mevcut dosyayı bozar. Uyarı bile vermiyoruz, bilinçli bir seçim.
        '@typescript-eslint/no-explicit-any': 'off',
        '@typescript-eslint/no-empty-function': 'off',
        'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    },
    overrides: [
        {
            // Test dosyalarında component olmayan export'lar (mock factory, helper) normaldir.
            files: ['**/__tests__/**', '**/*.test.ts', '**/*.test.tsx'],
            rules: {
                'react-refresh/only-export-components': 'off',
            },
        },
    ],
};
