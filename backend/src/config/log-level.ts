import type { LogLevel } from '@nestjs/common';

const VALID_LOG_LEVELS: readonly LogLevel[] = [
  'verbose',
  'debug',
  'log',
  'warn',
  'error',
  'fatal',
];

export function isValidLogLevel(value: string): value is LogLevel {
  return (VALID_LOG_LEVELS as readonly string[]).includes(
    value.trim().toLowerCase(),
  );
}

export function resolveLogLevel(
  raw: string | undefined,
  nodeEnv: string | undefined,
): LogLevel {
  const normalized = raw?.trim().toLowerCase();

  if (normalized && isValidLogLevel(normalized)) {
    return normalized;
  }

  return nodeEnv === 'production' ? 'log' : 'debug';
}
