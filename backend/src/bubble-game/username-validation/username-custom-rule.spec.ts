import { describe, expect, it } from '@jest/globals';
import { UsernameCustomRule } from './username-custom-rule';

describe('UsernameCustomRule', () => {
  const rule = UsernameCustomRule.containsLetter();

  it('identifies a violated rule without losing its explanation', () => {
    expect(rule.isViolated('12345')).toBe(true);
    expect(rule.rejectionMessage).toBe(
      'Username must contain at least one letter.',
    );
  });

  it('accepts a matching username', () => {
    expect(rule.isViolated('BubbleFan')).toBe(false);
  });
});
