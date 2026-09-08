import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
  Logger,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import type { Request } from 'express';
import { parseAllowedOrigins } from '../config/allowed-origins';

@Injectable()
export class OriginAllowlistGuard implements CanActivate {
  private readonly logger = new Logger(OriginAllowlistGuard.name);

  constructor(private readonly configService: ConfigService) {}

  canActivate(context: ExecutionContext): boolean {
    const allowedOrigins = parseAllowedOrigins(
      this.configService.get<string>('ALLOWED_APP_ORIGINS'),
    );

    if (!allowedOrigins) {
      return true;
    }

    const request = context.switchToHttp().getRequest<Request>();
    const origin = request.headers.origin;

    if (!origin || !allowedOrigins.includes(origin)) {
      this.logger.warn(`Rejected request with disallowed origin "${origin}".`);
      throw new ForbiddenException('Origin not allowed.');
    }

    return true;
  }
}
