const CracoAlias = require("craco-alias");

module.exports = {
  devServer: {
    port: 80,
  },
  plugins: [
    {
      plugin: CracoAlias,
      options: {
        source: "options",
        baseUrl: "./",
        aliases: {
          "@apis": "./src/apis",
          "@assets": "./src/assets",
          "@components": "./src/components",
          "@config": "./src/config",
          "@data": "./src/data",
          "@hooks": "./src/hooks",
          "@layouts": "./src/layouts",
          "@pages": "./src/pages",
          "@utils": "./src/utils",
        },
      },
    },
  ],
};
