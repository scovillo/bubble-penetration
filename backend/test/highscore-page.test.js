import assert from 'node:assert/strict';
import { test } from 'node:test';
import { getCenteredStartRank } from '../src/highscore-page.js';

test('starts twenty places before the selected player', () => {
  assert.equal(getCenteredStartRank(35), 15);
});

test('does not start before first place', () => {
  assert.equal(getCenteredStartRank(7), 1);
});
