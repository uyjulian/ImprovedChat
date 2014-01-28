package wdmods.improvedchat.overrides;

import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChatAllowedCharacters;

import org.lwjgl.input.Keyboard;

import static org.lwjgl.opengl.GL11.*;
import wdmods.improvedchat.ImprovedChat;


public class GuiImprovedChatTextField extends GuiTextField
{
    /**
     * Have the font renderer from GuiScreen to render the textbox text into the screen.
     */
    private final FontRenderer fontRenderer;
    private final int xPos;
    private final int yPos;

    /** The width of this text field. */
    private final int width;
    private final int height;

    /** Have the current text beign edited on the textbox. */
    private String text = "";
    private int maxStringLength = 32;
    public int cursorCounter;
    private boolean enableBackgroundDrawing = true;

    /**
     * if true the textbox can lose focus by clicking elsewhere on the screen
     */
    private boolean canLoseFocus = true;

    /**
     * If this value is true along isEnabled, keyTyped will process the keys.
     */
    private boolean isFocused = false;

    /**
     * If this value is true along isFocused, keyTyped will process the keys.
     */
    private boolean isEnabled = true;
    private int field_73816_n = 0;
    public int cursorPosition = 0;

    /** other selection position, maybe the same as the cursor */
    private int selectionEnd = 0;
    private int enabledColor = 14737632;
    private int disabledColor = 7368816;

    /** True if this textbox is visible */
    private boolean visible = true;

    public GuiImprovedChatTextField(FontRenderer fontRenderer, int xPos, int yPos, int width, int height)
    {
    	super(fontRenderer, xPos, yPos, width, height);
        this.fontRenderer = fontRenderer;
        this.xPos = xPos;
        this.yPos = yPos;
        this.width = width;
        this.height = height;
    }

    /**
     * Increments the cursor counter
     */
    @Override
	public void updateCursorCounter()
    {
        ++cursorCounter;
    }

    /**
     * Sets the text of the textbox.
     */
    @Override
	public void setText(String newText)
    {
        if (newText.length() > maxStringLength)
        {
            text = newText.substring(0, maxStringLength);
        }
        else
        {
            text = newText;
        }

        setCursorPositionEnd();
    }

    /**
     * Returns the text beign edited on the textbox.
     */
    @Override
	public String getText()
    {
        return text;
    }

    /**
     * @return returns the text between the cursor and selectionEnd
     */
    @Override
	public String getSelectedText()
    {
        int start = cursorPosition < selectionEnd ? cursorPosition : selectionEnd;
        int end = cursorPosition < selectionEnd ? selectionEnd : cursorPosition;
        return text.substring(start, end);
    }

    /**
     * replaces selected text, or inserts text at the position on the cursor
     */
    @Override
	public void writeText(String text)
    {
        String newText = "";
        String filteredText = ChatAllowedCharacters.filerAllowedCharacters(text);
        int selStart = cursorPosition < selectionEnd ? cursorPosition : selectionEnd;
        int selEnd = cursorPosition < selectionEnd ? selectionEnd : cursorPosition;
        int availableChars = maxStringLength - this.text.length() - (selStart - selectionEnd);

        if (this.text.length() > 0)
        {
            newText = newText + this.text.substring(0, selStart);
        }

        int addedChars;

        if (availableChars < filteredText.length())
        {
            newText = newText + filteredText.substring(0, availableChars);
            addedChars = availableChars;
        }
        else
        {
            newText = newText + filteredText;
            addedChars = filteredText.length();
        }

        if (this.text.length() > 0 && selEnd < this.text.length())
        {
            newText = newText + this.text.substring(selEnd);
        }

        this.text = newText;
        moveCursorBy(selStart - selectionEnd + addedChars);
    }

    /**
     * Deletes the specified number of words starting at the cursor position. Negative numbers will delete words left of
     * the cursor.
     */
    @Override
	public void deleteWords(int numWords)
    {
        if (text.length() != 0)
        {
            if (selectionEnd != cursorPosition)
            {
                writeText("");
            }
            else
            {
                deleteFromCursor(getNthWordFromCursor(numWords) - cursorPosition);
            }
        }
    }

    /**
     * delete the selected text, otherwsie deletes characters from either side of the cursor. params: delete num
     */
    @Override
	public void deleteFromCursor(int numChars)
    {
        if (text.length() != 0)
        {
            if (selectionEnd != cursorPosition)
            {
                writeText("");
            }
            else
            {
                boolean delete = numChars < 0;
                int start = delete ? cursorPosition + numChars : cursorPosition;
                int end = delete ? cursorPosition : cursorPosition + numChars;
                String newText = "";

                if (start >= 0)
                {
                    newText = text.substring(0, start);
                }

                if (end < text.length())
                {
                    newText = newText + text.substring(end);
                }

                text = newText;

                if (delete)
                {
                    moveCursorBy(numChars);
                }
            }
        }
    }

