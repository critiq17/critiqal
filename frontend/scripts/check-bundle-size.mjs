import { readdirSync, statSync, existsSync } from 'fs';
import { join } from 'path';

const OUTPUT_DIR = '.svelte-kit/output/client/_app/immutable';
const CHUNKS_DIR = join(OUTPUT_DIR, 'chunks');
const NODES_DIR = join(OUTPUT_DIR, 'nodes');

const CHUNK_LIMIT = 50 * 1024;   // 50 kB
const NODE_LIMIT  = 30 * 1024;   // 30 kB
const TOTAL_LIMIT = 520 * 1024;  // 520 kB

const KB = 1024;

function fmt(bytes) {
  return (bytes / KB).toFixed(1) + ' kB';
}

function checkDir(dir, limit, label) {
  if (!existsSync(dir)) {
    console.error(`ERROR: Directory not found: ${dir}`);
    console.error('Run `npm run build` before executing this script.');
    process.exit(1);
  }

  const files = readdirSync(dir).filter(f => f.endsWith('.js'));
  let passed = true;

  console.log(`\n${label} (max ${fmt(limit)}):`);

  for (const file of files) {
    const path = join(dir, file);
    const size = statSync(path).size;
    const ok = size <= limit;
    const icon = ok ? '✓' : '✗';
    const line = `  ${icon} ${file.padEnd(30)} ${fmt(size).padStart(9)} / ${fmt(limit)}`;
    console.log(line);
    if (!ok) {
      passed = false;
    }
  }

  return { files, passed };
}

if (!existsSync(OUTPUT_DIR)) {
  console.error(`ERROR: Build output not found at: ${OUTPUT_DIR}`);
  console.error('Run `npm run build` before executing this script.');
  process.exit(1);
}

let allPassed = true;

const { files: chunkFiles, passed: chunksPassed } = checkDir(CHUNKS_DIR, CHUNK_LIMIT, 'Chunk budget check');
const { files: nodeFiles,  passed: nodesPassed  } = checkDir(NODES_DIR,  NODE_LIMIT,  'Node budget check');

if (!chunksPassed || !nodesPassed) {
  allPassed = false;
}

// Total JS across both directories
const allDirs = [CHUNKS_DIR, NODES_DIR];
let totalBytes = 0;
for (const dir of allDirs) {
  if (existsSync(dir)) {
    for (const file of readdirSync(dir).filter(f => f.endsWith('.js'))) {
      totalBytes += statSync(join(dir, file)).size;
    }
  }
}

const totalOk = totalBytes <= TOTAL_LIMIT;
const totalIcon = totalOk ? '✓' : '✗';
console.log(`\nTotal JS: ${fmt(totalBytes)} / ${fmt(TOTAL_LIMIT)} ${totalIcon}`);

if (!totalOk) {
  allPassed = false;
}

if (allPassed) {
  console.log('\nAll budgets passed.');
  process.exit(0);
} else {
  console.error('\nBudget exceeded — build rejected.');
  process.exit(1);
}
