import { Injectable, Logger, NestMiddleware } from '@nestjs/common';
import type { NextFunction, Request, Response } from 'express';
import { createHash, randomUUID } from 'node:crypto';
import { runWithRequestContext } from './request-context';

export const REQUEST_ID_HEADER = 'x-request-id';
const REQUEST_ID_PATTERN = /^[A-Za-z0-9_-]{1,64}$/;
const HEALTH_CHECK_PATH_PATTERN = /(?:^|\/)health(?:\/|$)/;

@Injectable()
export class RequestLoggingMiddleware implements NestMiddleware {
  private readonly logger = new Logger('Request');

  use(request: Request, response: Response, next: NextFunction): void {
    const healthCheck = this.isHealthCheck(request);
    const requestId = this.resolveRequestId(request);
    const clientIpHash = this.hashClientIp(request);
    const proxyPeerHash = this.hashAddress(
      request.socket?.remoteAddress ?? 'unknown',
    );
    const proxyChainLength = request.ips?.length ?? 0;
    const userAgent = this.resolveUserAgent(request);
    const securityContext = `clientIpHash=${clientIpHash} proxyPeerHash=${proxyPeerHash} proxyChainLength=${proxyChainLength} userAgent=${JSON.stringify(userAgent)}`;
    response.setHeader('X-Request-Id', requestId);

    runWithRequestContext({ requestId }, () => {
      const startedAt = process.hrtime.bigint();

      if (!healthCheck) {
        this.logger.log(
          `=> ${request.method} ${request.originalUrl} ${securityContext}`,
        );
      }

      response.on('finish', () => {
        if (healthCheck) {
          return;
        }

        const durationMs =
          Number(process.hrtime.bigint() - startedAt) / 1_000_000;
        this.logger.log(
          `<= ${request.method} ${request.originalUrl} ${response.statusCode} ${durationMs.toFixed(1)}ms ${securityContext}`,
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

  private hashClientIp(request: Request): string {
    const clientIp = request.ip ?? request.socket?.remoteAddress ?? 'unknown';
    return this.hashAddress(clientIp);
  }

  private hashAddress(address: string): string {
    return createHash('sha256').update(address).digest('hex').slice(0, 16);
  }

  private resolveUserAgent(request: Request): string {
    const header: unknown = request.headers['user-agent'];
    const userAgent =
      typeof header === 'string'
        ? header
        : Array.isArray(header) && typeof header[0] === 'string'
          ? header[0]
          : 'unknown';

    return Array.from(userAgent, (character) => {
      const codePoint = character.charCodeAt(0);
      return codePoint <= 0x1f || codePoint === 0x7f ? '?' : character;
    })
      .join('')
      .slice(0, 256);
  }

  private isHealthCheck(request: Request): boolean {
    const path = request.originalUrl.split('?', 1)[0];
    return HEALTH_CHECK_PATH_PATTERN.test(path);
  }
}
