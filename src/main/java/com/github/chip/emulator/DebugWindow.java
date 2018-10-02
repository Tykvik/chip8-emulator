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

import com.github.chip.emulator.core.events.*;
import com.github.chip.emulator.core.services.AsyncEventService;
import com.google.common.eventbus.Subscribe;
import com.sun.javafx.scene.control.skin.ListViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.github.chip.emulator.core.formats.Formats.INDEX_REGISTER_FORMAT;
import static com.github.chip.emulator.core.formats.Formats.REGISTER_NUMBER_FORMAT;
import static com.github.chip.emulator.core.formats.Formats.REGISTER_VALUE_FORMAT;

/**
 * @author helloween
 */
public class DebugWindow extends AnchorPane {
    private static final Color BACKGROUND_COLOR = Color.valueOf("0x202a35");
    private static final String DELIMITER_VALUE = " = ";

    private final Font             font;
    private final List<Label>      registerValueLabels = new ArrayList<>();
    private final Label            indexRegisterValueLabel;
    private final Label            delayTimerValueLabel;
    private final Label            soundTimerValueLabel;
    private final ListView<String> programList;

    public DebugWindow(List<String> programListing) {
        GridPane registersPane = new GridPane();
        int column = 0;
        font = Font.loadFont(DebugWindow.class.getClassLoader().getResource("dfont.ttf").toExternalForm(), 10);

        for (int i = 0; i <= 0xF; ++i) {
            Label registerNameLabel  = createLabel(REGISTER_NUMBER_FORMAT.format(i));
            Label registerValueLabel = createLabel(REGISTER_VALUE_FORMAT.format(0x0));
            Label delimiterLabel     = createLabel(DELIMITER_VALUE);
            registerValueLabels.add(registerValueLabel);

            registersPane.add(registerNameLabel,    column,     i);
            registersPane.add(delimiterLabel,       column + 1, i);
            registersPane.add(registerValueLabel,   column + 2, i);
            delimiterLabel = new Label("  ");
            registersPane.add(delimiterLabel,       column + 3, i);
        }
        column = column + 4;

        Label indexRegisterNameLabel    = createLabel("IR");
        Label delimiterLabel            = createLabel(DELIMITER_VALUE);
        indexRegisterValueLabel         = createLabel(INDEX_REGISTER_FORMAT.format(0x0));
        registersPane.add(indexRegisterNameLabel,   column++, 0);
        registersPane.add(delimiterLabel,           column++, 0);
        registersPane.add(indexRegisterValueLabel,  column++, 0);

        Label delayTimerNameLabel      = createLabel("DT");
        delimiterLabel                 = createLabel(DELIMITER_VALUE);
        delayTimerValueLabel           = createLabel(REGISTER_VALUE_FORMAT.format(0x0));
        registersPane.add(delayTimerNameLabel,  column - 3, 1);
        registersPane.add(delimiterLabel,       column - 2, 1);
        registersPane.add(delayTimerValueLabel, column - 1, 1);

        Label soundTimerNameLabel     = createLabel("ST");
        delimiterLabel                = createLabel(DELIMITER_VALUE);
        soundTimerValueLabel          = createLabel(REGISTER_VALUE_FORMAT.format(0x0));
        registersPane.add(soundTimerNameLabel,  column - 3, 2);
        registersPane.add(delimiterLabel,       column - 2, 2);
        registersPane.add(soundTimerValueLabel, column - 1, 2);
        getChildren().add(registersPane);
        setLeftAnchor(registersPane, 0.0);

        programList = new ListView<>();
        programList.setCellFactory((list) -> new ProgramRectCell());
        programList.setMouseTransparent(true);
        programList.setFocusTraversable(false);
        programList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        programList.setMinWidth(350);
        programList.setMaxHeight(305);

        programList.setItems(FXCollections.observableArrayList(programListing));
        programList.getSelectionModel().select(0);
        programList.setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));

        getChildren().add(programList);
        setRightAnchor(programList, 0.0);

        setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));
        AsyncEventService.getInstance().registerHandler(this);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleChangeRegisterValueEvent(ChangeRegisterValueEvent event) {
        Platform.runLater(() -> registerValueLabels.get(event.getRegisterNumber()).setText(REGISTER_VALUE_FORMAT.format(event.getValue())));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleChangeIndexRegisterEvent(ChangeIndexRegisterValueEvent event) {
        Platform.runLater(() -> indexRegisterValueLabel.setText(INDEX_REGISTER_FORMAT.format(event.getValue())));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleChangeDelayTimerValueEvent(ChangeDelayTimerValueEvent event) {
        Platform.runLater(() -> delayTimerValueLabel.setText(REGISTER_VALUE_FORMAT.format(event.getValue())));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleChangeSoundTimerValueEvent(ChangeSoundTimerValueEvent event) {
        Platform.runLater(() -> soundTimerValueLabel.setText(REGISTER_VALUE_FORMAT.format(event.getValue())));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleChangeProgramCounterEvent(ChangeProgramCounterEvent event) {
        Platform.runLater(() -> {
            ListViewSkin<?> ts  = (ListViewSkin<?>) programList.getSkin();
            VirtualFlow<?> vf   = (VirtualFlow<?>) ts.getChildren().get(0);
            int index = event.getValue() / 2;
            if (vf.getLastVisibleCell().getIndex() < index || vf.getFirstVisibleCell().getIndex() > index) {
                programList.scrollTo(index);
            }
            programList.getSelectionModel().select(index);
        });
    }

    private Label createLabel(final String text) {
        Label label = new Label(text);
        label.setFont(font);
        label.setTextFill(Color.WHITE);
        return label;
    }

    private class ProgramRectCell extends ListCell<String> {
        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            if (selected)
                setBackground(new Background(new BackgroundFill(Color.ROYALBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
            else
                setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(item);
            setFont(font);
            setTextFill(Color.WHITE);
            setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));
        }
    }
}