    /**
     * see @getNthNextWordFromPos() params: N, position
     */
    @Override
	public int getNthWordFromCursor(int n)
    {
        return getNthWordFromPos(n, getCursorPosition());
    }

    /**
     * gets the position of the nth word. N may be negative, then it looks backwards. params: N, position
     */
    @Override
	public int getNthWordFromPos(int n, int position)
    {
        return func_146197_a(n, getCursorPosition(), true);
    }

    @Override
	public int func_146197_a(int n, int position, boolean option)
    {
        int pos = position;
        boolean reverse = n < 0;
        int size = Math.abs(n);

        for (int offset = 0; offset < size; ++offset)
        {
            if (reverse)
            {
                while (option && pos > 0 && text.charAt(pos - 1) == 32) --pos;
                while (pos > 0 && text.charAt(pos - 1) != 32) --pos;
            }
            else
            {
                int len = text.length();
                pos = text.indexOf(32, pos);

                if (pos == -1)
                {
                    pos = len;
                }
                else
                {
                    while (option && pos < len && text.charAt(pos) == 32) ++pos;
                }
            }
        }

        return pos;
    }

    /**
     * Moves the text cursor by a specified number of characters and clears the selection
     */
    @Override
	public void moveCursorBy(int amount)
    {
        setCursorPosition(selectionEnd + amount);
    }

    /**
     * sets the position of the cursor to the provided index
     */
    @Override
	public void setCursorPosition(int pos)
    {
        cursorPosition = pos;
        int len = text.length();

        if (cursorPosition < 0)
        {
            cursorPosition = 0;
        }

        if (cursorPosition > len)
        {
            cursorPosition = len;
        }

        setSelectionPos(cursorPosition);
    }

    /**
     * sets the cursors position to the beginning
     */
    @Override
	public void setCursorPositionZero()
    {
        setCursorPosition(0);
    }

    /**
     * sets the cursors position to after the text
     */
    @Override
	public void setCursorPositionEnd()
    {
        setCursorPosition(text.length());
    }

    /**
     * Call this method from you GuiScreen to process the keys into textbox.
     */
    @Override
	public boolean textboxKeyTyped(char keyChar, int keyCode)
    {
        if (isEnabled && isFocused)
        {
            switch (keyChar)
            {
                case 1: // CTRL + A
                    setCursorPositionEnd();
                    setSelectionPos(0);
                    return true;

                case 3: // CTRL + C
                    GuiScreen.setClipboardString(getSelectedText());
                    return true;

                case 22: // CTRL + V
                    writeText(GuiScreen.getClipboardString());
                    return true;

                case 24: // CTRL + X
                    GuiScreen.setClipboardString(getSelectedText());
                    writeText("");
                    return true;

                default:
                    switch (keyCode)
                    {
                        case Keyboard.KEY_BACK:
                            if (GuiScreen.isCtrlKeyDown())
                            {
                                deleteWords(-1);
                            }
                            else
                            {
                                deleteFromCursor(-1);
                            }

                            return true;

                        case Keyboard.KEY_HOME:
                            if (GuiScreen.isShiftKeyDown())
                            {
                                setSelectionPos(0);
                            }
                            else
                            {
                                setCursorPositionZero();
                            }

                            return true;

                        case Keyboard.KEY_LEFT:
                            if (GuiScreen.isShiftKeyDown())
                            {
                                if (GuiScreen.isCtrlKeyDown())
                                {
                                    setSelectionPos(getNthWordFromPos(-1, getSelectionEnd()));
                                }
                                else
                                {
                                    setSelectionPos(getSelectionEnd() - 1);
                                }
                            }
                            else if (GuiScreen.isCtrlKeyDown())
                            {
                                setCursorPosition(getNthWordFromCursor(-1));
                            }
                            else
                            {
                                moveCursorBy(-1);
                            }

                            return true;

                        case Keyboard.KEY_RIGHT:
                            if (GuiScreen.isShiftKeyDown())
                            {
                                if (GuiScreen.isCtrlKeyDown())
                                {
                                    setSelectionPos(getNthWordFromPos(1, getSelectionEnd()));
                                }
                                else
                                {
                                    setSelectionPos(getSelectionEnd() + 1);
                                }
                            }
                            else if (GuiScreen.isCtrlKeyDown())
                            {
                                setCursorPosition(getNthWordFromCursor(1));
                            }
                            else
                            {
                                moveCursorBy(1);
                            }

                            return true;

                        case Keyboard.KEY_END:
                            if (GuiScreen.isShiftKeyDown())
                            {
                                setSelectionPos(text.length());
                            }
                            else
                            {
                                setCursorPositionEnd();
                            }

                            return true;

                        case Keyboard.KEY_DELETE:
                            if (GuiScreen.isCtrlKeyDown())
                            {
                                deleteWords(1);
                            }
                            else
                            {
                                deleteFromCursor(1);
                            }

                            return true;

                        default:
                            if (ChatAllowedCharacters.isAllowedCharacter(keyChar))
                            {
                                writeText(Character.toString(keyChar));
                                return true;
                            }
                            
							return false;
                    }
            }
        }
        
		return false;
    }

