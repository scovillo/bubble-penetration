import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { privacy } from '../website/privacy.mjs';
import { siteUrl, translations } from '../website/translations.mjs';

const root = resolve(import.meta.dirname, '..');
const source = resolve(root, 'website');
const output = resolve(root, '_site');
const template = await readFile(resolve(source, 'template.html'), 'utf8');
const privacyTemplate = await readFile(
  resolve(source, 'privacy-template.html'),
  'utf8',
);
const keys = Object.keys(translations);
const escapeHtml = (value) =>
  String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');
const pathFor = (key) => (key === 'en' ? '' : `${key}/`);

const validateLandingPage = (html, destination) => {
  if (/\{\{\s*[A-Z_]+\s*\}\}/.test(html))
    throw new Error(`Unresolved template placeholder in ${destination}`);

  const jsonLdMatch = html.match(
    /<script type="application\/ld\+json">([\s\S]*?)<\/script>/,
  );
  if (!jsonLdMatch)
    throw new Error(`Missing JSON-LD block in ${destination}`);

  try {
    JSON.parse(jsonLdMatch[1]);
  } catch (error) {
    throw new Error(`Invalid JSON-LD in ${destination}: ${error.message}`);
  }
};

await rm(output, { recursive: true, force: true });
await mkdir(output, { recursive: true });
await cp(resolve(source, 'assets'), resolve(output, 'assets'), {
  recursive: true,
});
await cp(resolve(source, 'styles.css'), resolve(output, 'styles.css'));
await cp(resolve(source, 'app.js'), resolve(output, 'app.js'));
await cp(
  resolve(source, 'google3ab3e439713db7ac.html'),
  resolve(output, 'google3ab3e439713db7ac.html'),
);

for (const [key, copy] of Object.entries(translations)) {
  const base = key === 'en' ? './' : '../';
  const canonical = `${siteUrl}${pathFor(key)}`;
  const socialImage = `${siteUrl}assets/${key === 'de' ? 'social-de.png' : 'social-en.png'}`;
  const hrefLang =
    keys
      .map(
        (lang) =>
          `<link rel="alternate" hreflang="${translations[lang].locale}" href="${siteUrl}${pathFor(lang)}">`,
      )
      .join('\n  ') +
    `\n  <link rel="alternate" hreflang="x-default" href="${siteUrl}">`;
  const ogLocaleAlternates = keys
    .filter((lang) => lang !== key)
    .map(
      (lang) =>
        `<meta property="og:locale:alternate" content="${translations[lang].locale.replace('-', '_')}">`,
    )
    .join('\n  ');
  const options = keys
    .map((lang) => {
      return `<option value="${lang}"${lang === key ? ' selected' : ''}>${escapeHtml(translations[lang].name)}</option>`;
    })
    .join('');
  const jsonLd = JSON.stringify({
    '@context': 'https://schema.org',
    '@graph': [
      {
        '@type': 'WebSite',
        '@id': `${siteUrl}#website`,
        name: 'Bubble Penetration',
        url: siteUrl,
        inLanguage: copy.locale,
      },
      {
        '@type': ['VideoGame', 'SoftwareApplication'],
        '@id': `${siteUrl}#game`,
        name: 'Bubble Penetration',
        description: copy.description,
        url: canonical,
        image: socialImage,
        applicationCategory: 'GameApplication',
        genre: ['Arcade', 'Casual'],
        gamePlatform: 'Android',
        operatingSystem: 'Android',
        playMode: 'SinglePlayer',
        inLanguage: copy.locale,
        isAccessibleForFree: true,
        license: 'https://www.gnu.org/licenses/gpl-3.0.html',
        downloadUrl:
          'https://codeberg.org/scovillo/bubble-penetration/releases/latest',
        sameAs: ['https://codeberg.org/scovillo/bubble-penetration'],
        offers: {
          '@type': 'Offer',
          price: '0',
          priceCurrency: 'EUR',
          availability: 'https://schema.org/InStock',
        },
        featureList: [copy.colorText, copy.starText, copy.comboText],
      },
    ],
  }).replaceAll('<', '\\u003c');
  const values = {
    LANG: copy.locale,
    DIR: 'ltr',
    BASE: base,
    TITLE: copy.title,
    DESCRIPTION: copy.description,
    CANONICAL: canonical,
    HREFLANG: hrefLang,
    OG_LOCALE: copy.locale.replace('-', '_'),
    OG_LOCALE_ALTERNATES: ogLocaleAlternates,
    SOCIAL_IMAGE: socialImage,
    SOCIAL_ALT: `Bubble Penetration — ${copy.heroTitle}`,
    JSON_LD: jsonLd,
    SKIP: copy.skip,
    NAV_LABEL: copy.navLabel,
    NAV_GAME: copy.navGame,
    NAV_SCORES: copy.navScores,
    NAV_PLAY: copy.navPlay,
    LANGUAGE: copy.language,
    LANGUAGE_OPTIONS: options,
    HOME: key === 'en' ? './' : '../',
    EYEBROW: copy.eyebrow,
    HERO_TITLE: copy.heroTitle,
    HERO_LEAD: copy.heroLead,
    DOWNLOAD: copy.download,
    STORE_LABEL: copy.storeLabel,
    GET_IT_ON: copy.getItOn,
    COMING_SOON: copy.comingSoon,
    SEE_SCORES: copy.seeScores,
    FEATURES_LABEL: copy.featuresLabel,
    SECONDS: copy.seconds,
    COMBOS: copy.combos,
    TOP_PLAYERS: copy.topPlayers,
    HOW_LABEL: copy.howLabel,
    HOW_TITLE: copy.howTitle,
    COLOR_TITLE: copy.colorTitle,
    COLOR_TEXT: copy.colorText,
    STAR_TITLE: copy.starTitle,
    STAR_TEXT: copy.starText,
    COMBO_TITLE: copy.comboTitle,
    COMBO_TEXT: copy.comboText,
    SCREENSHOT_LOCALE: copy.screenshotLocale,
    SCREENSHOT_ALT: copy.screenshotAlt,
    LIVE_LABEL: copy.liveLabel,
    SCORE_TITLE: copy.scoreTitle,
    REFRESH: copy.refresh,
    CURRENT_CHAMPION: copy.currentChampion,
    LOADING: copy.loading,
    IN_ACTION: copy.inAction,
    SHOWCASE_TITLE: copy.showcaseTitle,
    SHOT_ONE: copy.shotOne,
    SHOT_TWO: copy.shotTwo,
    SHOT_THREE: copy.shotThree,
    READY: copy.ready,
    CTA_TITLE: copy.ctaTitle,
    CTA_TEXT: copy.ctaText,
    PRIVACY: copy.privacy,
    PRIVACY_HREF: 'privacy/',
    CLIENT_I18N: JSON.stringify({
      loading: copy.loading,
      empty: copy.empty,
      error: copy.error,
      points: copy.points,
    }).replaceAll('<', '\\u003c'),
  };
  let html = template;
  for (const [name, value] of Object.entries(values))
    html = html.replaceAll(`{{${name}}}`, value);
  const destination = key === 'en' ? output : resolve(output, key);
  await mkdir(destination, { recursive: true });
  const pagePath = resolve(destination, 'index.html');
  validateLandingPage(html, pagePath);
  await writeFile(pagePath, html);
}

