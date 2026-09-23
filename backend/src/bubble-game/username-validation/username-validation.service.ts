/* eslint-disable no-misleading-character-class -- The allowed username character set intentionally permits a ZWJ/variation-selector sequence. */
import { BadRequestException, Injectable, Logger } from '@nestjs/common';

type CustomUsernameRule = {
  regex: RegExp;
  mustMatch: boolean;
};

const CUSTOM_USERNAME_RULES: readonly CustomUsernameRule[] = [
  {
    regex:
      /^[\p{L}\p{N}](?:[\p{L}\p{N}\p{M} _!\-\p{Extended_Pictographic}\u200D\uFE0F]*[\p{L}\p{N}\p{M}\p{Extended_Pictographic}\uFE0F])?$/u,
    mustMatch: true,
  },
  { regex: /\p{L}/u, mustMatch: true },
  { regex: /[\u534D\u5350]/u, mustMatch: false },
];

type InappropriateUsernameResponse = {
  inappropriate: boolean;
};

const INAPPROPRIATE_USERNAME_API_TIMEOUT_MS = 3_000;

@Injectable()
export class UsernameValidationService {
  private readonly logger = new Logger(UsernameValidationService.name);

  validate(username: string): Promise<void> {
    return Promise.resolve().then(async () => {
      const isCustomRuleViolated = CUSTOM_USERNAME_RULES.some(
        ({ regex, mustMatch }) => regex.test(username) !== mustMatch,
      );
      if (isCustomRuleViolated) {
        this.logger.warn(
          `Username validation in ${username} failed a custom rule.`,
        );
        throw new BadRequestException('This username is not allowed.');
      }
      const classifierUrl = process.env.INAPPROPRIATE_USERNAME_API_URL;
      if (classifierUrl) {
        const response = await fetch(
          `${classifierUrl.replace(/\/$/, '')}/v1/predict`,
          {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username }),
            signal: AbortSignal.timeout(INAPPROPRIATE_USERNAME_API_TIMEOUT_MS),
          },
        );
        if (!response.ok) {
          throw new Error(
            `Inappropriate username API returned HTTP ${response.status}.`,
          );
        }

        const result = (await response.json()) as InappropriateUsernameResponse;
        if (typeof result.inappropriate !== 'boolean') {
          throw new Error(
            'Inappropriate username API returned an invalid response.',
          );
        }
        if (result.inappropriate) {
          this.logger.warn('Inappropriate username detected by classifier.');
          throw new BadRequestException('This username is not allowed.');
        }
      }
    });
  }
}
