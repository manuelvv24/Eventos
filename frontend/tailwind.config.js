export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Brand
        'primary':                '#3525cd',
        'on-primary':             '#ffffff',
        'primary-container':      '#e4dfff',
        'on-primary-container':   '#0e006f',
        'primary-fixed':          '#e4dfff',
        'on-primary-fixed':       '#0e006f',

        // Secondary
        'secondary':              '#712ae2',
        'on-secondary':           '#ffffff',
        'secondary-container':    '#eedcff',
        'on-secondary-container': '#27005a',

        // Surface scale
        'background':             '#f9f9ff',
        'on-background':          '#151c27',
        'surface':                '#f9f9ff',
        'on-surface':             '#151c27',
        'surface-variant':        '#dce2f3',
        'on-surface-variant':     '#464555',
        'surface-container-lowest':'#ffffff',
        'surface-container-low':  '#f3f3fb',
        'surface-container':      '#e7eefe',
        'surface-container-high': '#e1e1e9',
        'surface-container-highest':'#dcdce4',

        // Inverse
        'inverse-surface':        '#2f3038',
        'inverse-on-surface':     '#f1f0f8',

        // Outline
        'outline':                '#777587',
        'outline-variant':        '#c7c4d8',

        // Error
        'error':                  '#ba1a1a',
        'on-error':               '#ffffff',
        'error-container':        '#ffdad6',
        'on-error-container':     '#410002',

        // Semantic
        'success':                '#1a7f37',
        'success-container':      '#d3f8df',
        'warning':                '#b45309',
        'warning-container':      '#fef3c7',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        'display-lg':  ['3.5625rem', { lineHeight: '4rem',    letterSpacing: '-0.015625em', fontWeight: '400' }],
        'display-md':  ['2.8125rem', { lineHeight: '3.25rem', letterSpacing: '0',           fontWeight: '400' }],
        'display-sm':  ['2.25rem',   { lineHeight: '2.75rem', letterSpacing: '0',           fontWeight: '400' }],
        'headline-lg': ['2rem',      { lineHeight: '2.5rem',  letterSpacing: '0',           fontWeight: '400' }],
        'headline-md': ['1.75rem',   { lineHeight: '2.25rem', letterSpacing: '0',           fontWeight: '400' }],
        'headline-sm': ['1.5rem',    { lineHeight: '2rem',    letterSpacing: '0',           fontWeight: '400' }],
        'title-lg':    ['1.375rem',  { lineHeight: '1.75rem', letterSpacing: '0',           fontWeight: '400' }],
        'title-md':    ['1rem',      { lineHeight: '1.5rem',  letterSpacing: '0.009375em',  fontWeight: '500' }],
        'title-sm':    ['0.875rem',  { lineHeight: '1.25rem', letterSpacing: '0.00625em',   fontWeight: '500' }],
        'body-lg':     ['1rem',      { lineHeight: '1.5rem',  letterSpacing: '0.03125em',   fontWeight: '400' }],
        'body-md':     ['0.875rem',  { lineHeight: '1.25rem', letterSpacing: '0.015625em',  fontWeight: '400' }],
        'body-sm':     ['0.75rem',   { lineHeight: '1rem',    letterSpacing: '0.025em',     fontWeight: '400' }],
        'label-lg':    ['0.875rem',  { lineHeight: '1.25rem', letterSpacing: '0.00625em',   fontWeight: '500' }],
        'label-md':    ['0.75rem',   { lineHeight: '1rem',    letterSpacing: '0.03125em',   fontWeight: '500' }],
        'label-sm':    ['0.6875rem', { lineHeight: '1rem',    letterSpacing: '0.03125em',   fontWeight: '500' }],
      },
      boxShadow: {
        'elevation-1': '0px 1px 2px rgba(0,0,0,0.3), 0px 1px 3px 1px rgba(0,0,0,0.15)',
        'elevation-2': '0px 1px 2px rgba(0,0,0,0.3), 0px 2px 6px 2px rgba(0,0,0,0.15)',
        'elevation-3': '0px 4px 8px 3px rgba(0,0,0,0.15), 0px 1px 3px rgba(0,0,0,0.3)',
      },
      borderRadius: {
        'xs': '4px',
        'sm': '8px',
        'md': '12px',
        'lg': '16px',
        'xl': '20px',
        '2xl': '28px',
        'full': '9999px',
      },
      animation: {
        'spin-slow': 'spin 2s linear infinite',
        'fade-in':   'fadeIn 0.2s ease-out',
        'slide-up':  'slideUp 0.25s ease-out',
      },
      keyframes: {
        fadeIn:  { from: { opacity: 0 }, to: { opacity: 1 } },
        slideUp: { from: { opacity: 0, transform: 'translateY(8px)' }, to: { opacity: 1, transform: 'translateY(0)' } },
      },
    },
  },
  plugins: [],
}
