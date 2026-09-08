import { Test, TestingModule } from '@nestjs/testing';
import { OriginAllowlistGuard } from '../../common/origin-allowlist.guard';
import { PlayerCredentialGuard } from './player-credential.guard';
import { PlayerController } from './player.controller';
import { PlayerService } from './player.service';

describe('PlayerController', () => {
  let controller: PlayerController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [PlayerController],
      providers: [
        {
          provide: PlayerService,
          useValue: { createPlayer: jest.fn() },
        },
      ],
    })
      .overrideGuard(PlayerCredentialGuard)
      .useValue({ canActivate: jest.fn() })
      .overrideGuard(OriginAllowlistGuard)
      .useValue({ canActivate: jest.fn() })
      .compile();

    controller = module.get<PlayerController>(PlayerController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
