/**
 * MIT License
 * Copyright (c) 2017 Helloween
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.chip.emulator;

import com.github.chip.emulator.core.Disassembler;
import com.github.chip.emulator.core.ProgramExecutor;
import com.github.chip.emulator.core.ProgramLoader;
import com.github.chip.emulator.core.events.*;
import com.github.chip.emulator.core.exceptions.UnsupportedOpcodeException;
import com.github.chip.emulator.core.opcodes.Opcode;
import com.github.chip.emulator.core.services.AsyncEventService;
import com.github.chip.emulator.core.services.EventService;
import com.github.chip.emulator.events.ChangeColorEvent;
import com.github.chip.emulator.events.ChangeScaleEvent;
import com.github.chip.emulator.events.ResetEvent;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author helloween
 */
public class Chip8Emulator extends Application {
    private static final int DEFAULT_WIDTH  = 64;
    private static final int DEFAULT_HEIGHT = 32;
    private static final int SCALE          = 10;
    private static final int DEFAULT_DELAY  = 1;

    private double            scale               = SCALE;
    private Color             pixelColor          = Color.valueOf("0x202a35");
    private Color             backGroundColor     = Color.valueOf("0x8f9185");
    private int               delay               = DEFAULT_DELAY;
    private AtomicInteger     width               = new AtomicInteger(DEFAULT_WIDTH);
    private AtomicInteger     height              = new AtomicInteger(DEFAULT_HEIGHT);
    private ExecutorService   executorService;
    private Future<?>         emulatorFuture;
    private Canvas            canvas;
    private DebugWindow       debugWindow;
    private Stage             stage;
    private ByteBuffer        programBuffer;
    private ArrayList<String> programListing;
    private volatile boolean  paused;

    private CanvasRedrawTask<RefreshScreenEvent> task;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException, UnsupportedOpcodeException {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("emulator-worker-%d")
                                                                      .setDaemon(true)
                                                                      .build();
        executorService = Executors.newFixedThreadPool(1, threadFactory);
        ProgramLoader loader = new ProgramLoader();
        programBuffer = loader.load("GAME");

        BorderPane rootLayout = new BorderPane();

        programListing = new ArrayList<>();
        java.util.List<Opcode> program = new Disassembler(programBuffer).disassemble();
        for (Opcode opcode : program) {
            programListing.add(String.format("#%04X - %s %s", opcode.getRawOpcode(), opcode.getInstruction().name(), Arrays.toString(opcode.getArguments().toArray())));
        }

        debugWindow = new DebugWindow(programListing);
        rootLayout.setBottom(debugWindow);
        EmulatorMenuBar menuBar = new EmulatorMenuBar();
        rootLayout.setTop(menuBar);
        this.canvas = createCanvas();
        rootLayout.setCenter(this.canvas);

        Scene scene = new Scene(rootLayout);
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();
        stage = primaryStage;

        scene.setOnKeyPressed(new KeyEventHandler());

        AsyncEventService.getInstance().registerHandler(this);
        EventService.getInstance().registerHandler(this);
        emulatorFuture = executorService.submit(new ProgramExecutor(programBuffer, delay));
        primaryStage.setOnCloseRequest((event) -> {
            AsyncEventService.getInstance().postEvent(new PauseEvent(false));
            AsyncEventService.getInstance().postEvent(StopEvent.INSTANCE);
        });
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void draw(RefreshScreenEvent event) {
        task.requestRedraw(event);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleScaleEvent(ChangeScaleEvent event) {
        this.scale = event.getScale();
        canvas.setWidth(this.width.get() * scale);
        canvas.setHeight(this.height.get() * scale);
        stage.sizeToScene();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleChangeColorEvent(ChangeColorEvent event) {
        if (event.getType() == ChangeColorEvent.Type.PIXEL)
            this.pixelColor = event.getColor();
        else
            this.backGroundColor = event.getColor();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleChangeDelayEvent(SetDelayEvent event) {
        this.delay = event.getDelay();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleResetEvent(ResetEvent event) {
        try {
            boolean pauseFlag = this.paused;
            AsyncEventService.getInstance().postEvent(new PauseEvent(false));
            AsyncEventService.getInstance().postEvent(StopEvent.INSTANCE);
            emulatorFuture.get();
            this.width.getAndSet(DEFAULT_WIDTH);
            this.height.getAndSet(DEFAULT_HEIGHT);
            this.canvas             = createCanvas();
            this.debugWindow        = new DebugWindow(programListing);
            BorderPane rootLayout   = (BorderPane)this.stage.getScene().getRoot();
            rootLayout.setCenter(this.canvas);
            rootLayout.setBottom(this.debugWindow);
            emulatorFuture = executorService.submit(new ProgramExecutor(programBuffer, delay, pauseFlag));
            if (pauseFlag)
                AsyncEventService.getInstance().postEvent(new PauseEvent(true)); // for menu
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleSoundEvent(PlaySoundEvent event) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        toolkit.beep();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleExtendedScreenMode(EnableExtendedScreenModeEvent event) {
        if (this.width.get() != (DEFAULT_WIDTH << 1)) {
            this.width.getAndAdd(this.width.get());
            this.height.getAndAdd(this.height.get());
            Platform.runLater(() -> {
                canvas.setWidth(this.width.get() * scale);
                canvas.setHeight(this.height.get() * scale);
                stage.sizeToScene();
            });
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handlePauseEvent(PauseEvent event) {
        this.paused = event.isPauseFlag();
    }

    private Canvas createCanvas() {
        Canvas canvas = new Canvas();
        canvas.setWidth(this.width.get() * scale);
        canvas.setHeight(this.height.get() * scale);
        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
        graphicsContext.setFill(backGroundColor);
        graphicsContext.fillRect(0, 0, this.width.get() * scale, this.height.get() * scale);
        task = new CanvasRedrawTask<RefreshScreenEvent>(canvas) {
            @Override
            protected void redraw(GraphicsContext context, RefreshScreenEvent event) {
                boolean[][] data = event.getScreen();
                for (int i = 0; i < data.length; ++i) {
                    for (int j = 0; j < data[i].length; ++j) {
                        Color color = data[i][j] ? pixelColor : backGroundColor;
                        context.setFill(color);
                        context.fillRect(i * scale, j * scale, scale, scale);
                    }
                }
            }
        };
        return canvas;
    }

    public abstract class CanvasRedrawTask<T> extends AnimationTimer {
        private final AtomicReference<T> data = new AtomicReference<>(null);
        private final Canvas canvas;

        public CanvasRedrawTask(Canvas canvas) {
            this.canvas = canvas;
        }

        public void requestRedraw(T dataToDraw) {
            data.set(dataToDraw);
            start();
        }

        @Override
        public void handle(long now) {
            T dataToDraw = data.getAndSet(null);
            if (dataToDraw != null) {
                redraw(canvas.getGraphicsContext2D(), dataToDraw);
            }
        }

        protected abstract void redraw(GraphicsContext context, T data);
    }
}