    /**
     * Args: x, y, buttonClicked
     */
    @Override
	public void mouseClicked(int mouseX, int mouseY, int button)
    {
        boolean mouseOver = mouseX >= xPos && mouseX < xPos + width && mouseY >= yPos && mouseY < yPos + height;

        if (canLoseFocus)
        {
            setFocused(isEnabled && mouseOver);
        }

        if (isFocused && button == 0)
        {
            int xOffset = mouseX - xPos;

            if (enableBackgroundDrawing)
            {
                xOffset -= 4;
            }

            String visibleText = fontRenderer.trimStringToWidth(text.substring(field_73816_n), getWidth());
            setCursorPosition(fontRenderer.trimStringToWidth(visibleText, xOffset).length() + field_73816_n);
        }
    }

    /**
     * Draws the textbox
     * 
     * @TODO Fix the broken highlighting
     */
    @Override
	public void drawTextBox()
    {
        if (getVisible())
        {
            String modeText = null;

            if (ImprovedChat.getCurrentServer() != null && ImprovedChat.getCurrentServer().ChatMode != null)
            {
                modeText = ImprovedChat.getCurrentServer().ChatMode + " ";
            }

            int modeTextWidth = modeText != null && !enableBackgroundDrawing ? fontRenderer.getStringWidth(ImprovedChat.stripColors(modeText)) : 0;

            if (getEnableBackgroundDrawing())
            {
                Gui.drawRect(xPos - 1, yPos - 1, xPos + width + 1, yPos + height + 1, -6250336);
                Gui.drawRect(xPos, yPos, xPos + width, yPos + height, -16777216);
            }

            StringBuilder textBuilder = new StringBuilder();
            int backColour = ((ImprovedChat.getBgOpacity() & 255) << 24) + ImprovedChat.getBgColor();
            int foreColour = isEnabled ? enabledColor : disabledColor;
            int preTextPos = cursorPosition - field_73816_n;
            int postTextPos = selectionEnd - field_73816_n;
            String text = this.text;
            boolean cursorVisible = preTextPos >= 0 && preTextPos <= text.length();
            boolean cursorFlashState = isFocused && cursorCounter / 6 % 2 == 0 && cursorVisible;
            int left = enableBackgroundDrawing ? xPos + 4 : xPos;
            int top = enableBackgroundDrawing ? yPos + (height - 8) / 2 : yPos;
            left += modeTextWidth;

            if (postTextPos > text.length())
            {
                postTextPos = text.length();
            }

            char cursorChar = (char)(cursorFlashState ? 124 : 58);
            String textBeforeCursor = null;

            if (text.length() > 0)
            {
                textBeforeCursor = cursorVisible ? text.substring(0, preTextPos) : text;
                textBeforeCursor = ImprovedChat.replaceColors(textBeforeCursor);
                textBuilder.append(textBeforeCursor);
            }

            boolean cursorInsideString = cursorPosition < this.text.length() || this.text.length() >= getMaxStringLength();

            if (text.length() > 0 && cursorVisible && preTextPos < text.length())
            {
                Character lastColour = ImprovedChat.getLastColor(textBeforeCursor);
                String preCursorText = cursorChar + text.substring(preTextPos);
                preCursorText = ImprovedChat.replaceColors(preCursorText);

                if (lastColour != null)
                {
                    textBuilder.append("\u00a7").append(lastColour);
                }

                textBuilder.append(preCursorText);
            }

            String visibleText = textBuilder.toString();
            int maxTextLength = ImprovedChat.getMaxChatPacketLength();

            if (visibleText.length() >= maxTextLength)
            {
                visibleText = visibleText.substring(0, maxTextLength) + "\2474" + visibleText.substring(maxTextLength);
            }

            int extendedTextLength = maxTextLength * 2 + 2;

            if (visibleText.length() >= extendedTextLength)
            {
                visibleText = visibleText.substring(0, extendedTextLength) + "\247e" + visibleText.substring(extendedTextLength);
            }

            if (!cursorInsideString)
            {
                visibleText = visibleText + cursorChar;
            }

            List<String> displayLines = ImprovedChat.processDisplay(visibleText + " ");
            int linesToDisplay = displayLines.size();
            int textTop = enableBackgroundDrawing ? yPos + (height - 8) / 2 : yPos + 12 - 4 - (linesToDisplay <= 0 ? 1 : linesToDisplay) * 12;

            if (!getEnableBackgroundDrawing())
            {
                Gui.drawRect(xPos - 1, textTop + 2, xPos + width - 4, yPos + 12 - 2, backColour);
            }

            for (String displayLine : displayLines)
            {
                if (!enableBackgroundDrawing)
                {
                    fontRenderer.drawStringWithShadow(modeText, enableBackgroundDrawing ? xPos + 4 : xPos, yPos + 12 - 12 * linesToDisplay, foreColour);
                }

                fontRenderer.drawStringWithShadow(displayLine, left, enableBackgroundDrawing ? top + fontRenderer.FONT_HEIGHT - 10 : yPos + 12 - 12 * linesToDisplay, foreColour);
                --linesToDisplay;
            }
        }
    }

