import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { App } from 'supertest/types';
import { AppModule } from './../src/app.module';

describe('AppController (e2e)', () => {
  let app: INestApplication<App>;

  beforeEach(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.init();
  });

  it('limits GET / after three requests per minute', async () => {
    const server = app.getHttpServer();

    await request(server).get('/').expect(200);
    await request(server).get('/').expect(200);
    await request(server).get('/').expect(200);
    await request(server).get('/').expect(429);
  });

  afterEach(async () => {
    await app.close();
  });
});
