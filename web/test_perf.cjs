const { performance } = require('perf_hooks');

const generateData = (numLines, threadsPerLine, numLegacy) => {
  const lineThreads = {};
  for (let i = 0; i < numLines; i++) {
    const threads = [];
    for (let j = 0; j < threadsPerLine; j++) {
      threads.push({
        id: `t_${i}_${j}`,
        address: j % 2 === 0 ? `address_${j}` : null,
        archived: j % 3 === 0,
        pinned: j % 5 === 0,
        date: Date.now() - Math.random() * 10000
      });
    }
    lineThreads[`line_${i}`] = threads;
  }

  const legacyThreads = [];
  for (let i = 0; i < numLegacy; i++) {
    legacyThreads.push({
      id: `legacy_${i}`,
      address: `address_${i % (threadsPerLine * 2)}`,
      archived: i % 4 === 0,
      pinned: i % 6 === 0,
      date: Date.now() - Math.random() * 10000
    });
  }

  return { lineThreads, legacyThreads };
};

const original = (lineThreads, legacyThreads, lineInboxMode, showArchived) => {
  if (lineInboxMode === 'PER_LINE') return [];
  const lineFlattened = Object.values(lineThreads).flat();

  const lineAddresses = new Set(lineFlattened.map(t => t.address).filter(Boolean));
  const uniqueLegacy = legacyThreads.filter(t => !t.address || !lineAddresses.has(t.address));

  const all = [...uniqueLegacy, ...lineFlattened];

  const filtered = all.filter(t => showArchived ? t.archived : !t.archived);

  return filtered.sort((a, b) => {
    if (a.pinned !== b.pinned) return a.pinned ? -1 : 1;
    return (b.date ?? 0) - (a.date ?? 0);
  });
};

const optimized = (lineThreads, legacyThreads, lineInboxMode, showArchived) => {
  if (lineInboxMode === 'PER_LINE') return [];

  const lineFlattened = [];
  const lineAddresses = new Set();

  for (const threads of Object.values(lineThreads)) {
    const items = Array.isArray(threads) ? threads : [threads];
    for (const t of items) {
      if (!t) continue;
      lineFlattened.push(t);
      if (t.address) {
        lineAddresses.add(t.address);
      }
    }
  }

  const filtered = [];

  for (const t of legacyThreads) {
    if (!t) continue;
    if (!t.address || !lineAddresses.has(t.address)) {
      if (showArchived ? t.archived : !t.archived) {
        filtered.push(t);
      }
    }
  }

  for (const t of lineFlattened) {
    if (showArchived ? t.archived : !t.archived) {
      filtered.push(t);
    }
  }

  return filtered.sort((a, b) => {
    if (a.pinned !== b.pinned) return a.pinned ? -1 : 1;
    return (b.date ?? 0) - (a.date ?? 0);
  });
};

const { lineThreads, legacyThreads } = generateData(3, 100, 200);

const iterations = 5000;

for (let i = 0; i < 50; i++) original(lineThreads, legacyThreads, 'COMBINED', false);
const startOrig = performance.now();
for (let i = 0; i < iterations; i++) original(lineThreads, legacyThreads, 'COMBINED', false);
const endOrig = performance.now();

for (let i = 0; i < 50; i++) optimized(lineThreads, legacyThreads, 'COMBINED', false);
const startOpt = performance.now();
for (let i = 0; i < iterations; i++) optimized(lineThreads, legacyThreads, 'COMBINED', false);
const endOpt = performance.now();

const origTime = endOrig - startOrig;
const optTime = endOpt - startOpt;

console.log(`Original: ${origTime.toFixed(2)}ms`);
console.log(`Optimized: ${optTime.toFixed(2)}ms`);
console.log(`Improvement: ${((origTime - optTime) / origTime * 100).toFixed(2)}%`);
