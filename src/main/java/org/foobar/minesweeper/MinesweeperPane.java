/*
 * Copyright 2012, 2013 Evan Flynn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.foobar.minesweeper;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.PaneBuilder;

import org.foobar.minesweeper.model.Minefield;
import org.foobar.minesweeper.events.ChangeHandler;
import org.foobar.minesweeper.model.Minefield.State;
import org.foobar.minesweeper.model.Minefield.Cursor;
import org.foobar.minesweeper.model.Squares;

public final class MinesweeperPane implements HasParent {
  private final Parent root;
  private final Label status;
  private final Minefield field;
  private final FieldCanvas canvas;
  private final Minesweeper appController;

  public MinesweeperPane(MinesweeperPane pane) {
    this(pane.field, pane.appController);
  }

  public MinesweeperPane(final Minefield field, final Minesweeper appController) {
    this.field = field;
    this.appController = appController;

    canvas = new FieldCanvas();
    canvas.setLayoutX(14);
    canvas.setLayoutY(49.0);
    canvas.setWidth(240);
    canvas.setHeight(240);
    canvas.setOnMouseClicked(new EventHandler<MouseEvent>() {
      public void handle(MouseEvent event) {
        onCanvasClicked(event);
      }
    });

    canvas.setOnMousePressed(new EventHandler<MouseEvent>() {
      public void handle(MouseEvent event) {
        onCanvasPressed(event);
      }
    });

    root = PaneBuilder.create()
        .style("-fx-border-color: black;"
            + "-fx-border-width: 1;"
            + "-fx-border-radius: 6;"
            + "-fx-padding: 6;"
            + "-fx-background-color: white;")
        .prefHeight(308)
        .prefWidth(268)
        .children(
            HBoxBuilder.create()
            .layoutX(14)
            .layoutY(14)
            .spacing(10)
            .children(
                ButtonBuilder.create()
                .text("_New Game")
                .onAction(new EventHandler<ActionEvent>() {
                  @Override public void handle(ActionEvent event) {
                    onNewGame();
                  }
                }).build(),
                ButtonBuilder.create()
                .text("C_lone")
                .onAction(new EventHandler<ActionEvent>() {
                  @Override public void handle(ActionEvent event) {
                    appController.onClone(MinesweeperPane.this);
                  }
                }).build(),
                ButtonBuilder.create()
                .text("_Close")
                .onAction(new EventHandler<ActionEvent>() {
                  @Override public void handle(ActionEvent event) {
                    appController.onClose(MinesweeperPane.this);
                  }
                }).build()
            ).build(),
         canvas,
         status = LabelBuilder.create()
         .text("")
         .layoutX(14)
         .layoutY(290).build()
        ).build();

    Draggable.makeDraggable(root);

    field.addChangeHandler(new ChangeHandler() {
      @Override public void onUpdate() {
        drawBoard();
        updateText(field.getState());
      }
    });
  }

  public Parent asParent() {
    return root;
  }

  private void onNewGame() {
    field.reset();
  }

  private void onCanvasClicked(MouseEvent event) {
    Point point = asPoint(event);
    Minefield.Cursor cursor = field.cursor(point.row, point.column);
    int clicks = event.getClickCount();
    MouseButton button = event.getButton();

    // FIXME: square doesn't always redraw after mouse click

    if (button == MouseButton.MIDDLE
        || (clicks == 2 && button == MouseButton.PRIMARY)) {
      cursor.revealNearby();
    } else if (clicks == 1 && button == MouseButton.PRIMARY) {
      canvas.clearSelection();
      cursor.reveal();
    }
  }

  private void onCanvasPressed(MouseEvent event) {
    Point point = asPoint(event);
    Minefield.Cursor cursor = field.cursor(point.row, point.column);

    if (event.isSecondaryButtonDown()) {
      cursor.toggleFlag();
    } else if (event.isPrimaryButtonDown() && cursor.isRevealable()) {
      canvas.setSelection(point.row, point.column);
    }
  }

  private void drawSquare(Cursor cursor) {
    Image image = cursor.getType() == Squares.EXPOSED ? Tiles.getDigit(cursor
        .getNearbyMineCount()) : Tiles.getImage(cursor.getType());

    canvas.drawImage(cursor.getRow(), cursor.getColumn(), image);
  }

  private void updateText(Minefield.State state) {
    String text;

    switch(state) {
    case LOST:
      text = "You lost! Click New Game to try again.";
      break;
    case WON:
      text = "Congratulations, you won!";
      break;
    default:
      text = "";
    }

    status.setText(text);
  }

  private void drawBoard() {
    Minefield.Cursor cursor = field.cursor();

    for (int r = 0; r < field.getRowCount(); r++) {
      for (int c = 0; c < field.getColumnCount(); c++) {
        cursor.moveTo(r, c);
        drawSquare(cursor);
      }
    }
  }

  static final class Point {
    final int row;
    final int column;

    Point(int row, int column) {
      this.row = row;
      this.column = column;
    }
  }

  private Point asPoint(MouseEvent event) {
    return new Point(canvas.scaleRow(event.getY()),
                     canvas.scaleColumn(event.getX()));
  }


}
