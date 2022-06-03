const winston = require('winston');

winston.addColors({
    error: 'bold red'
});

const retryLogger = winston.createLogger({
    format: winston.format.simple(),
    transports: [new winston.transports.File({filename: './retry.log'})]
});

const consoleLogger = winston.createLogger({
    format: winston.format.combine(
        winston.format.colorize({all:true}),
        winston.format.simple()
    ),
    transports: [new winston.transports.Console()]
});

module.exports = {
    retry: (text) => {
        retryLogger.log({
            level: 'info',
            message: text
        });
    },
    error: (text) => {
        consoleLogger.log({
            level: 'error',
            message: text
        });
    },
    info: (text) => {
        consoleLogger.log({
            level: 'info',
            message: text
        });
    }
};
