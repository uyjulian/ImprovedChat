package net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import wdmods.improvedchat.ImprovedChat;
import wdmods.improvedchat.overrides.GuiImprovedChatNewChat;
import wdmods.improvedchat.overrides.GuiImprovedChatTextField;

/**
 *
 * @author Adam Mummery-Smith
 */
public class GuiImprovedChat extends GuiChat
{
    @SuppressWarnings("unused")
	private String rememberedText = "";

    /**
     * keeps position of which chat message you will select when you press up, (does not increase for duplicated
     * messages sent immediately after each other)
     */
    @SuppressWarnings("unused")
	private int sentHistoryCursor = -1;
    private boolean completing = false;
    private boolean waitingForAutoComplete = false;
    private int autoCompleteOption = 0;
    private List<String> autoCompleteChoices = new ArrayList<String>();

    /**
     * is the text that appears when you press the chat key and the input box appears pre-filled
     */
    private String defaultInputFieldText = "";
    
    public GuiImprovedChat() {}
    
    public GuiImprovedChat(GuiChat oldChat)
    {
    	defaultInputFieldText = oldChat.inputField.getText();
    	setWorldAndResolution(oldChat.mc, oldChat.width, oldChat.height);
    }

    public GuiImprovedChat(String defaultText)
    {
        defaultInputFieldText = defaultText;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    @Override
	public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        sentHistoryCursor = mc.ingameGUI.getChatGUI().getSentMessages().size();
        
        inputField = new GuiImprovedChatTextField(fontRendererObj, 4, height - 12, width - 4, 12);
        inputField.setMaxStringLength(ImprovedChat.getChatLineMaxLength());
        inputField.setEnableBackgroundDrawing(false);
        inputField.setFocused(true);
        inputField.setText(defaultInputFieldText);
        inputField.setCanLoseFocus(false);
    }

    /**
     * Fired when a key is typed. This is the equivalent of KeyListener.keyTyped(KeyEvent e).
     */
    @Override
	protected void keyTyped(char keyChar, int keyCode)
    {
        waitingForAutoComplete = false;

        if (keyCode == Keyboard.KEY_TAB)
        {
            //completePlayerName();
        }
        else
        {
            completing = false;
        }

        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            mc.displayGuiScreen((GuiScreen)null);
        }
        else if (keyCode != Keyboard.KEY_RETURN && keyCode != Keyboard.KEY_NUMPADENTER)
        {
            if (keyCode == Keyboard.KEY_UP && ImprovedChat.commandScroll < ImprovedChat.getPastCommands().size())
            {
                ++ImprovedChat.commandScroll;
                inputField.setText(ImprovedChat.getPastCommands().get(ImprovedChat.getPastCommands().size() - ImprovedChat.commandScroll));
            }
            else if (keyCode == Keyboard.KEY_DOWN && ImprovedChat.commandScroll > 0)
            {
                --ImprovedChat.commandScroll;

                if (ImprovedChat.commandScroll == 0)
                {
                    inputField.setText("");
                }
                else
                {
                    inputField.setText(ImprovedChat.getPastCommands().get(ImprovedChat.getPastCommands().size() - ImprovedChat.commandScroll));
                }
            }
            else if (keyCode == Keyboard.KEY_PRIOR)
            {
                ((GuiImprovedChatNewChat)mc.ingameGUI.getChatGUI()).doScroll(ImprovedChat.getScrollLines());
            }
            else if (keyCode == Keyboard.KEY_NEXT)
            {
                ((GuiImprovedChatNewChat)mc.ingameGUI.getChatGUI()).doScroll(-ImprovedChat.getScrollLines());
            }
            else
            {
                inputField.textboxKeyTyped(keyChar, keyCode);
            }
        }
        else
        {
            String currentText = inputField.getText().trim();

            if (currentText.length() > 0)
            {
                ImprovedChat.getPastCommands().add(currentText);
                ImprovedChat.process(currentText);
                ImprovedChat.commandScroll = 0;
                ImprovedChat.currentTab().chatScroll = 0;
            }

            mc.displayGuiScreen((GuiScreen)null);
        }
    }

    /**
     * Handles mouse input.
     */
    @Override
	public void handleMouseInput()
    {
        super.handleMouseInput();
        int scrollAmount = Mouse.getEventDWheel();

        if (scrollAmount != 0)
        {
            if (scrollAmount > 1)
                scrollAmount = ImprovedChat.getScrollLines();

            if (scrollAmount < -1)
                scrollAmount = -ImprovedChat.getScrollLines();

            GuiNewChat chatGUI = mc.ingameGUI.getChatGUI();
			if (chatGUI instanceof GuiImprovedChatNewChat)
            {
            	((GuiImprovedChatNewChat)chatGUI).doScroll(scrollAmount);
            }
        }
    }

//    /**
//     * Autocompletes player name
//     */
//	@Override
//	public void completePlayerName()
//    {
//        if (completing)
//        {
//            inputField.deleteFromCursor(inputField.func_146197_a(-1, inputField.getCursorPosition(), false) - inputField.getCursorPosition());
//
//            if (autoCompleteOption >= autoCompleteChoices.size())
//            {
//                autoCompleteOption = 0;
//            }
//        }
//        else
//        {
//            int cursorPos = inputField.func_146197_a(-1, inputField.getCursorPosition(), false);
//            autoCompleteChoices.clear();
//            autoCompleteOption = 0;
//            String afterCursor = inputField.getText().substring(cursorPos).toLowerCase();
//            String beforeCursor = inputField.getText().substring(0, inputField.getCursorPosition());
//            requestAutoComplete(beforeCursor, afterCursor);
//
//            if (autoCompleteChoices.isEmpty())
//            {
//                return;
//            }
//
//            completing = true;
//            inputField.deleteFromCursor(cursorPos - inputField.getCursorPosition());
//        }
//
//        if (autoCompleteChoices.size() > 1)
//        {
//            StringBuilder sb = new StringBuilder();
//            String choice;
//
//            for (Iterator<String> iter = autoCompleteChoices.iterator(); iter.hasNext(); sb.append(choice))
//            {
//                choice = iter.next();
//
//                if (sb.length() > 0)
//                    sb.append(", ");
//            }
//
//            mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(sb.toString(), 1);
//        }
//
//        String nextChoice = autoCompleteChoices.get(autoCompleteOption++);
//        inputField.writeText(ImprovedChat.stripColors(nextChoice));
//    }
//
//    private void requestAutoComplete(String beforeCursor, String afterCursor)
//    {
//        if (beforeCursor.length() >= 1)
//        {
//            mc.thePlayer.sendQueue.addToSendQueue(new Packet203AutoComplete(beforeCursor));
//            waitingForAutoComplete = true;
//        }
//    }

    /**
     * Draws the screen and all the components in it.
     */
    @Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
//        int colour = ((ImprovedChat.bgOpacity & 255) << 24) + ImprovedChat.bgColor;
//        drawRect(2, this.height - 14, this.width - 2, this.height - 2, colour);
        inputField.drawTextBox();
    }

//	@Override
//	public void func_73894_a(String[] options)
//    {
//        if (waitingForAutoComplete)
//        {
//            autoCompleteChoices.clear();
//            String[] selectedOptions = options;
//            int optionsCount = options.length;
//
//            for (int opt = 0; opt < optionsCount; ++opt)
//            {
//                String option = selectedOptions[opt];
//
//                if (option.length() > 0)
//                    autoCompleteChoices.add(option);
//            }
//
//            if (autoCompleteChoices.size() > 0)
//            {
//                completing = true;
//                completePlayerName();
//            }
//        }
//    }
}
