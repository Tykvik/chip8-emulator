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


import com.github.chip.emulator.core.events.NextStepEvent;
import com.github.chip.emulator.core.events.PressKeyEvent;
import com.github.chip.emulator.core.services.AsyncEventService;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * @author helloween
 */
public class KeyEventHandler implements javafx.event.EventHandler<KeyEvent> {
    private final BiMap<Integer, KeyCode> inputMapping = HashBiMap.create();

    public KeyEventHandler() {
        inputMapping.put(0x0, KeyCode.NUMPAD0);
        inputMapping.put(0x1, KeyCode.NUMPAD7);
        inputMapping.put(0x2, KeyCode.NUMPAD8);
        inputMapping.put(0x3, KeyCode.NUMPAD9);
        inputMapping.put(0x4, KeyCode.NUMPAD4);
        inputMapping.put(0x5, KeyCode.NUMPAD5);
        inputMapping.put(0x6, KeyCode.NUMPAD6);
        inputMapping.put(0x7, KeyCode.NUMPAD1);
        inputMapping.put(0x8, KeyCode.NUMPAD2);
        inputMapping.put(0x9, KeyCode.NUMPAD3);
        inputMapping.put(0xA, KeyCode.DECIMAL);
        inputMapping.put(0xB, KeyCode.ENTER);
        inputMapping.put(0xC, KeyCode.SUBTRACT);
        inputMapping.put(0xD, KeyCode.ADD);
        inputMapping.put(0xE, KeyCode.MULTIPLY);
        inputMapping.put(0xF, KeyCode.DIVIDE);
    }

    @Override
    public void handle(KeyEvent keyEvent) {
        Integer keyNumber = inputMapping.inverse().get(keyEvent.getCode());
        if (keyNumber != null)
            AsyncEventService.getInstance().postEvent(new PressKeyEvent(keyNumber));
        if (keyEvent.getCode() == KeyCode.F8)
            AsyncEventService.getInstance().postEvent(NextStepEvent.INSTANCE);
    }
}
