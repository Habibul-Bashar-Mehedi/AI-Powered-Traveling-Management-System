const puppeteer = require('puppeteer-core');

(async () => {
  const browser = await puppeteer.launch({
    executablePath: '/usr/bin/google-chrome',
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1400,1400']
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1400, height: 1400 });

  await page.goto('http://localhost:4300/login', { waitUntil: 'networkidle0', timeout: 30000 });
  await page.type('input[formcontrolname="email"], input[type="email"]', 'admin@test.com');
  await page.type('input[formcontrolname="password"], input[type="password"]', 'Admin@123');
  await Promise.all([
    page.click('button[type="submit"]'),
    page.waitForNavigation({ waitUntil: 'networkidle0', timeout: 15000 }).catch(() => {})
  ]);

  await page.goto('http://localhost:4300/admin/banners', { waitUntil: 'networkidle0', timeout: 30000 });
  await new Promise(r => setTimeout(r, 1500));
  await page.screenshot({ path: '/tmp/claude-1000/-home-habibul-bashar-mehedi-Documents-JAVA-AI-Powered-Traveling-Management-System/3adce222-abc1-4122-b1f0-f42c4bb2efc4/scratchpad/admin-banners-final.png', fullPage: true });

  await page.goto('http://localhost:4300/login', { waitUntil: 'networkidle0', timeout: 30000 });
  await page.type('input[formcontrolname="email"], input[type="email"]', 'user@test.com');
  await page.type('input[formcontrolname="password"], input[type="password"]', 'User@1234');
  await Promise.all([
    page.click('button[type="submit"]'),
    page.waitForNavigation({ waitUntil: 'networkidle0', timeout: 15000 }).catch(() => {})
  ]);
  await new Promise(r => setTimeout(r, 2000));
  await page.screenshot({ path: '/tmp/claude-1000/-home-habibul-bashar-mehedi-Documents-JAVA-AI-Powered-Traveling-Management-System/3adce222-abc1-4122-b1f0-f42c4bb2efc4/scratchpad/dashboard-banners-final.png', fullPage: true });

  await browser.close();
})().catch(err => { console.error('FAILED', err); process.exit(1); });
