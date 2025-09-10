module.exports = {
    env: {
        browser: true,
        es2021: true,
        node: true,
    },
    extends: [
        'eslint:recommended',
        '@typescript-eslint/recommended',
        'prettier'
    ],
    parser: '@typescript-eslint/parser',
    parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
    },
    plugins: [
        '@typescript-eslint',
        'prettier'
    ],
    rules: {
        // Prettier integration
        'prettier/prettier': 'error',
        
        // Code quality rules
        'no-unused-vars': 'warn',
        'no-undef': 'error',
        'no-console': 'warn',
        'no-debugger': 'error',
        
        // Best practices
        'eqeqeq': 'error',
        'curly': 'error',
        'no-eval': 'error',
        'no-implied-eval': 'error',
        'no-new-func': 'error',
        'no-script-url': 'error',
        
        // TypeScript specific rules
        '@typescript-eslint/no-unused-vars': 'warn',
        '@typescript-eslint/no-explicit-any': 'warn',
        '@typescript-eslint/explicit-function-return-type': 'off',
        '@typescript-eslint/explicit-module-boundary-types': 'off',
        '@typescript-eslint/no-inferrable-types': 'off',
        
        // Style rules (handled by prettier)
        'quotes': ['error', 'single'],
        'semi': ['error', 'always'],
        'comma-dangle': ['error', 'always-multiline'],
        'object-curly-spacing': ['error', 'always'],
        'array-bracket-spacing': ['error', 'never'],
        
        // Import rules
        'import/order': ['error', {
            'groups': [
                'builtin',
                'external',
                'internal',
                'parent',
                'sibling',
                'index'
            ],
            'newlines-between': 'always',
            'alphabetize': {
                'order': 'asc',
                'caseInsensitive': true
            }
        }],
    },
    settings: {
        'import/resolver': {
            'typescript': {}
        }
    },
    ignorePatterns: [
        'node_modules/',
        'build/',
        'dist/',
        '*.min.js',
        '*.bundle.js'
    ]
};