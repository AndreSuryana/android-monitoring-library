const amqp = require('amqplib');
const yargs = require('yargs');
const { hideBin } = require('yargs/helpers');
const { decompressLogs } = require('./utils/utils');

// Default parameters
const DEFAULT_EXCHANGE_NAME = 'monitoring';

// Parse command-line arguments
const argv = yargs(hideBin(process.argv))
    .option('host', {
        alias: 'h',
        type: 'string',
        description: 'MQTT host',
        default: '172.30.16.1'
    })
    .option('port', {
        alias: 'p',
        type: 'number',
        description: 'MQTT port',
        default: 5672
    })
    .option('username', {
        alias: 'u',
        type: 'string',
        description: 'MQTT username',
        default: 'guest'
    })
    .option('password', {
        alias: 'pw',
        type: 'string',
        description: 'MQTT password',
        default: 'guest'
    })
    .option('exchange', {
        alias: 'ex',
        type: 'string',
        description: 'MQTT exchange name',
        default: DEFAULT_EXCHANGE_NAME,
    })
    .option('deviceId', {
        alias: 'd',
        type: 'string',
        description: 'Device ID',
        demandOption: true // Make deviceId a required argument
    })
    .argv;

// Define queue nae for device logs
const QUEUE_DEVICE_LOGS = 'device_logs_' + process.pid;

async function setupConnection() {
    try {
        const connection = await amqp.connect({
            protocol: 'amqp',
            hostname: argv.host,
            port: argv.port,
            username: argv.username,
            password: argv.password,
        });
        connection.on('error', (err) => {
            console.error('Connection error:', err);
        });

        const channel = await connection.createChannel();
        channel.on('error', (err) => {
            console.error('Channel error:', err);
        });

        return { connection, channel };
    } catch (error) {
        console.error('Error establishing connection:', error);
        throw error;
    }
}

async function consumeLogs() {
    const { connection, channel } = await setupConnection();
    try {
        await channel.assertExchange(argv.exchange, 'topic', { durable: true });
        await channel.assertQueue(QUEUE_DEVICE_LOGS, { durable: true });
        await channel.bindQueue(QUEUE_DEVICE_LOGS, argv.exchange, `log.${argv.deviceId}`);

        console.log('Waiting for log messages. To exit press CTRL+C');
        channel.consume(QUEUE_DEVICE_LOGS, (msg) => {
            if (msg !== null) {
                // console.log(msg.content.toString());
                decompressLogs(msg.content.toString())
                    .then((log => console.log(log.replace(/\n{2,}/g, '\n').trim())))
                    .catch(err => console.error('Error:', err));
                channel.ack(msg);
            }
        }, { noAck: false });
        
        process.on('SIGINT', async () => {
            await clearQueue(channel);
            await connection.close();
            process.exit(0);
        });

    } catch (error) {
        console.error('Error:', error);
    }
}

async function clearQueue(channel) {
    try {
        await channel.deleteQueue(QUEUE_DEVICE_LOGS);
        console.log('Queue deleted:', QUEUE_DEVICE_LOGS);
    } catch (error) {
        console.error('Error deleting queue:', error);
    }
}

consumeLogs();