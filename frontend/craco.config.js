const CracoAlias = require("craco-alias");

module.exports = {
  devServer: (devServerConfig) => {
    devServerConfig.port = 80;

    const onBeforeSetupMiddleware = devServerConfig.onBeforeSetupMiddleware;
    const onAfterSetupMiddleware = devServerConfig.onAfterSetupMiddleware;

    if (onBeforeSetupMiddleware || onAfterSetupMiddleware) {
      devServerConfig.setupMiddlewares = (middlewares, devServer) => {
        if (typeof onBeforeSetupMiddleware === "function") {
          onBeforeSetupMiddleware(devServer);
        }

        if (typeof onAfterSetupMiddleware === "function") {
          onAfterSetupMiddleware(devServer);
        }

        return middlewares;
      };

      delete devServerConfig.onBeforeSetupMiddleware;
      delete devServerConfig.onAfterSetupMiddleware;
    }

    return devServerConfig;
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
