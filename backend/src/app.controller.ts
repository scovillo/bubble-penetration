import { Controller, Get } from '@nestjs/common';
import { seconds, Throttle } from '@nestjs/throttler';
import { ApiRootResource } from './api-root.resource';

@Controller()
export class AppController {
  @Throttle({ default: { limit: 3, ttl: seconds(60) } })
  @Get()
  root(): ApiRootResource {
    return new ApiRootResource();
  }

  @Get('health')
  health(): { status: string } {
    return { status: 'ok' };
  }
}
