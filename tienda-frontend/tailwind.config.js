/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  corePlugins: {
    preflight: false,
  },
  theme: {
    extend: {
      colors: {
        ink: { DEFAULT: '#1B201D', soft: '#5A635C' },
        piedra: { 50: '#F3F4F0', 100: '#E8EAE3', 300: '#C9CEC3' },
        musgo: { DEFAULT: '#2E4A3B', deep: '#1F3328', soft: '#DDE7E0' },
        linterna: { DEFAULT: '#E9A23B', soft: '#FBE9C8' },
      },
      fontFamily: {
        display: ['"Bricolage Grotesque"', 'system-ui', 'sans-serif'],
        sans: ['"Instrument Sans"', 'system-ui', 'sans-serif'],
        mono: ['"DM Mono"', 'ui-monospace', 'monospace'],
      },
    },
  },
  plugins: [],
};