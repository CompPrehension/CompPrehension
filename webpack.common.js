module.exports = {
  entry: {
    './src/main/resources/static/js/bundle-server.js': './src/main/js/server/server.js',
    './src/main/resources/static/js/bundle-client.js': './src/main/js/client/index.tsx',
  },
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
  },  
}
