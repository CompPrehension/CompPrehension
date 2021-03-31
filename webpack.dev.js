const { merge } = require('webpack-merge');
const common = require('./webpack.common.js');

module.exports = merge(common, {  
  mode: 'development',
  entry: {
    './target/classes/static/js/bundle-server.js': './src/main/js/server/server.js',
    './target/classes/static/js/bundle-client.js': './src/main/js/client/index.tsx',
  },
  devtool: 'inline-source-map',
  optimization: {
    minimize: false,
  },
});
