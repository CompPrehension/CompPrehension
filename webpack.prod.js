const { merge } = require('webpack-merge');
const common = require('./webpack.common.js');

module.exports = merge(common, {
  mode: 'production',
  entry: {
    './src/main/resources/static/js/bundle-client.js': './src/main/js/client/index.tsx',
  },
});
