import { validate } from 'class-validator';
import { CreatePlayerDto } from './create-player.dto';

const validateUsername = (username: string) =>
  validate(Object.assign(new CreatePlayerDto(), { username }));

describe('CreatePlayerDto', () => {
  it.each(['Élodie', 'Артём', 'خديجة', '汪芯逸'])(
    'accepts the international username %s',
    async (username) => {
      await expect(validateUsername(username)).resolves.toHaveLength(0);
    },
  );

  it('enforces the minimum length', async () => {
    await expect(validateUsername('')).resolves.not.toHaveLength(0);
    await expect(validateUsername('a')).resolves.toHaveLength(0);
    await expect(validateUsername('Äa')).resolves.toHaveLength(0);
  });
});
