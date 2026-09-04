import basicSsl from '@vitejs/plugin-basic-ssl';
import react from '@vitejs/plugin-react';
import fs from 'node:fs';
import http from 'node:http';
import https from 'node:https';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig, loadEnv, type Plugin, type PluginOption, type ProxyOptions } from 'vite';

const moduleRoot = fileURLToPath(new URL('.', import.meta.url));
const staticRoot = path.join(moduleRoot, 'src/main/resources/static');

/**
 * Path prefixes owned by the Spring backend. The dev server proxies them so that the
 * browser only ever talks to a single origin (https://localhost:4200): the JSESSIONID
 * cookie, the Keycloak login round-trip and the REST API then behave exactly as they
 * do in production, with no CORS and no cross-origin cookie games.
 */
const backendPaths = ['/api', '/oauth2', '/login', '/logout', '/lti', '/error'];

/** Dev-only prefix that reaches the backend with the prefix stripped - see springAuthGate. */
const passthroughPrefix = '/__spring';

function devCertificate() {
  const cert = path.join(moduleRoot, 'dev.crt');
  const key = path.join(moduleRoot, 'dev.key');
  return fs.existsSync(cert) && fs.existsSync(key)
    ? { cert: fs.readFileSync(cert), key: fs.readFileSync(key) }
    : undefined;
}

/** GET `url` with the browser's cookies attached; resolves to the response status. */
function probe(url: string, cookie: string | undefined): Promise<number> {
  const client = url.startsWith('https:') ? https : http;
  return new Promise((resolve, reject) => {
    const request = client.get(url, {
      headers: { accept: 'application/json', ...(cookie ? { cookie } : {}) },
      rejectUnauthorized: false, // the backend serves a self-signed dev certificate
      timeout: 5000,
    }, (response) => {
      response.resume();
      resolve(response.statusCode ?? 0);
    });
    request.on('timeout', () => request.destroy(new Error('timeout')));
    request.on('error', reject);
  });
}

/**
 * Reproduces the production login trigger.
 *
 * In production every `/pages/**` navigation goes to Spring, which answers an
 * unauthenticated browser with a redirect to Keycloak and remembers the requested url,
 * so the user comes back to the page they asked for. Under the dev server those routes
 * are served by Vite, so nothing ever triggers the login and the SPA just boots into a
 * wall of 401s.
 *
 * So: before serving index.html for a navigation, ask the backend whether this browser
 * has a session. If it has not, bounce the navigation through `/__spring/<path>`, which
 * the proxy forwards to Spring as `<path>` - from there the regular production flow
 * takes over and lands back on the dev server once the login is done.
 */
function springAuthGate(backendOrigin: string): Plugin {
  let backendReachable = true;
  return {
    name: 'compph:spring-auth-gate',
    apply: 'serve',
    configureServer(server) {
      // returned hook runs after Vite's own middlewares, but before index.html is served
      return () => server.middlewares.use((req, res, next) => {
        const url = req.originalUrl ?? req.url ?? '/';
        const isNavigation = req.method === 'GET' && (req.headers.accept?.includes('text/html') ?? false);
        if (!isNavigation || url.startsWith(passthroughPrefix)) {
          return next();
        }

        probe(`${backendOrigin}/api/users/whoami`, req.headers.cookie).then(
          (status) => {
            backendReachable = true;
            if (status !== 401) {
              return next();
            }
            res.statusCode = 302;
            res.setHeader('location', passthroughPrefix + url);
            res.end();
          },
          (error: NodeJS.ErrnoException) => {
            if (backendReachable) {
              backendReachable = false;
              server.config.logger.warn(
                `[spring-auth-gate] ${backendOrigin} is unreachable (${error.code ?? error.message}), serving the app unauthenticated`,
              );
            }
            next();
          },
        );
      });
    },
  };
}

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, moduleRoot);
  const mocking = mode === 'mock';
  const backendOrigin = env.VITE_BACKEND_ORIGIN || 'https://localhost:8433';
  const devPort = Number(env.VITE_DEV_PORT || 4200);

  // Keeping the original Host header makes Spring build every absolute url it emits
  // (the OAuth2 `redirect_uri` and the saved-request url it returns to after login)
  // against the dev server, so the whole Keycloak round-trip stays on
  // https://localhost:<devPort>. That requires https://localhost:<devPort>/* among the
  // Keycloak client's valid redirect uris; set VITE_PROXY_CHANGE_ORIGIN=true when it
  // cannot be added there.
  const changeOrigin = env.VITE_PROXY_CHANGE_ORIGIN === 'true';

  const proxy: Record<string, ProxyOptions> = Object.fromEntries([
    ...backendPaths.map((prefix) => [prefix, {
      target: backendOrigin,
      changeOrigin,
      secure: false, // the backend serves a self-signed dev certificate
    }]),
    [passthroughPrefix, {
      target: backendOrigin,
      changeOrigin,
      secure: false,
      rewrite: (p: string) => p.slice(passthroughPrefix.length) || '/',
    }],
  ]);

  const devHttps = mocking ? undefined : devCertificate();

  // and with no backend there is nothing to ask about the session either
  const plugins: PluginOption[] = mocking
    ? [react()]
    : [react(), springAuthGate(backendOrigin)];
  if (!mocking && !devHttps) {
    plugins.push(basicSsl());
  }

  return {
    root: moduleRoot,
    base: '/',
    resolve: {
      alias: [
        // fp-ts/lib is its commonjs build, which rollup cannot tree-shake: importing one
        // function from it drags the whole library in. es6 is the same code as modules.
        { find: /^fp-ts\/lib\/(.*)$/, replacement: 'fp-ts/es6/$1' },
      ],
    },
    publicDir: command === 'serve' ? 'public' : false,
    plugins,
    server: {
      host: 'localhost',
      port: devPort,
      strictPort: true,
      https: devHttps,
      open: true,
      proxy,
      fs: {
        // keep non-frontend parts out of reach of the dev server
        deny: ['**/*.{key,pfx,p12,jks}', '**/pom.xml', '**/target/**', '**/src/main/java/**', '**/src/main/resources/**'],
      },
    },
    build: {
      outDir: staticRoot,
      emptyOutDir: true,
      sourcemap: false,
      // the bundles are committed to the repository, so they are kept readable: a build
      // then shows up as a reviewable diff instead of one rewritten line
      minify: false,
      rollupOptions: {
        output: {
          // rollup shortens cross-chunk exports to single letters by default, which
          // reshuffles the whole import list on any change - keep the real names
          minifyInternalExports: false,
          manualChunks: (id) => (id.includes('node_modules') ? 'vendor' : undefined),
          entryFileNames: 'js/[name]-[hash].js',
          chunkFileNames: 'js/[name]-[hash].js',
          assetFileNames: (asset) => {
            const name = asset.names?.[0] ?? '';
            return name.endsWith('.css')
              ? 'css/[name]-[hash][extname]'
              : 'assets/[name]-[hash][extname]';
          },
        },
      },
    },
  };
});