for (const [key, copy] of Object.entries(translations)) {
  const langPrivacy = privacy[key] ?? privacy.en;
  const privacyBase = key === 'en' ? '../' : '../../';
  const privacyCanonical = `${siteUrl}${pathFor(key)}privacy/`;
  const privacyHreflang = keys
    .map(
      (lang) =>
        `<link rel="alternate" hreflang="${translations[lang].locale}" href="${siteUrl}${pathFor(lang)}privacy/">`,
    )
    .join('\n  ');
  const privacyOptions = keys
    .map(
      (lang) =>
        `<option value="${pathFor(lang)}privacy/"${lang === key ? ' selected' : ''}>${escapeHtml(translations[lang].name)}</option>`,
    )
    .join('');
  const sections = langPrivacy.sections
    .map(
      ([heading, body]) =>
        `<section class="legal-section"><h2>${heading}</h2>${body}</section>`,
    )
    .join('');
  const values = {
    LANG: translations[key].locale,
    BASE: privacyBase,
    TITLE: langPrivacy.title,
    DESCRIPTION: langPrivacy.description,
    CANONICAL: privacyCanonical,
    HREFLANG: privacyHreflang,
    HOME: key === 'en' ? './' : `${key}/`,
    LANGUAGE: copy.language,
    LANGUAGE_OPTIONS: privacyOptions,
    EYEBROW: langPrivacy.eyebrow,
    INTRO: langPrivacy.intro,
    SECTIONS: sections,
    UPDATED: langPrivacy.updated,
    BACK: langPrivacy.back,
  };
  let html = privacyTemplate;
  for (const [name, value] of Object.entries(values))
    html = html.replaceAll(`{{${name}}}`, value);
  const destination = resolve(
    output,
    key === 'en' ? 'privacy' : `${key}/privacy`,
  );
  await mkdir(destination, { recursive: true });
  await writeFile(resolve(destination, 'index.html'), html);
}

const sitemap = keys
  .flatMap((key) => [
    `<url><loc>${siteUrl}${pathFor(key)}</loc><changefreq>weekly</changefreq><priority>${key === 'en' ? '1.0' : '0.8'}</priority></url>`,
    `<url><loc>${siteUrl}${pathFor(key)}privacy/</loc><changefreq>yearly</changefreq><priority>0.5</priority></url>`,
  ])
  .join('');
await writeFile(
  resolve(output, 'sitemap.xml'),
  `<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">${sitemap}</urlset>`,
);
await writeFile(
  resolve(output, 'robots.txt'),
  `User-agent: *\nAllow: /\nSitemap: ${siteUrl}sitemap.xml\n`,
);
await writeFile(
  resolve(output, 'site.webmanifest'),
  JSON.stringify(
    {
      name: 'Bubble Penetration',
      short_name: 'Bubble',
      description: translations.en.description,
      start_url: '/bubble-penetration/',
      scope: '/bubble-penetration/',
      display: 'standalone',
      background_color: '#09031b',
      theme_color: '#09031b',
      icons: [
        {
          src: 'assets/icon.png',
          sizes: '512x512',
          type: 'image/png',
          purpose: 'any maskable',
        },
      ],
    },
    null,
    2,
  ),
);
await writeFile(
  resolve(output, '404.html'),
  `<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><meta http-equiv="refresh" content="0;url=${siteUrl}"><title>Page not found – Bubble Penetration</title><p><a href="${siteUrl}">Continue to Bubble Penetration</a></p></html>`,
);
console.log(`Built ${keys.length} localized pages in ${output}`);
