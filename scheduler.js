#!/usr/bin/env node

/**
 * Node.js Cron Scheduler for SMS Share Automation
 * Runs tests daily at 4:00 PM
 */

// Load environment variables from .env file
require('dotenv').config();

const cron = require('node-cron');
const { exec, spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

// Configuration
const PROJECT_DIR = __dirname;
const LOG_DIR = path.join(PROJECT_DIR, 'logs');

// Build cron schedule from hour, minute, second or use CRON_SCHEDULE if provided
let CRON_SCHEDULE;
if (process.env.CRON_SCHEDULE) {
    CRON_SCHEDULE = process.env.CRON_SCHEDULE;
} else {
    // Build from individual time components (format: second minute hour day month weekday)
    const hour = parseInt(process.env.SCHEDULE_HOUR || '16', 10);
    const minute = parseInt(process.env.SCHEDULE_MINUTE || '0', 10);
    const second = parseInt(process.env.SCHEDULE_SECOND || '0', 10);
    CRON_SCHEDULE = `${second} ${minute} ${hour} * * *`; // Daily at specified time
}

const SLACK_WEBHOOK_URL = process.env.SLACK_WEBHOOK_URL || '';
const SLACK_CHANNEL = process.env.SLACK_CHANNEL || '';
const SLACK_USERNAME = process.env.SLACK_USERNAME || 'SMS Share Test Bot';

// Ensure logs directory exists
if (!fs.existsSync(LOG_DIR)) {
    fs.mkdirSync(LOG_DIR, { recursive: true });
}

/**
 * Log message with timestamp
 */
function log(message) {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] ${message}`);
}

/**
 * Execute shell command and return promise (buffered output)
 */
function execCommand(command, options = {}) {
    return new Promise((resolve, reject) => {
        // Ensure Java and Maven are in PATH (common locations)
        const env = {
            ...process.env,
            PATH: [
                '/opt/homebrew/bin',
                '/opt/homebrew/opt/openjdk@11/bin',
                '/usr/local/bin',
                process.env.PATH || ''
            ].filter(Boolean).join(':')
        };
        
        exec(command, { 
            cwd: PROJECT_DIR,
            env,
            ...options 
        }, (error, stdout, stderr) => {
            if (error) {
                reject({ error, stdout, stderr });
            } else {
                resolve({ stdout, stderr });
            }
        });
    });
}

/**
 * Execute shell command with streaming output (for long-running processes)
 */
function execCommandStreaming(command, options = {}) {
    return new Promise((resolve, reject) => {
        // Ensure Java and Maven are in PATH (common locations)
        const env = {
            ...process.env,
            PATH: [
                '/opt/homebrew/bin',
                '/opt/homebrew/opt/openjdk@11/bin',
                '/usr/local/bin',
                process.env.PATH || ''
            ].filter(Boolean).join(':')
        };
        
        // Use shell to execute the command (handles complex commands with quotes properly)
        const child = spawn(command, [], {
            cwd: PROJECT_DIR,
            env,
            shell: true,
            ...options
        });
        
        let stdout = '';
        let stderr = '';
        
        // Stream stdout
        child.stdout.on('data', (data) => {
            const text = data.toString();
            stdout += text;
            // Log important lines in real-time
            const lines = text.split('\n').filter(line => line.trim());
            lines.forEach(line => {
                if (line.includes('TEST RESULT:') || 
                    line.includes('PASS') || 
                    line.includes('FAIL') || 
                    line.includes('CAPTCHA_ERROR') ||
                    line.includes('Success Rate:') ||
                    line.includes('All tests') ||
                    line.includes('Testing URL:')) {
                    log(`  → ${line.trim()}`);
                }
            });
        });
        
        // Stream stderr
        child.stderr.on('data', (data) => {
            const text = data.toString();
            stderr += text;
            // Log errors in real-time (but filter out common warnings)
            if (text.trim() && !text.includes('WARNING') && !text.includes('deprecated')) {
                log(`  ⚠ ${text.trim()}`);
            }
        });
        
        child.on('close', (code) => {
            if (code !== 0) {
                reject({ 
                    error: new Error(`Process exited with code ${code}`), 
                    stdout, 
                    stderr,
                    code
                });
            } else {
                resolve({ stdout, stderr, code });
            }
        });
        
        child.on('error', (error) => {
            reject({ error, stdout, stderr });
        });
    });
}

/**
 * Run the SMS Share tests
 */
async function runTests() {
    const startTime = new Date();
    const logFileName = `test_${startTime.toISOString().replace(/[:.]/g, '-').split('T')[0]}_${startTime.toTimeString().split(' ')[0].replace(/:/g, '')}.log`;
    const logFilePath = path.join(LOG_DIR, logFileName);
    const summaryLogPath = path.join(LOG_DIR, 'daily_summary.log');
    
    log('==========================================');
    log('Starting Scheduled Test Run');
    log('==========================================');
    log(`Log file: ${logFilePath}`);
    log('');
    
    let status = 'FAILED';
    let exitCode = 1;
    
    try {
        // Step 1: Compile project
        log('Step 1: Compiling project...');
        await execCommand('mvn clean compile test-compile -q');
        log('✓ Compilation successful');
        
        // Step 2: Build classpath
        log('Step 2: Building classpath...');
        await execCommand('mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q');
        log('✓ Classpath built');
        
        // Step 3: Read classpath
        const cpFilePath = path.join(PROJECT_DIR, 'cp.txt');
        if (!fs.existsSync(cpFilePath)) {
            throw new Error('Classpath file (cp.txt) not found');
        }
        const classpath = fs.readFileSync(cpFilePath, 'utf8').trim();
        const fullClasspath = `${classpath}:target/classes:target/test-classes`;
        
        // Step 4: Build Java command
        let javaCmd = `java -Dheadless=true -Dslack.webhook.url="${SLACK_WEBHOOK_URL}"`;
        
        if (SLACK_CHANNEL) {
            javaCmd += ` -Dslack.channel="${SLACK_CHANNEL}"`;
        }
        
        if (SLACK_USERNAME) {
            javaCmd += ` -Dslack.username="${SLACK_USERNAME}"`;
        }
        
        javaCmd += ` -cp "${fullClasspath}" com.selenium.test.SMSShareTest`;
        
        // Step 5: Execute tests (with streaming output)
        log('Step 3: Running tests...');
        log('(Tests may take 10-20 minutes to complete all URLs)');
        log('');
        
        const { stdout, stderr } = await execCommandStreaming(javaCmd);
        
        // Write output to log file
        const logContent = [
            `==========================================`,
            `Scheduled Test Run - ${startTime.toISOString()}`,
            `==========================================`,
            ``,
            stdout,
            stderr ? `\n--- STDERR ---\n${stderr}` : '',
            `==========================================`,
            `Test execution completed at ${new Date().toISOString()}`,
            `==========================================`
        ].join('\n');
        
        fs.writeFileSync(logFilePath, logContent);
        
        // Check if tests passed (look for success indicators in output)
        if (stdout.includes('All tests PASSED') || stdout.includes('Success Rate:      100.0')) {
            status = 'SUCCESS';
            exitCode = 0;
        }
        
        log('✓ Test execution completed');
        log(`Status: ${status}`);
        
    } catch (err) {
        const errorMsg = err.error ? err.error.message : err.message;
        const errorOutput = err.stdout || '';
        const errorStderr = err.stderr || '';
        
        log(`✗ Test execution failed: ${errorMsg}`);
        
        // Write error to log file
        const logContent = [
            `==========================================`,
            `Scheduled Test Run - ${startTime.toISOString()}`,
            `==========================================`,
            ``,
            `ERROR: ${errorMsg}`,
            ``,
            `--- STDOUT ---`,
            errorOutput,
            ``,
            `--- STDERR ---`,
            errorStderr,
            `==========================================`,
            `Test execution failed at ${new Date().toISOString()}`,
            `==========================================`
        ].join('\n');
        
        fs.writeFileSync(logFilePath, logContent);
        
        status = 'FAILED';
        exitCode = err.error ? err.error.code || 1 : 1;
    }
    
    // Append summary to daily summary log
    const summaryEntry = `[${new Date().toISOString()}] Status: ${status} | Exit Code: ${exitCode} | Log: ${logFilePath}\n`;
    fs.appendFileSync(summaryLogPath, summaryEntry);
    
    // Clean up old logs (keep last 30 days)
    try {
        const files = fs.readdirSync(LOG_DIR);
        const now = Date.now();
        const thirtyDaysMs = 30 * 24 * 60 * 60 * 1000;
        
        files.forEach(file => {
            if (file.startsWith('test_') && file.endsWith('.log')) {
                const filePath = path.join(LOG_DIR, file);
                const stats = fs.statSync(filePath);
                if (now - stats.mtimeMs > thirtyDaysMs) {
                    fs.unlinkSync(filePath);
                    log(`Deleted old log: ${file}`);
                }
            }
        });
    } catch (cleanupErr) {
        log(`Warning: Failed to clean up old logs: ${cleanupErr.message}`);
    }
    
    log('');
    log('==========================================');
    log(`Scheduled run completed: ${status}`);
    log('==========================================');
    log('');
    
    return { status, exitCode, logFilePath };
}

// Get schedule time for display
const scheduleHour = parseInt(process.env.SCHEDULE_HOUR || '16', 10);
const scheduleMinute = parseInt(process.env.SCHEDULE_MINUTE || '0', 10);
const scheduleSecond = parseInt(process.env.SCHEDULE_SECOND || '0', 10);
const scheduleTime24 = `${scheduleHour.toString().padStart(2, '0')}:${scheduleMinute.toString().padStart(2, '0')}:${scheduleSecond.toString().padStart(2, '0')}`;
const scheduleHour12 = scheduleHour >= 12 ? scheduleHour - 12 : scheduleHour;
const scheduleHour12Display = scheduleHour12 === 0 ? 12 : scheduleHour12;
const scheduleTime12 = `${scheduleHour12Display}:${scheduleMinute.toString().padStart(2, '0')}:${scheduleSecond.toString().padStart(2, '0')} ${scheduleHour >= 12 ? 'PM' : 'AM'}`;

// Schedule the cron job
log('==========================================');
log('SMS Share Automation Scheduler');
log('==========================================');
log(`Schedule: Daily at ${scheduleTime24} (${scheduleTime12})`);
log(`Cron: ${CRON_SCHEDULE}`);
log(`Slack Webhook: ${SLACK_WEBHOOK_URL.substring(0, 50)}...`);
log(`Log Directory: ${LOG_DIR}`);
log('');
log('Scheduler started. Waiting for scheduled time...');
log('Press Ctrl+C to stop.');
log('');

// Schedule the job
const scheduledTask = cron.schedule(CRON_SCHEDULE, async () => {
    log('⏰ Scheduled time reached. Starting test run...');
    await runTests();
}, {
    scheduled: true,
    timezone: process.env.TZ || Intl.DateTimeFormat().resolvedOptions().timeZone
});

// Handle graceful shutdown
process.on('SIGINT', () => {
    log('');
    log('Received SIGINT. Stopping scheduler...');
    scheduledTask.stop();
    log('Scheduler stopped.');
    process.exit(0);
});

process.on('SIGTERM', () => {
    log('');
    log('Received SIGTERM. Stopping scheduler...');
    scheduledTask.stop();
    log('Scheduler stopped.');
    process.exit(0);
});

// Keep the process running
process.stdin.resume();
