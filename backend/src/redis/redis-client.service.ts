import {
  Injectable,
  OnModuleDestroy,
  OnModuleInit,
  ServiceUnavailableException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import Redis from 'ioredis';

@Injectable()
export class RedisClientService implements OnModuleInit, OnModuleDestroy {
  private readonly client?: Redis;

  constructor(private readonly configService: ConfigService) {
    const redisUrl = this.configService.get<string>('REDIS_URL');

    if (redisUrl) {
      this.client = new Redis(redisUrl, {
        connectTimeout: 5_000,
        enableOfflineQueue: false,
        lazyConnect: true,
        maxRetriesPerRequest: 1,
        retryStrategy: () => null,
      });
    }
  }

  async onModuleInit(): Promise<void> {
    if (!this.client) {
      return;
    }

    await this.client.connect();
    await this.client.ping();
  }

  async onModuleDestroy(): Promise<void> {
    await this.client?.quit();
  }

  isEnabled(): boolean {
    return this.client !== undefined;
  }

  getClient(): Redis {
    if (!this.client) {
      throw new Error('Redis is not configured.');
    }

    return this.client;
  }

  async assertReady(): Promise<void> {
    if (!this.client) {
      return;
    }

    try {
      await this.client.ping();
    } catch {
      throw new ServiceUnavailableException('Redis is unavailable.');
    }
  }
}
