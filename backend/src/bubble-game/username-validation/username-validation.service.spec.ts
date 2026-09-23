import { beforeAll, describe, expect, it, jest } from '@jest/globals';
import { BadRequestException } from '@nestjs/common';
import { UsernameValidationService } from './username-validation.service';

jest.setTimeout(120_000);

const COMMON_NEGATIVE_USERNAMES: { [key: string]: string[] } = {
  English: [
    'shit', // Scheiße
    'fuck', // ficken
    'bitch', // Schlampe
    'asshole', // Arschloch
    'dick', // Schwanz
    'pussy', // Muschi
    'cunt', // Fotze
    'bastard', // Bastard
    'motherfucker', // Mutterficker
    'whore', // Hure
  ],
  Spanish: [
    'mierda', // Scheiße
    'joder', // verdammt / ficken
    'puta', // Hure
    'puto', // männliche Hure
    'cabron', // Arschloch
    'pendejo', // Idiot
    'cojones', // Eier
    'gilipollas', // Vollidiot
    'maricon', // Schwuchtel
    'culero', // Arschloch
  ],
  French: [
    'merde', // Scheiße
    'putain', // verdammt / Hure
    'salope', // Schlampe
    'connard', // Arschloch
    'encule', // Fick dich
    'con', // Idiot
    'bite', // Schwanz
    'chatte', // Muschi
    'bordel', // Bordell / verdammt
    'trouduc', // Arschloch
  ],
  Japanese: [
    'くそ', // Scheiße
    'ばか', // Idiot
    'ちくしょう', // Verdammt
    'くたばれ', // Verreck
    'しね', // Stirb
    'まんこ', // Muschi
    'ちんこ', // Schwanz
    'やりまん', // Schlampe
    'ばばあ', // alte Schachtel
    'きちがい', // Verrückter
  ],
  Portuguese: [
    'merda', // Scheiße
    'porra', // verdammt / Sperma
    'caralho', // Schwanz
    'puta', // Hure
    'puto', // männliche Hure
    'buceta', // Muschi
    'foder', // ficken
    'babaca', // Idiot
    'arrombado', // Arschloch
    'otario', // Trottel
  ],
  Russian: [
    'блядь', // Hure / verdammt
    'сука', // Schlampe
    'хуй', // Schwanz
    'пизда', // Fotze
    'ебать', // ficken
    'долбоеб', // Vollidiot
    'мудак', // Arschloch
    'говно', // Scheiße
    'шлюха', // Hure
    'педик', // Schwuchtel
  ],
  Ukrainian: [
    'лайно', // Scheiße
    'блядь', // Hure / verdammt
    'сука', // Schlampe
    'хуй', // Schwanz
    'пизда', // Fotze
    'йобаний', // verfickt
    'довбень', // Idiot
    'мудак', // Arschloch
    'шлюха', // Hure
    'гівно', // Scheiße
  ],
  Chinese: [
    '操', // ficken / Scheiße
    '傻逼', // Vollidiot
    '他妈的', // verdammt
    '王八蛋', // Bastard
    '去死', // Stirb
    '妈的', // verdammt
    '变态', // Perversling
    '贱人', // Schlampe
    '混蛋', // Bastard
    '狗娘养的', // Hurensohn
  ],
} as const;

describe('UsernameValidationService', () => {
  const service = new UsernameValidationService();

  beforeAll(async () => {
    await service.onModuleInit();
  });

  it.each([
    '卍卍卍',
    '卐卐卐卐卐卐卐卐卐',
    'f4ck',
    'Heil Hitla',
    'Heil Hitle',
    'these jews',
    'Nigger',
    'Nigger9',
    'nigger',
    'niggah',
    'Penis',
    'penis',
    'Sex',
    'cunt',
    'fuck',
    'fuck You',
    'fuckyou',
    'Shitfucker',
    'NutBuster1',
    'peepiss',
    't!ts',
    'thisisshit',
  ])('rejects the profane expression %s', async (username) => {
    await expect(service.validate(username)).rejects.toThrow(
      BadRequestException,
    );
  });

  it.each(['!!!', ',,,,', '...', '184583023', '😄'])(
    'rejects a username without letters: %s',
    async (username) => {
      await expect(service.validate(username)).rejects.toThrow(
        BadRequestException,
      );
    },
  );

  it.each(
    Object.entries(COMMON_NEGATIVE_USERNAMES).flatMap(([language, usernames]) =>
      usernames.map((username) => [language, username] as const),
    ),
  )('rejects common %s profanity: %s', async (_language, username) => {
    await expect(service.validate(username)).rejects.toThrow(
      BadRequestException,
    );
  });

  it.each([
    'BubbleFan! ❤️',
    'Sternenfunke',
    'Wolkenpfad',
    'Kometenflug',
    'Mondschein',
    'Nebelwald',
    'Regenbogen',
    'Sonnenstrahl',
    'Kristallsee',
    'Funkelstein',
    'Abendwind',
    'StarGlimmer',
    'CloudTrail',
    'CometDash',
    'Moonlight',
    'MistForest',
    'RainbowSky',
    'Sunbeam',
    'CrystalLake',
    'SparkStone',
    'EveningWind',
    'RayoClaro',
    'LunaAzul',
    'BrisaSuave',
    'CieloVivo',
    'BosqueNiebla',
    'SolDorado',
    'MarCalmo',
    'EstrellaFiel',
    'NubeVeloz',
    'RioBrillante',
    'LuneClaire',
    'EtoileVive',
    'BriseDouce',
    'CielBleu',
    'ForetBrume',
    'SoleilOr',
    'MerCalme',
    'NuageRapide',
    'RiviereClair',
    'PierreEtincelle',
    '月光',
    '星旅',
    '青空',
    '虹風',
    '海音',
    '雲道',
    '花火',
    '光輪',
    '雪兎',
    '夜空',
    'LuaClara',
    'EstrelaViva',
    'BrisaLeve',
    'CeuAzul',
    'FlorestaNeblina',
    'SolDourado',
    'MarCalmo',
    'NuvemRapida',
    'RioBrilhante',
    'PedraLuz',
    'ЛунныйЛуч',
    'ЗвёздныйПуть',
    'ТихийВетер',
    'СинееНебо',
    'ЛеснойТуман',
    'ЗолотоеСолнце',
    'СпокойноеМоре',
    'БыстроеОблако',
    'ЯркаяРека',
    'ИскраКамня',
    'МісячнийПромінь',
    'ЗорянийШлях',
    'ТихийВітер',
    'СинєНебо',
    'ЛісовийТуман',
    'ЗолотеСонце',
    'СпокійнеМоре',
    'ШвидкаХмара',
    'ЯскраваРічка',
    'ІскраКаменю',
    '月光',
    '星旅',
    '青空',
    '虹風',
    '海音',
    '雲道',
    '花火',
    '光輪',
    '雪兎',
    '夜空',
  ])('accepts the clean username %s', async (username) => {
    await expect(service.validate(username)).resolves.toBeUndefined();
  });
});
