package net.aaavein.tidymods.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AutoCompleteEditBox extends EditBox {

    private static final int DEFAULT_TEXT_COLOR = 0xE0E0E0;
    private static final int DEFAULT_SUGGESTION_COLOR = 0x888888;
    private static final int CATEGORY_TEXT_COLOR = 0x55FF55;
    private static final int CATEGORY_SUGGESTION_COLOR = 0x2A802A;
    private static final int MODID_TEXT_COLOR = 0x5555FF;
    private static final int MODID_SUGGESTION_COLOR = 0x2A2A80;
    private static final int AUTHOR_TEXT_COLOR = 0xFFFF55;
    private static final int AUTHOR_SUGGESTION_COLOR = 0x80802A;
    private static final int LICENSE_TEXT_COLOR = 0x55FFFF;
    private static final int LICENSE_SUGGESTION_COLOR = 0x2A8080;
    private static final int PADDING = 4;

    private final Font font;
    private final Supplier<List<String>> suggestionsSupplier;
    private Consumer<String> externalResponder;
    private String currentSuggestion = "";

    public AutoCompleteEditBox(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            Supplier<List<String>> suggestionsSupplier
    ) {
        super(font, x, y, width, height, message);
        this.font = font;
        this.suggestionsSupplier = suggestionsSupplier;
        super.setResponder(this::onTextChanged);
        setFormatter(this::formatText);
    }

    private FormattedCharSequence formatText(String text, int firstCharIndex) {
        return FormattedCharSequence.forward(text, Style.EMPTY.withColor(getTextColor()));
    }

    private int getTextColor() {
        String value = getValue();
        if (value.startsWith("#")) {
            return CATEGORY_TEXT_COLOR;
        } else if (value.startsWith("@")) {
            return MODID_TEXT_COLOR;
        } else if (value.startsWith("!")) {
            return AUTHOR_TEXT_COLOR;
        } else if (value.startsWith("$")) {
            return LICENSE_TEXT_COLOR;
        }
        return DEFAULT_TEXT_COLOR;
    }

    private int getSuggestionColor() {
        String value = getValue();
        if (value.startsWith("#")) {
            return CATEGORY_SUGGESTION_COLOR;
        } else if (value.startsWith("@")) {
            return MODID_SUGGESTION_COLOR;
        } else if (value.startsWith("!")) {
            return AUTHOR_SUGGESTION_COLOR;
        } else if (value.startsWith("$")) {
            return LICENSE_SUGGESTION_COLOR;
        }
        return DEFAULT_SUGGESTION_COLOR;
    }

    @Override
    public void setResponder(@NotNull Consumer<String> responder) {
        this.externalResponder = responder;
    }

    private void onTextChanged(String text) {
        updateSuggestion(text);
        if (externalResponder != null) {
            externalResponder.accept(text);
        }
    }

    private void updateSuggestion(String text) {
        if (text.isEmpty()) {
            currentSuggestion = "";
            return;
        }

        String lower = text.toLowerCase(Locale.ROOT);
        for (String candidate : suggestionsSupplier.get()) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
                currentSuggestion = candidate.substring(text.length());
                return;
            }
        }
        currentSuggestion = "";
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB && !currentSuggestion.isEmpty() && isFocused()) {
            setValue(getValue() + currentSuggestion);
            moveCursorToEnd(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(gfx, mouseX, mouseY, partialTick);
        renderSuggestion(gfx);
    }

    private void renderSuggestion(GuiGraphics gfx) {
        if (currentSuggestion.isEmpty() || !isFocused() || !isVisible()) {
            return;
        }

        if (getCursorPosition() != getValue().length()) {
            return;
        }

        String text = getValue();
        int textWidth = font.width(text);
        int innerWidth = getInnerWidth();

        if (textWidth >= innerWidth) {
            return;
        }

        int availableWidth = innerWidth - textWidth;
        String visible = font.plainSubstrByWidth(currentSuggestion, availableWidth);

        if (visible.isEmpty()) {
            return;
        }

        int x = getX() + PADDING + textWidth;
        int y = getY() + (height - 8) / 2;

        gfx.drawString(font, visible, x, y, getSuggestionColor(), false);
    }
}