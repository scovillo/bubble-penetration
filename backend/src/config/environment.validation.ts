import { parseAllowedOrigins } from './allowed-origins';
import { isValidLogLevel } from './log-level';

export function validateEnvironment(environment: Record<string, unknown>) {
  const nodeEnv = environment.NODE_ENV ?? 'development';

  if (typeof nodeEnv !== 'string') {
    throw new Error('NODE_ENV must be a string.');
  }

  const isProduction = nodeEnv === 'production';
  const redisUrl = environment.REDIS_URL;
  const trustProxyHops = environment.TRUST_PROXY_HOPS;

  const logLevel = environment.LOG_LEVEL;

  if (typeof logLevel === 'string' && logLevel && !isValidLogLevel(logLevel)) {
    throw new Error(
      'LOG_LEVEL must be one of verbose, debug, log, warn, error, fatal.',
    );
  }

  if (isProduction && (typeof redisUrl !== 'string' || !redisUrl)) {
    throw new Error('REDIS_URL must be set in production.');
  }

  if (typeof redisUrl === 'string' && redisUrl) {
    let protocol: string;
    try {
      protocol = new URL(redisUrl).protocol;
    } catch {
      throw new Error('REDIS_URL must be a valid Redis URL.');
    }

    if (protocol !== 'redis:' && protocol !== 'rediss:') {
      throw new Error('REDIS_URL must use redis:// or rediss://.');
    }
  }

  if (isProduction && (typeof trustProxyHops !== 'string' || !trustProxyHops)) {
    throw new Error('TRUST_PROXY_HOPS must be set in production.');
  }

  if (typeof trustProxyHops === 'string' && trustProxyHops) {
    const hops = Number.parseInt(trustProxyHops, 10);
    if (!Number.isInteger(hops) || hops < 1 || `${hops}` !== trustProxyHops) {
      throw new Error('TRUST_PROXY_HOPS must be a positive integer.');
    }
  }

  const allowedAppOrigins = environment.ALLOWED_APP_ORIGINS;

  if (typeof allowedAppOrigins === 'string' && allowedAppOrigins) {
    const origins = parseAllowedOrigins(allowedAppOrigins) ?? [];
    for (const origin of origins) {
      try {
        new URL(origin);
      } catch {
        throw new Error(
          'ALLOWED_APP_ORIGINS must be a comma-separated list of valid origins.',
        );
      }
    }
  }

  return environment;
}
