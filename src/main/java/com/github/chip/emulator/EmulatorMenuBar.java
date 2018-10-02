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

import com.github.chip.emulator.core.events.PauseEvent;
import com.github.chip.emulator.core.events.SetDelayEvent;
import com.github.chip.emulator.core.services.AsyncEventService;
import com.github.chip.emulator.core.services.EventService;
import com.github.chip.emulator.events.ChangeColorEvent;
import com.github.chip.emulator.events.ChangeScaleEvent;
import com.github.chip.emulator.events.ResetEvent;
import com.google.common.eventbus.Subscribe;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.github.chip.emulator.events.ChangeColorEvent.Type.BACKGROUND;
import static com.github.chip.emulator.events.ChangeColorEvent.Type.PIXEL;

/**
 * @author helloween
 */
public class EmulatorMenuBar extends MenuBar {

    private final CheckMenuItem pauseItem;

    public EmulatorMenuBar() {
        Menu emulator = new Menu("Emulator");
        Menu delay = new Menu("Delay (ms)");
        for (int i = 1; i <= 64; i <<= 1) {
            MenuItem msDelay = new MenuItem(Integer.toString(i));
            msDelay.setOnAction((event) ->
                    AsyncEventService.getInstance().postEvent(new SetDelayEvent(Integer.parseInt(((MenuItem) (event.getSource())).getText()))));
            delay.getItems().add(msDelay);
        }
        pauseItem  = new CheckMenuItem("Pause");
        pauseItem.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN));
        pauseItem.selectedProperty().addListener((ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) -> {
            AsyncEventService.getInstance().postEvent(new PauseEvent(newValue));
        });
        MenuItem resetItem  = new MenuItem("Reset");
        resetItem.setOnAction((event) -> {
            EventService.getInstance().postEvent(ResetEvent.INSTANCE);
        });
        emulator.getItems().add(delay);
        emulator.getItems().add(pauseItem);
        emulator.getItems().add(resetItem);

        Menu video   = new Menu("Video");
        Menu scale   = new Menu("Scale");
        MenuItem x5  = new MenuItem("x5");
        MenuItem x10 = new MenuItem("x10");
        MenuItem x20 = new MenuItem("x20");
        scale.getItems().add(x5);
        scale.getItems().add(x10);
        scale.getItems().add(x20);
        video.getItems().add(scale);
        List<String> colorList = new ArrayList<>();
        final Field[] fields = Color.class.getFields();
        for (final Field field : fields) {
            if (field.getType() == Color.class) {
                colorList.add(field.getName());
            }
        }
        Menu backGroundColor = new Menu("Background color");
        Menu pixelColor = new Menu("Pixel color");
        for (String color : colorList) {
            MenuItem bgColorMenu = new MenuItem(color);
            bgColorMenu.setOnAction((event) ->
                    EventService.getInstance().postEvent(new ChangeColorEvent(BACKGROUND, Color.valueOf(((MenuItem) (event.getSource())).getText()))));
            MenuItem pixelColorMenu = new MenuItem(color);
            pixelColorMenu.setOnAction((event) ->
                    EventService.getInstance().postEvent(new ChangeColorEvent(PIXEL, Color.valueOf(((MenuItem) (event.getSource())).getText()))));
            backGroundColor.getItems().add(bgColorMenu);
            pixelColor.getItems().add(pixelColorMenu);
        }
        video.getItems().add(backGroundColor);
        video.getItems().add(pixelColor);

        x5.setOnAction((event) -> EventService.getInstance().postEvent(new ChangeScaleEvent(5)));
        x10.setOnAction((event) -> EventService.getInstance().postEvent(new ChangeScaleEvent(10)));
        x20.setOnAction((event) -> EventService.getInstance().postEvent(new ChangeScaleEvent(20)));
        getMenus().add(emulator);
        getMenus().add(video);
        AsyncEventService.getInstance().registerHandler(this);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handlePauseEvent(PauseEvent event) {
        pauseItem.setSelected(event.isPauseFlag());
    }
}
