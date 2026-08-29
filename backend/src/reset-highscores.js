import { prisma } from './prisma-client.js';
import { resetAll } from './services/highscore.service.js';

try {
  const result = await resetAll();
  console.log(`Reset ${result.count} highscores.`);
} catch (error) {
  console.error('Could not reset highscores.', error);
  process.exitCode = 1;
} finally {
  await prisma.$disconnect();
}
