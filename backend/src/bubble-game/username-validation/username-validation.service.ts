import { BadRequestException, Injectable, Logger } from '@nestjs/common';

type CustomUsernameRule = {
  regex: RegExp;
  mustMatch: boolean;
};

const EMOJI_SEQUENCE_PATTERN = String.raw`\p{Extended_Pictographic}(?:\uFE0F)?(?:\u200D\p{Extended_Pictographic}(?:\uFE0F)?)*`;
const USERNAME_CHARACTER_PATTERN = String.raw`(?:[\p{L}\p{N}\p{M} _!\-]|${EMOJI_SEQUENCE_PATTERN})`;
const USERNAME_END_CHARACTER_PATTERN = String.raw`(?:[\p{L}\p{N}\p{M}]|${EMOJI_SEQUENCE_PATTERN})`;
const USERNAME_FORMAT_REGEX = new RegExp(
  String.raw`^[\p{L}\p{N}](?:${USERNAME_CHARACTER_PATTERN}*${USERNAME_END_CHARACTER_PATTERN})?$`,
  'u',
);

const CUSTOM_USERNAME_RULES: readonly CustomUsernameRule[] = [
  {
    // Allow Unicode letters/numbers, combining marks, spaces, !, -, and emoji.
    // Names must start and end with a letter, number, mark, or emoji; ZWJ and
    // variation selectors are handled as part of complete emoji sequences.
    regex: USERNAME_FORMAT_REGEX,
    mustMatch: true,
  },
  // Require at least one Unicode letter; numbers or emoji alone are rejected.
  { regex: /\p{L}/u, mustMatch: true },
];

interface InappropriateUsernameResponse {
  username: string;
  inappropriate: boolean;
  classification: 'inappropriate' | 'clean';
  probability: number;
  confidence: number;
  margin: number;
  threshold_distance: number;
  threshold: number;
  model_version: string;
}

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
            signal: AbortSignal.timeout(3_000),
          },
        );
        if (!response.ok) {
          throw new Error(
            `Inappropriate username API returned HTTP ${response.status}.`,
          );
        }
        const result = (await response.json()) as InappropriateUsernameResponse;
        const logMessage = `Username classification: ${JSON.stringify(result)}`;
        if (result.inappropriate) {
          this.logger.warn(logMessage);
          throw new BadRequestException('This username is not allowed.');
        }
        this.logger.log(logMessage);
      }
    });
  }
}
