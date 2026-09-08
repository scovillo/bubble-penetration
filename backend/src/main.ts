import { ValidationPipe, VersioningType } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { AppLogger } from './common/app-logger.service';
import { parseAllowedOrigins } from './config/allowed-origins';
import { resolveLogLevel } from './config/log-level';

async function bootstrap() {
  const logger = new AppLogger({
    logLevels: [resolveLogLevel(process.env.LOG_LEVEL, process.env.NODE_ENV)],
  });
  const app = await NestFactory.create(AppModule, { logger });
  app.useGlobalPipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
    }),
  );

  const allowedOrigins = parseAllowedOrigins(process.env.ALLOWED_APP_ORIGINS);
  app.enableCors(allowedOrigins ? { origin: allowedOrigins } : undefined);
  app.setGlobalPrefix('api', { exclude: ['health/ready', 'health/live'] });
  app.enableVersioning({ type: VersioningType.URI, defaultVersion: '2' });

  const trustProxyHops = process.env.TRUST_PROXY_HOPS;

  if (trustProxyHops) {
    app
      .getHttpAdapter()
      .getInstance()
      .set('trust proxy', Number.parseInt(trustProxyHops, 10));
  }
  app.enableShutdownHooks();
  await app.listen(process.env.PORT ?? 3000);
  logger.log(
    `Server is running on http://localhost:${process.env.PORT ?? 3000}`,
  );
}
bootstrap().catch((err) => {
  console.error('Error starting the server:', err);
  process.exit(1);
});
