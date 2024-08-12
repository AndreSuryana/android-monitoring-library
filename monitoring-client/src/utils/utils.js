const zlib = require('zlib')

/**
 * Decompresses a GZIP compressed string.
 *
 * @param {string} compressedData - The GZIP compressed data as a string.
 * @return {Promise<string>} - The decompressed string.
 */
function decompressLogs(compressedData) {
    return new Promise((resolve, reject) => {
        // Convert compressed data to Buffer
        const buffer = Buffer.from(compressedData, 'binary');

        // Decompress the buffer
        zlib.gunzip(buffer, (err, result) => {
            if (err) {
                return reject(err);
            }
            // Convert result to string
            resolve(result.toString('utf8'));
        });
    });
}

module.exports = { decompressLogs }