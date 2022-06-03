module.exports = {
    json: (text) => {
        console.log(Log.reset, JSON.stringify(text, null, 4))
    },
    log: (text) => {
        console.log(Log.reset, text)
    },
    error: (text) => {
        console.error(Log.fg.red, text)
    }
};

const Log = {
    reset: "\x1b[0m",
    bright: "\x1b[1m",
    dim: "\x1b[2m",
    underscore: "\x1b[4m",
    blink: "\x1b[5m",
    reverse: "\x1b[7m",
    hidden: "\x1b[8m",
    // Foreground (text) colors
    // `;1` bolds the text
    fg: {
        black: "\x1b[30;1m",
        red: "\x1b[31;1m",
        green: "\x1b[32;1m",
        yellow: "\x1b[33;1m",
        blue: "\x1b[34;1m",
        magenta: "\x1b[35;1m",
        cyan: "\x1b[36;1m",
        white: "\x1b[37;1m",
        crimson: "\x1b[38;1m"
    },
    // Background colors
    bg: {
        black: "\x1b[40m",
        red: "\x1b[41m",
        green: "\x1b[42m",
        yellow: "\x1b[43m",
        blue: "\x1b[44m",
        magenta: "\x1b[45m",
        cyan: "\x1b[46m",
        white: "\x1b[47m",
        crimson: "\x1b[48m"
    }
};
