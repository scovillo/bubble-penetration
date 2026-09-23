/* eslint-disable no-misleading-character-class -- The allowed username character set intentionally permits a ZWJ/variation-selector sequence. */
import { BadRequestException, Injectable, Logger } from '@nestjs/common';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { Profanease } from 'profanease';
import de from 'profanease/langs/de';
import en from 'profanease/langs/en';
import es from 'profanease/langs/es';
import fr from 'profanease/langs/fr';
import ja from 'profanease/langs/ja';
import { default as pt } from 'profanease/langs/pt';
import ru from 'profanease/langs/ru';
import zh from 'profanease/langs/zh';

type MatchOptions = {
  collapseRepeats: boolean;
  leetspeak: boolean;
  splitOnPunctuation: boolean;
};

type ProfanityFilter = {
  addWords(words: string[]): void;
  isProfane(text: string, options: MatchOptions): boolean;
};

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

const USERNAME_PROFANITY_OPTIONS: MatchOptions = {
  collapseRepeats: true,
  leetspeak: true,
  splitOnPunctuation: true,
};

const USERNAME_LANGUAGE_PACKS = [
  'de',
  'en',
  'es',
  'fr',
  'ja',
  'pt',
  'pt_br',
  'uk',
  'ru',
  'zh',
];

@Injectable()
export class UsernameValidationService {
  private readonly logger = new Logger(UsernameValidationService.name);
  private profanityFilter: ProfanityFilter | undefined;
  private readonly filter = new Profanease({
    languages: [de, en, es, fr, ja, pt, ru, zh],
  });

  async onModuleInit(): Promise<void> {
    const badwords = await import('badwords-wasm');
    const badwordsModulePath = require.resolve('badwords-wasm');
    badwords.initSync({
      module: readFileSync(
        join(dirname(badwordsModulePath), 'badwords_wasm_bg.wasm'),
      ),
    });

    const profanityFilter = new badwords.ProfanityFilter(
      true,
      true,
      true,
      true,
    );
    const languagePacks = await Promise.all(
      USERNAME_LANGUAGE_PACKS.map(async (language) => {
        const languagePack = (await import(`@badwords/languages/${language}`, {
          with: { type: 'json' },
        })) as unknown as { default: string[] };
        return languagePack.default;
      }),
    );
    profanityFilter.addWords(languagePacks.flat());
    this.profanityFilter = profanityFilter;
  }

  validate(username: string): Promise<void> {
    return Promise.resolve().then(() => {
      const isCustomRuleViolated = CUSTOM_USERNAME_RULES.some(
        ({ regex, mustMatch }) => regex.test(username) !== mustMatch,
      );
      if (isCustomRuleViolated) {
        this.logger.warn(
          `Username validation in ${username} failed a custom rule.`,
        );
        throw new BadRequestException('This username is not allowed.');
      }
      if (!this.profanityFilter) {
        throw new Error(
          'Username validation service has not been initialized.',
        );
      }
      if (
        this.profanityFilter.isProfane(username, USERNAME_PROFANITY_OPTIONS)
      ) {
        this.logger.warn(`Profanity detected in ${username} by badwords-wasm.`);
        throw new BadRequestException('This username is not allowed.');
      }
      if (this.filter.check(username)) {
        this.logger.warn(`Profanity detected in ${username} by profanease.`);
        throw new BadRequestException('This username is not allowed.');
      }
    });
  }
}
