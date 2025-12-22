package net.aaavein.tidymods.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
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
        if (text.isEmpty()) {
            return FormattedCharSequence.EMPTY;
        }

        int[] colors = calculateColors(text);

        List<FormattedCharSequence> parts = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int color = colors[i];
            int start = i;
            while (i < text.length() && colors[i] == color) {
                i++;
            }
            parts.add(FormattedCharSequence.forward(
                    text.substring(start, i),
                    Style.EMPTY.withColor(color)
            ));
        }

        return FormattedCharSequence.composite(parts);
    }

    private int[] calculateColors(String text) {
        int[] colors = new int[text.length()];
        Arrays.fill(colors, DEFAULT_TEXT_COLOR);

        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == ' ') {
                colors[i] = DEFAULT_TEXT_COLOR;
                i++;
                continue;
            }

            char firstChar = text.charAt(i);

            if (isFilterPrefix(firstChar)) {
                int filterColor = getFilterColor(firstChar);
                colors[i] = filterColor;
                i++;

                if (i < text.length() && text.charAt(i) == '"') {
                    do {
                        colors[i] = filterColor;
                        i++;
                    } while (i < text.length() && text.charAt(i) != '"');
                    if (i < text.length()) {
                        colors[i] = filterColor;
                        i++;
                    }
                } else {
                    while (i < text.length() && text.charAt(i) != ' ') {
                        colors[i] = filterColor;
                        i++;
                    }
                }
            } else {
                while (i < text.length() && text.charAt(i) != ' ') {
                    colors[i] = DEFAULT_TEXT_COLOR;
                    i++;
                }
            }
        }

        return colors;
    }

    private boolean isFilterPrefix(char c) {
        return c == '#' || c == '@' || c == '!' || c == '$';
    }

    private int getFilterColor(char prefix) {
        return switch (prefix) {
            case '#' -> CATEGORY_TEXT_COLOR;
            case '@' -> MODID_TEXT_COLOR;
            case '!' -> AUTHOR_TEXT_COLOR;
            case '$' -> LICENSE_TEXT_COLOR;
            default -> DEFAULT_TEXT_COLOR;
        };
    }

    private int getFilterSuggestionColor(char prefix) {
        return switch (prefix) {
            case '#' -> CATEGORY_SUGGESTION_COLOR;
            case '@' -> MODID_SUGGESTION_COLOR;
            case '!' -> AUTHOR_SUGGESTION_COLOR;
            case '$' -> LICENSE_SUGGESTION_COLOR;
            default -> DEFAULT_SUGGESTION_COLOR;
        };
    }

    private int getSuggestionColor() {
        String value = getValue();
        if (value.isEmpty()) {
            return DEFAULT_SUGGESTION_COLOR;
        }

        String currentToken = getCurrentToken(value);
        if (currentToken.isEmpty()) {
            return DEFAULT_SUGGESTION_COLOR;
        }

        char firstChar = currentToken.charAt(0);
        if (isFilterPrefix(firstChar)) {
            return getFilterSuggestionColor(firstChar);
        }

        return DEFAULT_SUGGESTION_COLOR;
    }

    private String getCurrentToken(String text) {
        if (text.isEmpty()) {
            return "";
        }

        int tokenStart = 0;
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                tokenStart = i + 1;
            }
        }

        return text.substring(tokenStart);
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

        String currentToken = getCurrentToken(text);
        if (currentToken.isEmpty()) {
            currentSuggestion = "";
            return;
        }

        char firstChar = currentToken.charAt(0);
        boolean isFilter = isFilterPrefix(firstChar);

        String searchPart;
        boolean isQuoted = false;

        if (isFilter) {
            if (currentToken.length() > 1 && currentToken.charAt(1) == '"') {
                isQuoted = true;
                searchPart = currentToken.substring(2);
            } else {
                searchPart = currentToken.substring(1);
            }
        } else {
            searchPart = currentToken;
        }

        String searchLower = searchPart.toLowerCase(Locale.ROOT);

        for (String candidate : suggestionsSupplier.get()) {
            if (candidate.isEmpty()) {
                continue;
            }

            char candFirst = candidate.charAt(0);

            if (isFilter) {
                if (candFirst != firstChar) {
                    continue;
                }

                String candValue = candidate.substring(1);

                if (candValue.toLowerCase(Locale.ROOT).startsWith(searchLower)) {
                    String suggestion = candValue.substring(searchPart.length());

                    if (candValue.contains(" ") && isQuoted) {
                        suggestion = suggestion + "\"";
                    }

                    currentSuggestion = suggestion;
                    return;
                }
            } else {
                if (isFilterPrefix(candFirst)) {
                    continue;
                }

                if (candidate.toLowerCase(Locale.ROOT).startsWith(searchLower)) {
                    currentSuggestion = candidate.substring(searchPart.length());
                    return;
                }
            }
        }

        currentSuggestion = "";
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB && !currentSuggestion.isEmpty() && isFocused()) {
            String currentValue = getValue();
            String currentToken = getCurrentToken(currentValue);

            if (!currentToken.isEmpty() && isFilterPrefix(currentToken.charAt(0))) {
                boolean isAlreadyQuoted = currentToken.length() > 1 && currentToken.charAt(1) == '"';

                if (!isAlreadyQuoted) {
                    String valueWithoutPrefix = currentToken.substring(1) + currentSuggestion;
                    if (valueWithoutPrefix.contains(" ")) {
                        int tokenStartInValue = currentValue.length() - currentToken.length();
                        char prefix = currentToken.charAt(0);
                        String newValue = currentValue.substring(0, tokenStartInValue)
                                + prefix + "\"" + valueWithoutPrefix + "\"";
                        setValue(newValue);
                        moveCursorToEnd(false);
                        return true;
                    }
                }
            }

            setValue(currentValue + currentSuggestion);
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