/**
 * Risk bazlı senaryo hedeflerine göre testdata JSON dosyalarını günceller.
 * Çalıştır: node scripts/rebalance-scenario-targets.js
 */
const fs = require('fs');
const path = require('path');

const TESTDATA = path.join(__dirname, '..', 'src', 'test', 'resources', 'testdata');

const TARGETS = {
  Checkout: 28,
  Login: 24,
  ForgotPassword: 24,
  Register: 22,
  Cart: 22,
  MyAccountSettings: 22,
  MyAccount: 20,
  Search: 18,
  WishList: 12,
  Newsletter: 10,
  Api: 32,
  Db: 35,
  E2EOrderFlow: 4
};

const FILE_MAP = {
  Checkout: 'checkout-scenarios.json',
  Login: 'login-scenarios.json',
  ForgotPassword: 'forgot-password-scenarios.json',
  Register: 'register-scenarios.json',
  Cart: 'cart-scenarios.json',
  MyAccountSettings: 'my-account-settings-scenarios.json',
  MyAccount: 'my-account-scenarios.json',
  Search: 'search-scenarios.json',
  WishList: 'wishlist-scenarios.json',
  Newsletter: 'newsletter-scenarios.json',
  Api: 'api-scenarios.json',
  Db: 'db-scenarios.json',
  E2EOrderFlow: 'e2e-scenarios.json'
};

const TRIM = {
  'newsletter-scenarios.json': new Set([
    'NWS-003', 'NWS-EP-01', 'NWS-EP-02', 'NWS-ST-02', 'NWS-ST-04',
    'NWS-EG-01', 'NWS-EG-02', 'NWS-BV-03'
  ]),
  'wishlist-scenarios.json': new Set([
    'WLS-004', 'WLS-ST-02', 'WLS-EP-01', 'WLS-EG-02', 'WLS-NEG-04', 'WLS-BV-03'
  ])
};

function readJson(file) {
  return JSON.parse(fs.readFileSync(path.join(TESTDATA, file), 'utf8'));
}

function writeJson(file, data) {
  fs.writeFileSync(path.join(TESTDATA, file), JSON.stringify(data, null, 2) + '\n', 'utf8');
}

function clone(obj) {
  return JSON.parse(JSON.stringify(obj));
}

function addCheckout(arr, n) {
  const products = ['iPhone', 'iPod', 'Samsung', 'Dell', 'HP', 'Nikon', 'Apple Cinema', 'Palm Treo', 'Mac Mini', 'Surface'];
  const base = arr.find((s) => s.requireProductInCart === true && s.expectCheckoutAccessible === true) || arr[0];
  for (let i = 0; i < n; i++) {
    const p = products[i % products.length];
    const item = clone(base);
    item.caseId = 'CHK-RB-' + String(i + 1).padStart(2, '0');
    item.description = 'Risk-based: ' + p + ' ile checkout erişimi (ödeme akışı)';
    item.priority = i < 4 ? 'P1' : 'P2';
    item.technique = ['state-transition', 'equivalence-partitioning', 'boundary-value', 'decision-table'][i % 4];
    item.productNameContains = p;
    item.persistOrderToDb = i < 3;
    item.expectedDbStatus = i < 3 ? 'CHECKOUT_STARTED' : undefined;
    arr.push(item);
  }
}

