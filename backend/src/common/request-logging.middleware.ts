import { Injectable, Logger, NestMiddleware } from '@nestjs/common';
import type { NextFunction, Request, Response } from 'express';
import { randomUUID } from 'node:crypto';
import { runWithRequestContext } from './request-context';

export const REQUEST_ID_HEADER = 'x-request-id';
const REQUEST_ID_PATTERN = /^[A-Za-z0-9_-]{1,64}$/;

@Injectable()
export class RequestLoggingMiddleware implements NestMiddleware {
  private readonly logger = new Logger('Request');

  use(request: Request, response: Response, next: NextFunction): void {
    const healthCheck = this.isHealthCheck(request);
    const requestId = this.resolveRequestId(request);
    response.setHeader('X-Request-Id', requestId);

    runWithRequestContext({ requestId }, () => {
      const startedAt = process.hrtime.bigint();

      if (!healthCheck) {
        this.logger.log(`=> ${request.method} ${request.originalUrl}`);
      }

      response.on('finish', () => {
        if (healthCheck) {
          return;
        }

        const durationMs =
          Number(process.hrtime.bigint() - startedAt) / 1_000_000;
        this.logger.log(
          `<= ${request.method} ${request.originalUrl} ${response.statusCode} ${durationMs.toFixed(1)}ms`,
        );
      });

      next();
    });
  }

  private resolveRequestId(request: Request): string {
    const header = request.headers[REQUEST_ID_HEADER];
    const candidate = Array.isArray(header) ? header[0] : header;

    if (candidate && REQUEST_ID_PATTERN.test(candidate)) {
      return candidate;
    }

    const generated = randomUUID();
    if (!this.isHealthCheck(request)) {
      this.logger.warn(
        `Request is missing a valid ${REQUEST_ID_HEADER} header, generated ${generated}.`,
      );
    }
    return generated;
  }

  private isHealthCheck(request: Request): boolean {
    return (
      request.path === '/health/live' || request.path === '/health/ready'
    );
  }
}
