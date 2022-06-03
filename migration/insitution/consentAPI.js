const axios = require('axios');
const log = require('./logging')

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
        axios.post(url, data, config).then(response => {
            log.log(response.status + ": " + name)
        }).catch(err => {
            log.error(err.message + ": " + name)
        })
    }
}