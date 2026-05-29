// tailwind.config.cjs
module.exports = {
    content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
    theme: {
        extend: {
            colors: {
                riskRed: '#E53935',
                riskYellow: '#FDD835',
                riskGreen: '#43A047',
            },
        },
    },
    plugins: [],
};
