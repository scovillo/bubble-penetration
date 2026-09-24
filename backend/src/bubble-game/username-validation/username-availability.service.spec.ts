import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { ConflictException } from '@nestjs/common';
import { UsernameAvailabilityService } from './username-availability.service';

describe('UsernameAvailabilityService', () => {
  const players = { findOne: jest.fn() };
  const service = new UsernameAvailabilityService(players as never);

  beforeEach(() => players.findOne.mockReset());

  it('checks availability case-insensitively', async () => {
    players.findOne.mockResolvedValue(null);

    await expect(service.ensureAvailable('BubbleFan')).resolves.toBeUndefined();
    expect(players.findOne).toHaveBeenCalledWith({ usernameKey: 'bubblefan' });
  });

  it('rejects an already registered username', async () => {
    players.findOne.mockResolvedValue({ id: 'existing-player' });

    await expect(service.ensureAvailable('BubbleFan')).rejects.toBeInstanceOf(
      ConflictException,
    );
  });
});
