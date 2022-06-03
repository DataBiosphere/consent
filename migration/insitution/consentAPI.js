const axios = require('axios');
const log = require('./logging');

module.exports = {

    postInstitution: async (host, token, name) => {
        const url = host + '/api/institutions'
        const config = {
            headers: {
                Authorization: `Bearer ${token}`,
                Accept: 'application/json'
            }
        }
        const data = {name: name}
        return axios.post(url, data, config).then(response => {
            log.log(response.status + ": " + name);
            return response.status;
        }).catch(err => {
            log.error(err.message + ": " + name);
            return err.response.status;
        });
    }
};
