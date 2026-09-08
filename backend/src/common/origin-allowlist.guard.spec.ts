import { ForbiddenException } from '@nestjs/common';
import { ExecutionContext } from '@nestjs/common/interfaces';
import { OriginAllowlistGuard } from './origin-allowlist.guard';

describe('OriginAllowlistGuard', () => {
  const configService = { get: jest.fn() };
  const guard = new OriginAllowlistGuard(configService as never);

  beforeEach(() => {
    configService.get.mockReset();
  });

  function contextFor(origin?: string): ExecutionContext {
    const request = { headers: { origin } };
    return {
      switchToHttp: () => ({ getRequest: () => request }),
    } as ExecutionContext;
  }

  it('allows any request when no allowlist is configured', () => {
    configService.get.mockReturnValue(undefined);

    expect(guard.canActivate(contextFor())).toBe(true);
  });

  it('rejects a missing Origin header when an allowlist is configured', () => {
    configService.get.mockReturnValue('app://org.codeberg.scovillo.bubble');

    expect(() => guard.canActivate(contextFor())).toThrow(ForbiddenException);
  });

  it('rejects an Origin header not present in the allowlist', () => {
    configService.get.mockReturnValue('app://org.codeberg.scovillo.bubble');

    expect(() => guard.canActivate(contextFor('https://evil.example'))).toThrow(
      ForbiddenException,
    );
  });

  it('accepts an Origin header present in the allowlist', () => {
    configService.get.mockReturnValue(
      'app://org.codeberg.scovillo.bubble, https://bubble.lukas-scheerer.de',
    );

    expect(
      guard.canActivate(contextFor('https://bubble.lukas-scheerer.de')),
    ).toBe(true);
  });
});
