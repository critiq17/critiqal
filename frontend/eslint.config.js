import js from '@eslint/js';
import ts from '@typescript-eslint/eslint-plugin';
import tsParser from '@typescript-eslint/parser';
import svelte from 'eslint-plugin-svelte';
import svelteParser from 'svelte-eslint-parser';
import globals from 'globals';

const svelteRunes = {
	$state: 'readonly',
	$derived: 'readonly',
	$effect: 'readonly',
	$props: 'readonly',
	$bindable: 'readonly',
	$inspect: 'readonly',
	$host: 'readonly',
};

const unusedVarsOptions = {
	argsIgnorePattern: '^_',
	varsIgnorePattern: '^_',
	caughtErrorsIgnorePattern: '^_',
};

export default [
	js.configs.recommended,
	{
		files: ['**/*.ts'],
		languageOptions: { parser: tsParser, globals: { ...globals.browser, ...globals.node } },
		plugins: { '@typescript-eslint': ts },
		rules: {
			...ts.configs.recommended.rules,
			'no-console': 'error',
			'no-undef': 'off',
			'no-unused-vars': 'off',
			'@typescript-eslint/no-explicit-any': 'error',
			'@typescript-eslint/no-unused-vars': ['error', unusedVarsOptions],
		},
	},
	{
		files: ['**/*.svelte.ts'],
		languageOptions: {
			parser: tsParser,
			globals: { ...globals.browser, ...globals.node, ...svelteRunes },
		},
		plugins: { '@typescript-eslint': ts },
		rules: {
			...ts.configs.recommended.rules,
			'no-console': 'error',
			'no-undef': 'off',
			'no-unused-vars': 'off',
			'@typescript-eslint/no-explicit-any': 'error',
			'@typescript-eslint/no-unused-vars': ['error', unusedVarsOptions],
		},
	},
	{
		files: ['**/*.svelte'],
		languageOptions: {
			parser: svelteParser,
			parserOptions: { parser: tsParser },
			globals: { ...globals.browser, ...svelteRunes },
		},
		plugins: { svelte, '@typescript-eslint': ts },
		rules: {
			...svelte.configs.recommended.rules,
			'no-console': 'error',
			'no-unused-vars': 'off',
			'@typescript-eslint/no-unused-vars': ['error', unusedVarsOptions],
		},
	},
	{ ignores: ['build/', '.svelte-kit/', 'node_modules/', 'coverage/'] },
];