    /**
     * draws the vertical line cursor in the textbox
     */
    @SuppressWarnings("unused")
	private void drawCursorVertical(int x1, int y1, int x2, int y2)
    {
        int temp;

        if (x1 < x2)
        {
            temp = x1;
            x1 = x2;
            x2 = temp;
        }

        if (y1 < y2)
        {
            temp = y1;
            y1 = y2;
            y2 = temp;
        }

        Tessellator tessellator = Tessellator.instance;
        glColor4f(0.0F, 0.0F, 255.0F, 255.0F);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_COLOR_LOGIC_OP);
        glLogicOp(GL_OR_REVERSE);
        tessellator.startDrawingQuads();
        tessellator.addVertex(x1, y2, 0.0D);
        tessellator.addVertex(x2, y2, 0.0D);
        tessellator.addVertex(x2, y1, 0.0D);
        tessellator.addVertex(x1, y1, 0.0D);
        tessellator.draw();
        glDisable(GL_COLOR_LOGIC_OP);
        glEnable(GL_TEXTURE_2D);
    }

    @Override
	public void setMaxStringLength(int len)
    {
        maxStringLength = len;

        if (text.length() > len)
        {
            text = text.substring(0, len);
        }
    }

    /**
     * returns the maximum number of character that can be contained in this textbox
     */
    @Override
	public int getMaxStringLength()
    {
        return maxStringLength;
    }

    /**
     * returns the current position of the cursor
     */
    @Override
	public int getCursorPosition()
    {
        return cursorPosition;
    }

    /**
     * get enable drawing background and outline
     */
    @Override
	public boolean getEnableBackgroundDrawing()
    {
        return enableBackgroundDrawing;
    }

    /**
     * enable drawing background and outline
     */
    @Override
	public void setEnableBackgroundDrawing(boolean enabled)
    {
        enableBackgroundDrawing = enabled;
    }

    /**
     * Sets the text colour for this textbox (disabled text will not use this colour)
     */
    @Override
	public void setTextColor(int colour)
    {
        enabledColor = colour;
    }

    @Override
	public void setDisabledTextColour(int colour)
    {
        disabledColor = colour;
    }

    /**
     * setter for the focused field
     */
    @Override
	public void setFocused(boolean focused)
    {
        if (focused && !isFocused)
        {
            cursorCounter = 0;
        }

        isFocused = focused;
    }

    /**
     * getter for the focused field
     */
    @Override
	public boolean isFocused()
    {
        return isFocused;
    }
    
    @Override
	public void setEnabled(boolean enabled)
    {
        isEnabled = enabled;
    }

    /**
     * the side of the selection that is not the cursor, maye be the same as the cursor
     */
    @Override
	public int getSelectionEnd()
    {
        return selectionEnd;
    }

    /**
     * returns the width of the textbox depending on if the the box is enabled
     */
    @Override
	public int getWidth()
    {
        return getEnableBackgroundDrawing() ? width - 8 : width;
    }

    /**
     * Sets the position of the selection anchor (i.e. position the selection was started at)
     */
    @Override
	public void setSelectionPos(int pos)
    {
        int len = text.length();

        if (pos > len)
        {
            pos = len;
        }

        if (pos < 0)
        {
            pos = 0;
        }

        selectionEnd = pos;

        if (fontRenderer != null && field_73816_n != 0)
        {
            field_73816_n = 0;
        }
    }

    /**
     * if true the textbox can lose focus by clicking elsewhere on the screen
     */
    @Override
	public void setCanLoseFocus(boolean canLoseFocus)
    {
        this.canLoseFocus = canLoseFocus;
    }

    /**
     * @return {@code true} if this textbox is visible
     */
    @Override
	public boolean getVisible()
    {
        return visible;
    }

    /**
     * Sets whether or not this textbox is visible
     */
    @Override
	public void setVisible(boolean visible)
    {
        this.visible = visible;
    }
}
