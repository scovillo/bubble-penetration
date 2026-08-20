import { prisma } from '../prisma-client.js';
import { log } from '../middlewares/logging.middleware.js';

/**
 * Makes sure that light, invalid cases are catched and handled.
 * <ul>
 *     <li>Highscores lower than 0 will be set to 0</li>
 *     <li>Highscores passed as a string will be parsed to an integer value</li>
 * </ul>
 * @param highscore Value that is tried to be used
 * @returns Adjusted highscore value
 */
export function parseHighscore(highscore) {
    if (typeof highscore === 'string') {
        highscore = parseInt(highscore, 10);
    }
    if (!highscore || highscore < 0) {
        highscore = 0;
    }

    return highscore;
}

export function findAll() {
    return prisma.highscores.findMany({
        orderBy: [{ highscore: 'desc' }]
    });
}

export function findHighscore(username) {
    return prisma.highscores.findFirst({
        where: { username },
        orderBy: [{ highscore: 'desc' }]
    });
}

export function create(username, highscore) {
    const score = parseHighscore(highscore);

    log.info(`Create new highscore for user ${username}: ${score} points`);
    return prisma.highscores.create({
        data: {
            username,
            'highscore': score
        }
    });
}

export function update(username, highscore) {
    const score = parseHighscore(highscore);

    log.info(`Set new highscore for user ${username}: ${score} points`);
    return prisma.highscores.update({
        where: { username },
        data: {
            'highscore': score
        }
    });
}

export async function save(username, highscore) {
    const existingHighscore = await findHighscore(username);
    if (!existingHighscore) {
        return create(username, highscore);
    }

    log.debug(`Found existing highscore: ${JSON.stringify(existingHighscore)}`);
    const score = parseHighscore(highscore);
    if (existingHighscore.highscore >= score) {
        log.info(`Skip highscore update for user ${username}`);
        log.debug(`Posted highscore ${score} was lower than or equal to existing highscore ${existingHighscore.highscore}`);
        return existingHighscore;
    }

    return update(username, score);
}
