import { Test, TestingModule } from '@nestjs/testing';
import { AppController } from './app.controller';
import { RedisClientService } from './redis/redis-client.service';

describe('AppController', () => {
  let appController: AppController;

  beforeEach(async () => {
    const app: TestingModule = await Test.createTestingModule({
      controllers: [AppController],
      providers: [
        {
          provide: RedisClientService,
          useValue: { assertReady: jest.fn() },
        },
      ],
    }).compile();

    appController = app.get<AppController>(AppController);
  });

  describe('root', () => {
    it('should return an instance of ApiRootResource', () => {
      const result = appController.root();
      expect(result).toHaveProperty('name');
      expect(result).toHaveProperty('version');
      expect(result).toHaveProperty('apiVersions');
      expect(result).toHaveProperty('healthEndpoint');
    });
  });
});
