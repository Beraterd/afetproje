/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  safelist: [
    'animate-stagger-1',
    'animate-stagger-2',
    'animate-stagger-3',
    'animate-stagger-4',
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f4f7f5',
          100: '#e5eee7',
          200: '#cbdad0',
          300: '#a4beb0',
          400: '#799e89',
          500: '#59826a',
          600: '#436752',
          700: '#365342',
          800: '#2e4337',
          900: '#26382e',
        },
        earth: {
          50: '#fdfcfb',
          100: '#f8f6f2',
          200: '#eeeae0',
          300: '#dfd8c7',
          400: '#cdbfab',
          500: '#bca48d',
          600: '#af8c73',
          700: '#92715e',
          800: '#795e50',
          900: '#624e43',
        }
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
        display: ['Outfit', 'sans-serif'],
      },
      boxShadow: {
        'soft': '0 4px 20px -2px rgba(67, 103, 82, 0.05)',
        'soft-lg': '0 10px 30px -5px rgba(67, 103, 82, 0.08)',
        'glass': '0 8px 32px 0 rgba(100, 115, 107, 0.07)',
      },
      animation: {
        'fade-in': 'fadeIn 0.5s ease-out',
        'slide-up': 'slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1)',
        'stagger-1': 'slideUp 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.1s both',
        'stagger-2': 'slideUp 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.2s both',
        'stagger-3': 'slideUp 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.3s both',
        'stagger-4': 'slideUp 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.4s both',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        }
      }
    },
  },
  plugins: [],
}