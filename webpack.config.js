module.exports = {
  entry: {
    './target/classes/js/built/bundle-server.js': './src/main/js/server/server.js',
    './target/classes/static/js/built/bundle-client.js': './src/main/js/client/index.tsx',
  },
  devtool: 'inline-source-map',
  output: {
    path: __dirname,
    filename: '[name]'
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: 'babel-loader'
      },
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules/,
      },
      {
        test: /\.css$/i,
        use: ['style-loader', 'css-loader'],
      },
    ]
  },
  resolve: {
    extensions: [ '.tsx', '.ts', '.js' ],
  },
  performance: {
    hints: false
  }
}