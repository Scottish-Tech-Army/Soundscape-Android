// Accessibility check of the built docs site (docs/_site), run by the
// jekyll-gh-pages workflow after the Jekyll build.
//
// Serves the site locally under its baseurl, loads each page in headless
// Chrome and runs axe-core. WCAG 2.x A/AA violations and JavaScript errors
// fail the check; axe "best-practice" findings are reported as warnings only.
//
// Not every page is checked: the translated pages share the English
// templates, so this checks every English page, every page in one
// left-to-right and one right-to-left translation, and the home page of every
// other language (which catches per-language chrome such as the translated
// screen-reader labels). The generated Dokka code docs are skipped.
//
// Usage: node check.js [site dir] [baseurl]
//   CHROME_PATH   Chrome binary (default /usr/bin/google-chrome)
//   A11Y_ALL=1    check every page in every language (slow: ~2,700 pages)

const fs = require('fs');
const http = require('http');
const path = require('path');
const puppeteer = require('puppeteer-core');

const SITE = path.resolve(process.argv[2] || path.join(__dirname, '../../docs/_site'));
const BASEURL = process.argv[3] || '/Soundscape-Android';
const CHROME = process.env.CHROME_PATH || '/usr/bin/google-chrome';
const FULL_LANGUAGES = ['fr', 'ar'];
const CONCURRENCY = 4;
const FAIL_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];
const axeSource = fs.readFileSync(require.resolve('axe-core/axe.min.js'), 'utf8');

// --- Pick the pages ----------------------------------------------------------

const languages = new Set(fs.readdirSync(SITE).filter(d =>
  /^[a-z]{2,3}(-[A-Z]{2})?$/.test(d) && fs.existsSync(path.join(SITE, d, 'index.html'))));

function htmlFiles(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(e => {
    const full = path.join(dir, e.name);
    if (e.isDirectory()) return htmlFiles(full);
    return e.name.endsWith('.html') ? [full] : [];
  });
}

const urls = htmlFiles(SITE)
  .map(f => '/' + path.relative(SITE, f).split(path.sep).join('/'))
  .filter(u => !u.startsWith('/dokka/') && !u.startsWith('/assets/'))
  .filter(u => {
    const first = u.split('/')[1];
    if (!languages.has(first)) return true;  // English
    if (process.env.A11Y_ALL || FULL_LANGUAGES.includes(first)) return true;
    return u === `/${first}/index.html`;
  })
  .map(u => u.replace(/index\.html$/, ''))
  .sort();

// --- Serve the site under its baseurl ----------------------------------------

const TYPES = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.css': 'text/css',
  '.json': 'application/json', '.geojson': 'application/geo+json', '.svg': 'image/svg+xml',
  '.png': 'image/png', '.ogg': 'audio/ogg', '.pmtiles': 'application/octet-stream',
};

const server = http.createServer((req, res) => {
  let p = decodeURIComponent(req.url.split('?')[0]);
  if (!p.startsWith(BASEURL + '/')) { res.writeHead(404); return res.end(); }
  p = path.join(SITE, p.slice(BASEURL.length));
  if (!p.startsWith(SITE)) { res.writeHead(403); return res.end(); }
  if (fs.existsSync(p) && fs.statSync(p).isDirectory()) p = path.join(p, 'index.html');
  fs.readFile(p, (err, data) => {
    if (err) { res.writeHead(404); return res.end(); }
    res.writeHead(200, { 'Content-Type': TYPES[path.extname(p)] || 'application/octet-stream' });
    res.end(data);
  });
});

// --- Check ---------------------------------------------------------------

async function checkPage(browser, origin, url) {
  const page = await browser.newPage();
  const scriptErrors = [];
  page.on('pageerror', e => scriptErrors.push(e.message));
  await page.setViewport({ width: 1280, height: 900 });
  try {
    await page.goto(origin + BASEURL + url, { waitUntil: 'networkidle2', timeout: 60000 });
  } catch (e) {
    // A slow third-party script (map tiles, Mermaid) shouldn't stop the check.
    console.log(`  (load timeout on ${url}, checking what rendered)`);
  }
  await page.evaluate(axeSource);
  const result = await page.evaluate(() => axe.run(document, {
    runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'],
  }));
  await page.close();
  return { url, scriptErrors, violations: result.violations };
}

