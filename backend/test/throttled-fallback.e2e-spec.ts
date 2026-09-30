import {
  INestApplication,
  ValidationPipe,
  VersioningType,
} from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import request from 'supertest';
import type { App } from 'supertest/types';
import { AppModule } from '../src/app.module';
import { BubbleGameService } from '../src/bubble-game/bubble-game.service';
import { HighscorePageQueryDto } from '../src/bubble-game/dto/highscore-page-query.dto';

describe('API validation and throttling (e2e)', () => {
  let app: INestApplication<App>;
  let getHighscorePage: jest.Mock;
  let receivedQueries: HighscorePageQueryDto[];

  beforeEach(async () => {
    receivedQueries = [];
    getHighscorePage = jest.fn((query: HighscorePageQueryDto) => {
      receivedQueries.push(query);
      return Promise.resolve({
        highscores: [],
        hasPrevious: false,
        hasNext: false,
      });
    });
    const moduleBuilder = Test.createTestingModule({
      imports: [AppModule],
    })
      .overrideProvider(BubbleGameService)
      .useValue({ getHighscorePage });
    const moduleFixture: TestingModule = await moduleBuilder.compile();

    app = moduleFixture.createNestApplication();
    app.useGlobalPipes(
      new ValidationPipe({
        transform: true,
        whitelist: true,
        forbidNonWhitelisted: true,
      }),
    );
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

  it('rejects limits above 100 with 400 and accepts a limit of 100', async () => {
    const server = app.getHttpServer();

    await request(server)
      .get('/api/v2/bubble-game/highscores?limit=101')
      .expect(400);
    expect(receivedQueries).toHaveLength(0);

    await request(server)
      .get('/api/v2/bubble-game/highscores?limit=100')
      .expect(200);
    expect(receivedQueries[0]).toMatchObject({ limit: 100 });
  });

  it('accepts a request without limit and leaves the default to the service', async () => {
    const server = app.getHttpServer();

    await request(server).get('/api/v2/bubble-game/highscores').expect(200);

    expect(receivedQueries).toHaveLength(1);
    expect(receivedQueries[0].limit).toBeUndefined();
  });

  afterEach(async () => {
    await app.close();
  });
});
