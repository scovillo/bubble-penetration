import { getRepositoryToken } from '@mikro-orm/nestjs';
import { ConflictException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { Player } from '../entities/player.entity';
import { PlayerService } from './player.service';

describe('PlayerService', () => {
  let service: PlayerService;

  beforeEach(async () => {
    const players = {
      findOne: jest.fn(),
      create: jest.fn(),
      getEntityManager: jest.fn(),
    };
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        PlayerService,
        { provide: getRepositoryToken(Player), useValue: players },
      ],
    }).compile();

    service = module.get<PlayerService>(PlayerService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('rejects an existing username', async () => {
    const players = (service as unknown as { players: { findOne: jest.Mock } })
      .players;
    players.findOne.mockResolvedValue({});

    await expect(service.createPlayer('BubbleFan')).rejects.toBeInstanceOf(
      ConflictException,
    );
  });
});