function addLogin(arr, n) {
  const variants = [
    { email: 'locked@example.com', password: 'wrong1', desc: 'Kilitli hesap benzeri e-posta ile giriş' },
    { email: 'test+alias@example.com', password: 'wrong2', desc: 'E-posta alias (+) karakteri ile giriş' },
    { email: 'user@sub.domain.com', password: 'wrong3', desc: 'Alt domain e-posta formatı' },
    { email: 'UPPER@EXAMPLE.COM', password: 'wrong4', desc: 'Büyük harf e-posta duyarlılığı' },
    { email: 'spaces@example.com', password: 'pass word', desc: 'Şifrede boşluk karakteri' },
    { email: 'unicode@example.com', password: 'şifre123', desc: 'Unicode karakterli şifre denemesi' }
  ];
  const base = arr.find((s) => s.expectWarning === true) || arr[0];
  for (let i = 0; i < n; i++) {
    const v = variants[i];
    const item = clone(base);
    item.caseId = 'LGN-RB-' + String(i + 1).padStart(2, '0');
    item.description = v.desc;
    item.email = v.email;
    item.password = v.password;
    item.technique = i % 2 === 0 ? 'equivalence-partitioning' : 'error-guessing';
    item.priority = 'P1';
    arr.push(item);
  }
}

function addForgotPassword(arr, n) {
  const emails = [
    'disposable@mailinator.com',
    'plus+tag@example.com',
    'longlocalpart_' + 'x'.repeat(40) + '@example.com',
    'user.name@company.co.uk',
    '123@456.789',
    'no-at-sign'
  ];
  const base = arr.find((s) => s.caseId === 'FPW-001') || arr[0];
  for (let i = 0; i < n; i++) {
    const item = clone(base);
    item.caseId = 'FPW-RB-' + String(i + 1).padStart(2, '0');
    item.description = 'Risk-based: e-posta formatı / güvenlik — ' + emails[i];
    item.email = emails[i];
    item.technique = i < 3 ? 'boundary-value' : 'equivalence-partitioning';
    item.standard = i < 2 ? 'OWASP' : 'ISTQB';
    item.priority = 'P1';
    arr.push(item);
  }
}

function addRegister(arr, n) {
  const base = arr.find((s) => s.expectSuccess === true) || arr[0];
  for (let i = 0; i < n; i++) {
    const item = clone(base);
    item.caseId = 'RGN-RB-' + String(i + 1).padStart(2, '0');
    item.description = 'Risk-based: kayıt formu varyasyonu #' + (i + 1);
    item.firstName = 'Risk' + i;
    item.lastName = 'User' + i;
    item.email = 'rb.user' + i + '+${timestamp}@example.com';
    item.telephone = '+155500' + String(1000 + i);
    item.password = 'SecurePass!' + i;
    item.confirmPassword = item.password;
    item.subscribeNewsletter = i % 2 === 0;
    item.expectSuccess = true;
    item.expectedSuccessContains = 'created';
    item.technique = ['equivalence-partitioning', 'boundary-value', 'negative-test', 'error-guessing'][i];
    item.priority = i < 2 ? 'P1' : 'P2';
    arr.push(item);
  }
}

function addCart(arr, n) {
  const products = ['iMac', 'HTC', 'MacBook', 'Canon'];
  const base = arr.find((s) => s.expectAddSuccess === true) || arr[0];
  for (let i = 0; i < n; i++) {
    const item = clone(base);
    item.caseId = 'CRT-RB-' + String(i + 1).padStart(2, '0');
    item.description = 'Risk-based: sepete ' + products[i] + ' ekleme';
    item.productNameContains = products[i];
    item.cartQuantity = i === 1 ? 2 : 1;
    item.expectAddSuccess = true;
    item.expectedCartContains = products[i];
    item.technique = ['state-transition', 'boundary-value', 'equivalence-partitioning', 'decision-table'][i];
    item.priority = 'P1';
    arr.push(item);
  }
}

function addMyAccount(arr, n) {
  const links = [
    { text: 'Reward Points', url: 'reward', body: 'Reward' },
    { text: 'Transactions', url: 'transaction', body: 'Transaction' }
  ];
  const base = arr.find((s) => s.sidebarLinkText && s.sidebarLinkText.length > 0) || arr[0];
  for (let i = 0; i < n; i++) {
    const link = links[i];
    const item = clone(base);
    item.caseId = 'MAC-RB-' + String(i + 1).padStart(2, '0');
    item.description = 'Risk-based: ' + link.text + ' sayfasına gezinme';
    item.sidebarLinkText = link.text;
    item.expectedUrlContains = link.url;
    item.expectedBodyContains = link.body;
    item.technique = i === 0 ? 'state-transition' : 'equivalence-partitioning';
    item.priority = 'P1';
    arr.push(item);
  }
}

