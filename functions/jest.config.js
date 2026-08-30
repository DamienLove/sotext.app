module.exports = {
  preset: "ts-jest",
  testEnvironment: "node",
  // ts-jest defaults to tsconfig.json, which is the production build config: it excludes
  // *.test.ts and narrows "types" to ["node"] (no jest globals), which breaks every test file's
  // type-checking (`expect`/`it` "Cannot find name"). Point it at tsconfig.dev.json instead,
  // which includes jest's ambient types and doesn't exclude test files.
  transform: {
    "^.+\\.tsx?$": ["ts-jest", {tsconfig: "tsconfig.dev.json"}],
  },
  testMatch: ["**/__tests__/**/*.ts", "**/?(*.)+(spec|test).ts"],
  collectCoverageFrom: [
    "src/**/*.ts",
    "!src/**/*.d.ts",
    "!src/index.ts",
  ],
  coverageThreshold: {
    global: {
      branches: 80,
      functions: 80,
      lines: 80,
      statements: 80,
    },
  },
};
