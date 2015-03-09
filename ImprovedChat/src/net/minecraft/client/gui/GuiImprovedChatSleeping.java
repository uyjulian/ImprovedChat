package net.minecraft.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import wdmods.improvedchat.ImprovedChat;
import wdmods.improvedchat.overrides.GuiImprovedChatNewChat;
import wdmods.improvedchat.overrides.GuiImprovedChatTextField;

/**
 *
 * @author Adam Mummery-Smith
 */
public class GuiImprovedChatSleeping extends GuiSleepMP
{
    @SuppressWarnings("unused")
	private String rememberedText = "";

    /**
     * keeps position of which chat message you will select when you press up, (does not increase for duplicated
     * messages sent immediately after each other)
     */
    @SuppressWarnings("unused")
	private int sentHistoryCursor = -1;
    private boolean field_73897_d = false;
    private boolean field_73905_m = false;
    private int field_73903_n = 0;
    private List<String> field_73904_o = new ArrayList<String>();

    /**
     * is the text that appears when you press the chat key and the input box appears pre-filled
     */
    private String defaultInputFieldText = "";
    
    public GuiImprovedChatSleeping() {}
    
    public GuiImprovedChatSleeping(GuiSleepMP oldChat)
    {
    	defaultInputFieldText = oldChat.inputField.getText();
    	setWorldAndResolution(oldChat.mc, oldChat.width, oldChat.height);
    }

    public GuiImprovedChatSleeping(String defaultInputFieldText)
    {
        this.defaultInputFieldText = defaultInputFieldText;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    @SuppressWarnings("unchecked")
	@Override
	public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        sentHistoryCursor = mc.ingameGUI.getChatGUI().getSentMessages().size();
        
        inputField = new GuiImprovedChatTextField(0, fontRendererObj, 4, height - 12, width - 4, 12);
        inputField.setMaxStringLength(ImprovedChat.getChatLineMaxLength());
        inputField.setEnableBackgroundDrawing(false);
        inputField.setFocused(true);
        inputField.setText(defaultInputFieldText);
        inputField.setCanLoseFocus(false);

        buttonList.clear();
        buttonList.add(new GuiButton(1, width / 2 - 100, height - 40, I18n.format("multiplayer.stopSleeping", new Object[0])));
    }

    /**
     * Fired when a key is typed. This is the equivalent of KeyListener.keyTyped(KeyEvent e).
     */
    @Override
	protected void keyTyped(char keyChar, int keyCode)
    {
        if (keyCode == 1)
        {
            //wakeEntity();
        }
        else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)
        {
    		sendChatMessage();

            inputField.setText("");
            mc.ingameGUI.getChatGUI().resetScroll();
        }
        else
        {
        	field_73905_m = false;
        	
        	if (keyCode == Keyboard.KEY_TAB)
        	{
        		//completePlayerName();
        	}
        	else
        	{
        		field_73897_d = false;
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
        		sendChatMessage();
        		mc.displayGuiScreen((GuiScreen)null);
        	}
        }
    }

	/**
	 * 
	 */
	protected void sendChatMessage()
	{
		String text = inputField.getText().trim();
		
		if (text.length() > 0)
		{
			ImprovedChat.getPastCommands().add(text);
			ImprovedChat.process(text);
			ImprovedChat.commandScroll = 0;
			ImprovedChat.currentTab().chatScroll = 0;
		}
	}

    /**
     * Handles mouse input.
     * @throws IOException 
     */
    @Override
	public void handleMouseInput() throws IOException
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
//        if (field_73897_d)
//        {
//            inputField.deleteFromCursor(inputField.func_73798_a(-1, inputField.getCursorPosition(), false) - inputField.getCursorPosition());
//
//            if (field_73903_n >= field_73904_o.size())
//            {
//                field_73903_n = 0;
//            }
//        }
//        else
//        {
//            int cursorPos = inputField.func_73798_a(-1, inputField.getCursorPosition(), false);
//            field_73904_o.clear();
//            field_73903_n = 0;
//            String afterCursor = inputField.getText().substring(cursorPos).toLowerCase();
//            String beforeCursor = inputField.getText().substring(0, inputField.getCursorPosition());
//            requestAutoComplete(beforeCursor, afterCursor);
//
//            if (field_73904_o.isEmpty())
//            {
//                return;
//            }
//
//            field_73897_d = true;
//            inputField.deleteFromCursor(cursorPos - inputField.getCursorPosition());
//        }
//
//        if (field_73904_o.size() > 1)
//        {
//            StringBuilder sb = new StringBuilder();
//            String choice;
//
//            for (Iterator<String> iter = field_73904_o.iterator(); iter.hasNext(); sb.append(choice))
//            {
//                choice = iter.next();
//
//                if (sb.length() > 0)
//                {
//                    sb.append(", ");
//                }
//            }
//
//            mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(sb.toString(), 1);
//        }
//
//        String nextOption = field_73904_o.get(field_73903_n++);
//        inputField.writeText(ImprovedChat.stripColors(nextOption));
//    }
//
//    private void requestAutoComplete(String beforeCursor, String afterCursor)
//    {
//        if (beforeCursor.length() >= 1)
//        {
//            mc.thePlayer.sendQueue.addToSendQueue(new Packet203AutoComplete(beforeCursor));
//            field_73905_m = true;
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

        for (int btn = 0; btn < buttonList.size(); ++btn)
        {
            GuiButton button = (GuiButton)buttonList.get(btn);
            button.drawButton(mc, mouseX, mouseY);
        }
    }

//	@Override
//	public void func_73894_a(String[] options)
//    {
//        if (field_73905_m)
//        {
//            field_73904_o.clear();
//            String[] choices = options;
//            int numOptions = options.length;
//
//            for (int opt = 0; opt < numOptions; ++opt)
//            {
//                String choice = choices[opt];
//
//                if (choice.length() > 0)
//                {
//                    field_73904_o.add(choice);
//                }
//            }
//
//            if (field_73904_o.size() > 0)
//            {
//                field_73897_d = true;
//                completePlayerName();
//            }
//        }
//    }

//    /**
//     * Fired when a control is clicked. This is the equivalent of ActionListener.actionPerformed(ActionEvent e).
//     */
//    @Override
//	protected void actionPerformed(GuiButton button)
//    {
//        if (button.id == 1)
//        {
//            wakeEntity();
//        }
//        else
//        {
//            super.actionPerformed(button);
//        }
//    }

//    /**
//     * Wakes the entity from the bed
//     */
//    private void wakeEntity()
//    {
//        NetClientHandler nch = mc.thePlayer.sendQueue;
//        nch.addToSendQueue(new Packet19EntityAction(mc.thePlayer, 3));
//    }
}
