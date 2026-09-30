import {
  All,
  Controller,
  NotFoundException,
  VERSION_NEUTRAL,
} from '@nestjs/common';
import { minutes, Throttle } from '@nestjs/throttler';

@Controller({ version: VERSION_NEUTRAL })
export class NotFoundController {
  @All('{*path}')
  @Throttle({ default: { limit: 2, ttl: minutes(15) } })
  notFound(): never {
    throw new NotFoundException();
  }
}
