const winston = require('winston');

const retryLogger = winston.createLogger({
    format: winston.format.simple(),
    transports: [new winston.transports.File({filename: './retry.log'})]
});
const logger = winston.createLogger({
    format: winston.format.combine(
        winston.format.colorize(),
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
        logger.log({
            level: 'error',
            message: text
        });
    },
    info: (text) => {
        logger.log({
            level: 'info',
            message: text
        });
    }
};

