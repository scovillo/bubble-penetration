(() => {
  const config = window.BUBBLE_I18N;
  const list = document.querySelector('#leaderboard');
  const status = document.querySelector('#score-status');
  const refresh = document.querySelector('#refresh-scores');
  const championName = document.querySelector('#champion-name');
  const championScore = document.querySelector('#champion-score');
  const number = new Intl.NumberFormat(document.documentElement.lang);

  document
    .querySelector('#language-select')
    .addEventListener('change', (event) => {
      const locale = event.target.value;
      const isCodebergPages =
        window.location.hostname.endsWith('.codeberg.page');
      const siteRoot = isCodebergPages ? '/bubble-penetration/' : '/';
      const localePath = locale === 'en' ? siteRoot : `${siteRoot}${locale}/`;
      window.location.assign(new URL(localePath, window.location.origin));
    });

  function renderScores(scores) {
    list.replaceChildren();
    scores.slice(0, 10).forEach((entry) => {
      const row = document.createElement('li');
      const player = document.createElement('span');
      const points = document.createElement('strong');
      player.className = 'player';
      points.className = 'points';
      player.textContent = entry.username;
      points.textContent = `${number.format(entry.score)} ${config.points}`;
      row.append(player, points);
      list.append(row);
    });
    const champion = scores[0];
    championName.textContent = champion.username;
    championScore.textContent = number.format(champion.score);
    status.hidden = true;
  }

  async function loadScores() {
    refresh.disabled = true;
    status.hidden = false;
    status.textContent = config.loading;
    try {
      const response = await fetch(
        'https://bubble.lukas-scheerer.de/api/v1/highscores?startRank=1',
        { headers: { Accept: 'application/json' } },
      );
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const payload = await response.json();
      const scores = Array.isArray(payload.highscores)
        ? payload.highscores
        : [];
      if (!scores.length) {
        status.textContent = config.empty;
        championName.textContent = '—';
        championScore.textContent = '—';
        list.replaceChildren();
      } else renderScores(scores);
    } catch (error) {
      console.error('Could not load leaderboard', error);
      status.textContent = config.error;
      championName.textContent = '—';
      championScore.textContent = '—';
      list.replaceChildren();
    } finally {
      refresh.disabled = false;
    }
  }

  refresh.addEventListener('click', loadScores);
  loadScores();
})();
