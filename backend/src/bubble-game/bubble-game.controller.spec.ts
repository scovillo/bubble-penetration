import { Test, TestingModule } from '@nestjs/testing';
import { OriginAllowlistGuard } from '../common/origin-allowlist.guard';
import { BubbleGameController } from './bubble-game.controller';
import { BubbleGameService } from './bubble-game.service';
import { PlayerCredentialGuard } from './player/player-credential.guard';

describe('BubbleGameController', () => {
  let controller: BubbleGameController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [BubbleGameController],
      providers: [
        {
          provide: BubbleGameService,
          useValue: {
            createSession: jest.fn(),
            submitScore: jest.fn(),
            getHighscorePage: jest.fn(),
          },
        },
      ],
    })
      .overrideGuard(PlayerCredentialGuard)
      .useValue({ canActivate: jest.fn() })
      .overrideGuard(OriginAllowlistGuard)
      .useValue({ canActivate: jest.fn() })
      .compile();

    controller = module.get<BubbleGameController>(BubbleGameController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