function addMyAccountSettings(arr, n) {
  const flows = [
    { flow: 'edit-information', desc: 'Profil telefon güncelleme (risk)', tel: '+15559990001' },
    { flow: 'edit-information', desc: 'Profil ad güncelleme (risk)', tel: '+15559990002' },
    { flow: 'address-book', desc: 'Adres defteri — İzmir kaydı', city: 'Izmir', pc: '35000' },
    { flow: 'change-password', desc: 'Şifre değiştirme negatif — kısa şifre', pwd: '123', ok: false }
  ];
  for (let i = 0; i < n; i++) {
    const f = flows[i];
    const template = arr.find((s) => s.flow === f.flow) || arr[0];
    const item = clone(template);
    item.caseId = 'MAC-SET-RB-' + String(i + 1).padStart(2, '0');
    item.description = f.desc;
    item.flow = f.flow;
    if (f.flow === 'edit-information') {
      item.firstName = 'RiskEdit' + i;
      item.telephone = f.tel;
      item.expectSuccess = true;
      item.expectedMessageContains = 'success';
    } else if (f.flow === 'address-book') {
      item.city = f.city;
      item.postcode = f.pc;
      item.addressLine1 = 'Risk Cad. No ' + (i + 1);
      item.expectSuccess = true;
    } else if (f.flow === 'change-password') {
      item.newPassword = f.pwd;
      item.confirmNewPassword = f.pwd;
      item.expectSuccess = f.ok;
      item.expectedMessageContains = 'Password';
    }
    item.technique = ['state-transition', 'boundary-value', 'negative-test', 'equivalence-partitioning'][i];
    item.priority = 'P1';
    arr.push(item);
  }
}

function addE2E(arr, n) {
  const products = ['HTC', 'MacBook', 'Canon'];
  const base = arr[0];
  for (let i = 0; i < n; i++) {
    const item = clone(base);
    item.caseId = 'E2E-RB-' + String(i + 1).padStart(2, '0');
    item.description = 'E2E: ' + products[i] + ' checkout → API → SQL doğrulama';
    item.productNameContains = products[i];
    item.technique = ['state-transition', 'equivalence-partitioning', 'boundary-value'][i];
    item.priority = i === 0 ? 'P0' : 'P1';
    arr.push(item);
  }
}

const ADDERS = {
  Checkout: addCheckout,
  Login: addLogin,
  ForgotPassword: addForgotPassword,
  Register: addRegister,
  Cart: addCart,
  MyAccount: addMyAccount,
  MyAccountSettings: addMyAccountSettings,
  E2EOrderFlow: addE2E
};

console.log('Risk bazlı senaryo dengeleme\n');

Object.entries(FILE_MAP).forEach(([module, file]) => {
  let arr = readJson(file);
  const trimSet = TRIM[file];
  if (trimSet) {
    const before = arr.length;
    arr = arr.filter((s) => !trimSet.has(s.caseId));
    console.log(file + ': trimmed ' + (before - arr.length) + ' redundant scenarios');
  }
  const target = TARGETS[module];
  if (target == null) return;
  const need = target - arr.length;
  if (need > 0 && ADDERS[module]) {
    ADDERS[module](arr, need);
    console.log(file + ': added ' + need + ' scenarios → ' + arr.length);
  } else if (need < 0) {
    console.warn(file + ': still ' + arr.length + ' scenarios, target ' + target + ' (manual trim needed)');
  } else {
    console.log(file + ': unchanged at ' + arr.length);
  }
  writeJson(file, arr);
});

console.log('\nDone. Update dashboard MODULE_SCENARIO_TARGETS to match TARGETS (+ Db dashboard uses 36 with SqlServerDb).');
