import { MikroOrmModule } from '@mikro-orm/nestjs';
import { ThrottlerStorageRedisService } from '@nest-lab/throttler-storage-redis';
import { MiddlewareConsumer, Module, NestModule } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { APP_GUARD } from '@nestjs/core';
import { seconds, ThrottlerGuard, ThrottlerModule } from '@nestjs/throttler';
import { AppController } from './app.controller';
import { BubbleGameModule } from './bubble-game/bubble-game.module';
import { RequestLoggingMiddleware } from './common/request-logging.middleware';
import { validateEnvironment } from './config/environment.validation';
import mikroOrmConfig from './database/mikro-orm.config';
import { RedisClientService } from './redis/redis-client.service';
import { RedisModule } from './redis/redis.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      cache: true,
      isGlobal: true,
      validate: validateEnvironment,
    }),
    RedisModule,
    ThrottlerModule.forRootAsync({
      imports: [RedisModule],
      inject: [RedisClientService],
      useFactory: (redisClient: RedisClientService) => ({
        throttlers: [
          {
            ttl: seconds(60),
            limit: 30,
          },
        ],
        ...(redisClient.isEnabled()
          ? {
              storage: new ThrottlerStorageRedisService(
                redisClient.getClient(),
              ),
            }
          : {}),
      }),
    }),
    MikroOrmModule.forRoot(mikroOrmConfig),
    BubbleGameModule,
  ],
  controllers: [AppController],
  providers: [
    {
      provide: APP_GUARD,
      useClass: ThrottlerGuard,
    },
  ],
})
export class AppModule implements NestModule {
  configure(consumer: MiddlewareConsumer): void {
    consumer.apply(RequestLoggingMiddleware).forRoutes('*path');
  }
}
