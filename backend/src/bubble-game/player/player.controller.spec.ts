import { Test, TestingModule } from '@nestjs/testing';
import { OriginAllowlistGuard } from '../../common/origin-allowlist.guard';
import { UsernameValidationService } from '../username-validation/username-validation.service';
import { UsernameAvailabilityService } from '../username-validation/username-availability.service';
import { PlayerCredentialGuard } from './player-credential.guard';
import { PlayerController } from './player.controller';
import { PlayerService } from './player.service';

describe('PlayerController', () => {
  let controller: PlayerController;
  let playerService: { createPlayer: jest.Mock };
  let usernameValidationService: { validate: jest.Mock };
  let usernameAvailabilityService: { ensureAvailable: jest.Mock };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [PlayerController],
      providers: [
        {
          provide: PlayerService,
          useValue: { createPlayer: jest.fn() },
        },
        {
          provide: UsernameValidationService,
          useValue: { validate: jest.fn() },
        },
        {
          provide: UsernameAvailabilityService,
          useValue: { ensureAvailable: jest.fn() },
        },
      ],
    })
      .overrideGuard(PlayerCredentialGuard)
      .useValue({ canActivate: jest.fn() })
      .overrideGuard(OriginAllowlistGuard)
      .useValue({ canActivate: jest.fn() })
      .compile();

    controller = module.get<PlayerController>(PlayerController);
    playerService = module.get<{
      createPlayer: jest.Mock,
    }>(PlayerService);
    usernameValidationService = module.get<{
      validate: jest.Mock,
    }>(UsernameValidationService);
    usernameAvailabilityService = module.get<{ ensureAvailable: jest.Mock }>(
      UsernameAvailabilityService,
    );
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  it('validates a username without creating a player', async () => {
    await expect(
      controller.validateUsername({ username: 'BubbleFan' }),
    ).resolves.toEqual({ valid: true });
    expect(usernameValidationService.validate).toHaveBeenCalledWith(
      'BubbleFan',
    );
    expect(usernameAvailabilityService.ensureAvailable).toHaveBeenCalledWith(
      'BubbleFan',
    );
    expect(playerService.createPlayer).not.toHaveBeenCalled();
  });
});
