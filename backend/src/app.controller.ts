import { Controller, Get, VERSION_NEUTRAL } from '@nestjs/common';
import { seconds, Throttle } from '@nestjs/throttler';
import { ApiRootResource } from './api-root.resource';
import { RedisClientService } from './redis/redis-client.service';

@Controller({ version: VERSION_NEUTRAL })
export class AppController {
  constructor(private readonly redisClient: RedisClientService) {}

  @Throttle({ default: { limit: 3, ttl: seconds(60) } })
  @Get()
  root(): ApiRootResource {
    return new ApiRootResource();
  }

  @Throttle({ default: { limit: 30, ttl: seconds(60) } })
  @Get('health/ready')
  ready(): { status: string } {
    return { status: 'ok' };
  }

  @Throttle({ default: { limit: 30, ttl: seconds(60) } })
  @Get('health/live')
  async live(): Promise<{ status: string }> {
    await this.redisClient.assertReady();
    return { status: 'ok' };
  }
}
