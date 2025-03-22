const { merge } = require('webpack-merge');
const common = require('./webpack.common.js');
const path = require('path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');

module.exports = merge(common, {  
  plugins: [
    new CleanWebpackPlugin({
      cleanOnceBeforeBuildPatterns: [
        'js/*',
        'css/*',
      ],
    }),
    new HtmlWebpackPlugin({
      template: __dirname + '/src/main/js/templates/index.html',
      filename: 'index.html',
      inject: 'body',
      publicPath: '/',
      minify: false,
    }),
    new MiniCssExtractPlugin({
      filename: 'css/[name].[contenthash].css',
    }),      
  ],
  mode: 'development',
  devtool: 'inline-source-map',
  output: {
    ...common.output,
    path: path.join(__dirname, './target/classes/static/'),
  },
  optimization: {
    ...common.optimization,
    minimize: false,
  },
  devServer: {
    /*static: {
      directory: path.join(__dirname, 'public'),
      /*directory: path.join(__dirname, './target/classes/static/'),
    },*/
    historyApiFallback: true,
    /*historyApiFallback: {
      index: 'index.html'
    },
    */
    /*
    devMiddleware: {
      index: true,
      mimeTypes: { phtml: 'text/html' },
      publicPath: path.join(__dirname, './target/classes/static/'),
      serverSideRender: true,
      writeToDisk: true,
    },
    
    */    
    allowedHosts: 'all',
    proxy: [
      {
        context: ['/api'],
        target: 'https://localhost:8433'
      },
    ],
    compress: true,
    port: 4200,
    hot: true,
    open: true,
  },
});
