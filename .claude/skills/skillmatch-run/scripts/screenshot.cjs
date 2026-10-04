#!/usr/bin/env node
// Captura una página del frontend de SkillMatch con Playwright (Chromium preinstalado).
// Opcionalmente inicia sesión por la UI real antes de navegar, y reporta errores de consola
// y peticiones fallidas para detectar fallos del frontend o de la API.
//
// Uso: node screenshot.cjs <pagina.html> [--login usuario|empresa] [--out archivo.png] [--mobile]
// Ej.: node screenshot.cjs oportunidades.html --login usuario --out /tmp/oportunidades.png

const { execSync } = require('child_process');
const path = require('path');

function loadPlaywright() {
  try {
    return require('playwright');
  } catch (_) {
    const globalRoot = execSync('npm root -g').toString().trim();
    return require(path.join(globalRoot, 'playwright'));
  }
}

function parseArgs(argv) {
  const args = { page: argv[0], login: null, out: null, mobile: false };
  for (let i = 1; i < argv.length; i++) {
    if (argv[i] === '--login') args.login = argv[++i];
    else if (argv[i] === '--out') args.out = argv[++i];
    else if (argv[i] === '--mobile') args.mobile = true;
  }
  if (!args.page) {
    console.error('Uso: node screenshot.cjs <pagina.html> [--login usuario|empresa] [--out archivo.png] [--mobile]');
    process.exit(2);
  }
  if (args.login && !['usuario', 'empresa'].includes(args.login)) {
    console.error('--login debe ser "usuario" o "empresa"');
    process.exit(2);
  }
  args.out ||= path.join(process.env.TMPDIR || '/tmp', `skillmatch-${path.basename(args.page, '.html')}.png`);
  return args;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const baseUrl = process.env.SKILLMATCH_WEB_URL || 'http://localhost:5500/pages';
  const { chromium } = loadPlaywright();

  const browser = await chromium.launch();
  const viewport = args.mobile ? { width: 390, height: 844 } : { width: 1366, height: 900 };
  const page = await browser.newPage({ viewport });

  // Los fallos de la app (localhost) se separan de los recursos externos (CDNs, avatares), que la red
  // de un sandbox puede bloquear sin que sea un bug de SkillMatch.
  let problems = [];
  let externalHosts = new Set();
  const isLocal = url => /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?\//.test(url);
  const record = (url, message) => {
    if (url && !isLocal(url) && !url.startsWith('data:')) externalHosts.add(new URL(url).host);
    else problems.push(message);
  };

  page.on('console', msg => {
    if (msg.type() === 'error') record(msg.location()?.url, `console: ${msg.text()}`);
  });
  page.on('pageerror', err => problems.push(`pageerror: ${err.message}`));
  page.on('response', res => {
    if (res.status() >= 400) record(res.url(), `HTTP ${res.status()} ${res.url()}`);
  });
  page.on('requestfailed', req => {
    record(req.url(), `requestfailed: ${req.url()} (${req.failure()?.errorText})`);
  });

  try {
    if (args.login) {
      await page.goto(`${baseUrl}/login.html`);
      await page.fill('#email', `${args.login}1@skillmatch.com`);
      await page.fill('#password', 'password123');
      await Promise.all([
        page.waitForURL(/perfil-(usuario|empresa)\.html/, { timeout: 15000 }),
        page.click('button[type="submit"]'),
      ]);
      // Dejar que la página de perfil (destino del login) termine sus peticiones; si no, se abortan
      // al navegar y aparecerían como falsos fallos de la página que se quiere capturar.
      await page.waitForLoadState('networkidle');
      console.log(`Sesión iniciada como ${args.login}1@skillmatch.com`);
    }

    problems = [];
    externalHosts = new Set();
    await page.goto(`${baseUrl}/${args.page}`, { waitUntil: 'networkidle' });
    const { scrollWidth, viewportWidth } = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      viewportWidth: window.innerWidth,
    }));
    if (scrollWidth > viewportWidth) {
      problems.push(`layout: desbordamiento horizontal (contenido de ${scrollWidth}px en un viewport de ${viewportWidth}px)`);
    }
    await page.screenshot({ path: args.out, fullPage: true });
    console.log(`Captura guardada en ${args.out}`);
  } finally {
    await browser.close();
  }

  if (problems.length) {
    console.log(`\n${problems.length} problema(s) de la app detectado(s):`);
    problems.forEach(p => console.log(`  - ${p}`));
  } else {
    console.log('Sin errores de la app (consola, JS ni peticiones a localhost).');
  }
  if (externalHosts.size) {
    console.log(`Recursos externos que no cargaron (posible bloqueo de red del entorno): ${[...externalHosts].join(', ')}`);
  }
}

main().catch(err => {
  console.error(`ERROR: ${err.message}`);
  process.exit(1);
});