(async () => {
  await new Promise(r => server.listen(0, '127.0.0.1', r));
  const origin = `http://127.0.0.1:${server.address().port}`;
  const browser = await puppeteer.launch({ executablePath: CHROME, headless: true, args: ['--no-sandbox'] });
  console.log(`Checking ${urls.length} pages from ${SITE}`);

  const results = [];
  const queue = urls.slice();
  await Promise.all(Array.from({ length: CONCURRENCY }, async () => {
    while (queue.length) {
      const url = queue.shift();
      results.push(await checkPage(browser, origin, url));
    }
  }));
  await browser.close();
  server.close();

  // Group findings by rule and element, since one template problem shows up
  // on every page.
  const groups = new Map();
  for (const r of results) {
    for (const v of r.violations) {
      const failing = v.tags.some(t => FAIL_TAGS.includes(t));
      for (const node of v.nodes) {
        // The failure text (e.g. "contrast of 2.99 (#50a04f on #f5f6fa)") is
        // what one fix addresses; the element differs per highlighted token.
        const detail = (node.failureSummary || '').split('\n').slice(1).join(' ').trim();
        const key = `${v.id} ${detail.replace(/font size: [^,)]*/, '')}`;
        if (!groups.has(key)) {
          groups.set(key, {
            failing, rule: v.id, impact: v.impact, help: v.help, helpUrl: v.helpUrl,
            target: node.target.join(' '),
            detail,
            pages: [],
            elements: 0,
          });
        }
        const g = groups.get(key);
        g.elements++;
        if (!g.pages.includes(r.url)) g.pages.push(r.url);
      }
    }
  }
  const scriptErrors = results.flatMap(r => r.scriptErrors.map(m => ({ url: r.url, message: m })));
  const errors = [...groups.values()].filter(g => g.failing);
  const warnings = [...groups.values()].filter(g => !g.failing);

  const lines = [`## Docs accessibility (axe-core)`, '',
    `Checked ${results.length} pages: ${errors.length} WCAG failure(s), ` +
    `${scriptErrors.length} script error(s), ${warnings.length} best-practice warning(s).`, ''];
  const describe = g => {
    const pages = g.pages.length > 3 ? `${g.pages.slice(0, 3).join(', ')} and ${g.pages.length - 3} more` : g.pages.join(', ');
    return `**${g.rule}** (${g.impact}): ${g.help}\n  - ${g.elements} element(s), e.g. \`${g.target}\`\n  - ${g.detail}\n  - pages: ${pages}\n  - ${g.helpUrl}`;
  };
  if (errors.length) lines.push('### WCAG failures', '', ...errors.map(g => '- ' + describe(g)), '');
  if (scriptErrors.length) lines.push('### Script errors', '', ...scriptErrors.map(e => `- ${e.url}: ${e.message}`), '');
  if (warnings.length) lines.push('### Best-practice warnings', '', ...warnings.map(g => '- ' + describe(g)), '');
  const report = lines.join('\n');
  console.log('\n' + report);
  if (process.env.GITHUB_STEP_SUMMARY) fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, report + '\n');
  if (process.env.GITHUB_ACTIONS) {
    for (const g of errors) console.log(`::error title=axe ${g.rule}::${g.help} (${g.target}) on ${g.pages.length} page(s), e.g. ${g.pages[0]}`);
    for (const e of scriptErrors) console.log(`::error title=Script error::${e.url}: ${e.message}`);
    for (const g of warnings) console.log(`::warning title=axe ${g.rule}::${g.help} (${g.target}) on ${g.pages.length} page(s), e.g. ${g.pages[0]}`);
  }
  process.exit(errors.length || scriptErrors.length ? 1 : 0);
})().catch(e => { console.error(e); process.exit(2); });
