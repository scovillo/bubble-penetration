import { INestApplication, VersioningType } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { afterEach, beforeEach, describe, it } from 'node:test';
import request from 'supertest';
import type { App } from 'supertest/types';
import { AppModule } from '../src/app.module';

describe('Throttled fallback route (e2e)', () => {
  let app: INestApplication<App>;

  beforeEach(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    app.setGlobalPrefix('api', {
      exclude: ['health/ready', 'health/live'],
    });
    app.enableVersioning({
      type: VersioningType.URI,
      defaultVersion: '2',
    });
    await app.init();
  });

  it('throttles unknown API paths and leaves a valid endpoint reachable', async () => {
    const server = app.getHttpServer();

    await request(server).get('/api/v2/unknown-1').expect(404);
    await request(server).get('/api/v2/unknown-2').expect(404);
    await request(server).get('/api/v2/unknown-3').expect(429);

    await request(server).get('/api').expect(200);
  });

  afterEach(async () => {
    await app.close();
  });
});
