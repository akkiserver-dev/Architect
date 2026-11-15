/* Progressive enhancement: the whole handbook stays readable without JavaScript. */
(() => {
  const fontButton = document.querySelector('.font-toggle');
  const applyFont = standard => {
    document.documentElement.classList.toggle('standard-font', standard);
    fontButton.setAttribute('aria-pressed', String(standard));
    fontButton.textContent = standard ? 'ドットの字体にする' : '標準の字体にする';
  };
  if (fontButton) {
    let standard = false;
    try { standard = localStorage.getItem('otogi-standard-font') === 'true'; } catch { /* Storage can be disabled. */ }
    applyFont(standard);
    fontButton.hidden = false;
    fontButton.addEventListener('click', () => {
      const next = !document.documentElement.classList.contains('standard-font');
      applyFont(next);
      try { localStorage.setItem('otogi-standard-font', String(next)); } catch { /* Optional preference. */ }
    });
  }

  const search = document.querySelector('#ability-search');
  const kind = document.querySelector('#kind-filter');
  if (!search || !kind) return;
  const cards = [...document.querySelectorAll('.ability-card')];
  const groups = [...document.querySelectorAll('.ability-group')];
  const normalize = value => value.normalize('NFKC').toLocaleLowerCase('ja');
  const searchable = new Map(cards.map(card => [card, normalize(card.textContent)]));
  const filter = () => {
    const terms = normalize(search.value).trim().split(/\s+/).filter(Boolean);
    let count = 0;
    for (const card of cards) {
      const visible = (kind.value === 'all' || card.dataset.kind === kind.value)
        && terms.every(term => searchable.get(card).includes(term));
      card.hidden = !visible;
      if (visible) count++;
    }
    for (const group of groups) group.hidden = ![...group.querySelectorAll('.ability-card')].some(card => !card.hidden);
    document.querySelector('#catalog-count').textContent = `${count} / ${cards.length} 種を表示`;
    document.querySelector('#catalog-empty').hidden = count !== 0;
  };
  document.querySelector('.catalog-tools').hidden = false;
  document.querySelector('.catalog-tools').addEventListener('submit', event => event.preventDefault());
  search.addEventListener('input', filter);
  kind.addEventListener('change', filter);
  filter();
})();
