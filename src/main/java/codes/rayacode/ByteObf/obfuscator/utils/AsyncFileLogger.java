/*  ByteObf: A Java Bytecode Obfuscator
 *  Copyright (C) 2021 vimasig
 *  Copyright (C) 2025 Mohammad Ali Solhjoo mohammadalisolhjoo@live.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package codes.rayacode.ByteObf.obfuscator.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AsyncFileLogger {

    private static final String LOG_FILE_PATH = "log.txt";
    private static final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    private static volatile boolean running = false;
    private static Thread logWriterThread;

    public static void start() {
        if (running) {
            return;
        }
        running = true;
        logWriterThread = new Thread(() -> {
            try (FileWriter fw = new FileWriter(LOG_FILE_PATH, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                while (running || !logQueue.isEmpty()) {
                    try {
                        String message = logQueue.poll(100, TimeUnit.MILLISECONDS); 
                        if (message != null) {
                            pw.println(message);
                            
                            if (logQueue.isEmpty()) {
                                pw.flush();
                            }
                        } else if (!running) {
                            
                            break;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        running = false; 
                    }
                }
            } catch (IOException e) {
                System.err.println("Error writing to log file: " + e.getMessage());
                e.printStackTrace();
            } finally {
                running = false;
            }
        }, "AsyncFileLogger-Thread");
        logWriterThread.setDaemon(true); 
        logWriterThread.start();
    }

    public static void log(String message) {
        if (!running) {
            start(); 
        }
        try {
            logQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void logStackTrace(Throwable t) {
        if (!running) {
            start(); 
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        log(sw.toString());
    }

    public static void stop() {
        running = false;
        if (logWriterThread != null) {
            logWriterThread.interrupt();
            try {
                logWriterThread.join(5000); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}