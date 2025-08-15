/*  ByteObf: A Java Bytecode Obfuscator
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

package codes.rayacode.ByteObf.ui;

import codes.rayacode.ByteObf.obfuscator.ByteObf;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class ObfuscationService extends Service<Void> {

    private final ByteObfConfig config;
    private final ListView<String> console;
    private final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    private volatile boolean runningLogProcessor = false;
    private Thread logProcessorThread;

    
    private static final int UI_LOG_BATCH_SIZE = 200; 
    private static final long UI_LOG_BATCH_DELAY_MS = 100; 

    public ObfuscationService(ByteObfConfig config, ListView<String> console) {
        this.config = config;
        this.console = console;
    }

    @Override
    protected Task<Void> createTask() {
        startLogProcessor();

        Consumer<String> logConsumer = msg -> {
            try {
                logQueue.put(msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        Consumer<String> errConsumer = msg -> {
            try {
                logQueue.put(msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        return new ByteObf(config, logConsumer, errConsumer);
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        stopLogProcessor();
        processRemainingLogs();
    }

    @Override
    protected void failed() {
        super.failed();
        stopLogProcessor();
        processRemainingLogs();
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        stopLogProcessor();
        processRemainingLogs();
    }

    private void startLogProcessor() {
        if (runningLogProcessor) return;

        runningLogProcessor = true;
        logProcessorThread = new Thread(() -> {
            List<String> batch = new ArrayList<>();
            long lastFlushTime = System.currentTimeMillis();

            while (runningLogProcessor || !logQueue.isEmpty()) {
                try {
                    String msg = logQueue.poll(); 
                    if (msg != null) {
                        batch.add(msg);
                    }

                    long currentTime = System.currentTimeMillis();
                    boolean timeToFlush = (currentTime - lastFlushTime >= UI_LOG_BATCH_DELAY_MS);
                    boolean batchFull = (batch.size() >= UI_LOG_BATCH_SIZE);
                    boolean serviceStoppingAndQueueEmpty = !runningLogProcessor && logQueue.isEmpty();

                    if (!batch.isEmpty() && (batchFull || timeToFlush || serviceStoppingAndQueueEmpty)) {
                        final List<String> finalBatch = new ArrayList<>(batch);
                        Platform.runLater(() -> {
                            console.getItems().addAll(finalBatch);
                            console.scrollTo(console.getItems().size() - 1);
                        });
                        batch.clear();
                        lastFlushTime = currentTime;
                    }

                    
                    if (msg == null && !batchFull && !timeToFlush && runningLogProcessor) {
                        Thread.sleep(10); 
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    runningLogProcessor = false;
                }
            }
        }, "UILogProcessor-Thread");
        logProcessorThread.setDaemon(true);
        logProcessorThread.start();
    }

    private void stopLogProcessor() {
        runningLogProcessor = false;
        if (logProcessorThread != null) {
            logProcessorThread.interrupt();
            try {
                logProcessorThread.join(2000); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processRemainingLogs() {
        if (!logQueue.isEmpty()) {
            List<String> remainingLogs = new ArrayList<>();
            logQueue.drainTo(remainingLogs);
            if (!remainingLogs.isEmpty()) {
                Platform.runLater(() -> {
                    console.getItems().addAll(remainingLogs);
                    console.scrollTo(console.getItems().size() - 1);
                });
            }
        }
    }
}