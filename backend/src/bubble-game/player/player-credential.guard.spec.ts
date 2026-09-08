import { UnauthorizedException } from '@nestjs/common';
import { ExecutionContext } from '@nestjs/common/interfaces';
import { PlayerCredentialGuard } from './player-credential.guard';

describe('PlayerCredentialGuard', () => {
  const credential = 'a'.repeat(43);
  const players = { findOne: jest.fn() };
  const guard = new PlayerCredentialGuard(players as never);

  beforeEach(() => {
    players.findOne.mockReset();
  });

  function contextFor(authorization?: string): {
    context: ExecutionContext;
    request: { headers: { authorization?: string }; player?: unknown };
  } {
    const request = { headers: { authorization } };
    return {
      context: {
        switchToHttp: () => ({ getRequest: () => request }),
      } as ExecutionContext,
      request,
    };
  }

  it('rejects a missing credential without querying the database', async () => {
    const { context } = contextFor();

    await expect(guard.canActivate(context)).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
    expect(players.findOne).not.toHaveBeenCalled();
  });

  it('attaches an active player to the request', async () => {
    const player = { id: 'player-id', blockedAt: undefined };
    players.findOne.mockResolvedValue(player);
    const { context, request } = contextFor(`Bearer ${credential}`);

    await expect(guard.canActivate(context)).resolves.toBe(true);
    expect(request.player).toBe(player);
  });

  it('rejects a blocked player', async () => {
    players.findOne.mockResolvedValue({ blockedAt: new Date() });
    const { context } = contextFor(`Bearer ${credential}`);

    await expect(guard.canActivate(context)).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
  });
});
