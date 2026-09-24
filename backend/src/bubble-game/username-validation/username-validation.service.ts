import { BadRequestException, Injectable, Logger } from '@nestjs/common';
import { UsernameCustomRule } from './username-custom-rule';

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
  private readonly customRules = [
    UsernameCustomRule.hasValidFormat(),
    UsernameCustomRule.containsLetter(),
  ];

  validate(username: string): Promise<void> {
    return Promise.resolve().then(async () => {
      const failedRule = this.customRules.find((rule) =>
        rule.isViolated(username),
      );
      if (failedRule) {
        this.logger.warn(
          `Username validation in ${username} failed the ${failedRule.name} rule.`,
        );
        throw new BadRequestException(failedRule.rejectionMessage);
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
          throw new BadRequestException(
            'This username was rejected by the content filter.',
          );
        }
        this.logger.log(logMessage);
      }
    });
  }
}
