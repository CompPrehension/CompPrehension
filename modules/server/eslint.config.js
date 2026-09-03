import js from '@eslint/js';
import { defineConfig, globalIgnores } from 'eslint/config';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import unusedImports from 'eslint-plugin-unused-imports';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default defineConfig([
  globalIgnores([
    'src/main/resources/**', // build output and backend resources
    'public/**',             // generated msw service worker
    'target/**',
    'node_modules/**',
  ]),

  // frontend sources
  {
    files: ['src/main/js/**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs['recommended-latest'],
      reactRefresh.configs.vite,
    ],
    plugins: { 'unused-imports': unusedImports },
    languageOptions: {
      ecmaVersion: 'latest',
      globals: globals.browser,
    },
    rules: {
      // the codebase leans on inference; annotating every boundary is not the house style
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      // `any` is still used in a few dom-wrangling spots; worth seeing, not worth blocking
      '@typescript-eslint/no-explicit-any': 'warn',
      // an unused import is always a mistake and removing one is safe, so it blocks;
      // dead locals inside a component are existing debt, so they only nag
      '@typescript-eslint/no-unused-vars': 'off',
      'unused-imports/no-unused-imports': 'error',
      'unused-imports/no-unused-vars': ['warn', {
        args: 'after-used',
        argsIgnorePattern: '^_',
        varsIgnorePattern: '^_',
        caughtErrors: 'none',
      }],
      // `interface X extends Y {}` is how io-ts declares its codec types - that one is fine,
      // a bare `{}` type is not
      '@typescript-eslint/no-empty-object-type': ['error', { allowInterfaces: 'with-single-extends' }],
      // advisory: it only costs a full reload instead of a hot update, and obeying it would
      // mean splitting files apart for the sake of the dev server
      'react-refresh/only-export-components': 'warn',
    },
  },

  // build tooling runs in node
  {
    files: ['vite.config.ts'],
    extends: [js.configs.recommended, tseslint.configs.recommended],
    languageOptions: {
      ecmaVersion: 'latest',
      globals: globals.node,
    },
  },
]);
