/** A deterministic username rule with a user-facing explanation. */
const EMOJI_SEQUENCE_PATTERN = String.raw`\p{Extended_Pictographic}(?:\uFE0F)?(?:\u200D\p{Extended_Pictographic}(?:\uFE0F)?)*`;
const USERNAME_CHARACTER_PATTERN = String.raw`(?:[\p{L}\p{N}\p{M} _!\-]|${EMOJI_SEQUENCE_PATTERN})`;
const USERNAME_END_CHARACTER_PATTERN = String.raw`(?:[\p{L}\p{N}\p{M}]|${EMOJI_SEQUENCE_PATTERN})`;

export class UsernameCustomRule {
  private constructor(
    readonly name: string,
    private readonly pattern: RegExp,
    readonly rejectionMessage: string,
  ) {}

  static hasValidFormat(): UsernameCustomRule {
    return new UsernameCustomRule(
      'hasValidFormat',
      new RegExp(
        String.raw`^[\p{L}\p{N}](?:${USERNAME_CHARACTER_PATTERN}*${USERNAME_END_CHARACTER_PATTERN})?$`,
        'u',
      ),
      'Username contains invalid characters or has an invalid start or end.',
    );
  }

  static containsLetter(): UsernameCustomRule {
    return new UsernameCustomRule(
      'containsLetter',
      /\p{L}/u,
      'Username must contain at least one letter.',
    );
  }

  isViolated(username: string): boolean {
    return !this.pattern.test(username);
  }
}
