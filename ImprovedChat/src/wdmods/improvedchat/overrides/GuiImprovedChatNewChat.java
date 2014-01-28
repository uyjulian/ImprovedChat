package wdmods.improvedchat.overrides;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiImprovedChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;
import wdmods.improvedchat.ImprovedChat;
import wdmods.improvedchat.ImprovedChatLine;

public class GuiImprovedChatNewChat extends GuiNewChat
{
    /** The Minecraft instance. */
    private final Minecraft mc;

    /** A list of messages previously sent through the chat GUI */
    private final List<String> sentMessages = new ArrayList<String>();

    /** Chat lines to be displayed in the chat box */
    private final List<ImprovedChatLine> chatLines = new ArrayList<ImprovedChatLine>();
    public int scrollAmount = 0;
    private boolean isScrolledBack = false;

    public GuiImprovedChatNewChat(Minecraft minecraft)
    {
    	super(minecraft);
        mc = minecraft;
    }

    @Override
	public void drawChat(int updateCounter)
    {
        if (mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN)
        {
            byte visibleLineCount = ImprovedChat.getChatLinesSmall();
            boolean chatOpen = false;
            int totalLines = chatLines.size();

            if (getChatOpen())
            {
            	visibleLineCount = ImprovedChat.getChatLinesBig();
            	chatOpen = true;
            }
            
            int chatLines = ImprovedChat.drawChatHistory(this, mc.fontRenderer, chatOpen, visibleLineCount, updateCounter);

            if (totalLines > 0)
            {
                if (chatOpen)
                {
                    int fontHeight = mc.fontRenderer.FONT_HEIGHT;
                    glTranslatef(0.0F, fontHeight, 0.0F);
                    int totalHeight = totalLines * fontHeight + totalLines;
                    int visibleHeight = chatLines * fontHeight + chatLines;
                    int scrollPos = scrollAmount * visibleHeight / totalLines;
                    int scrollAmt = visibleHeight * visibleHeight / totalHeight;

                    if (totalHeight != visibleHeight)
                    {
                        int scrollOpacity = scrollPos > 0 ? 170 : 96;
                        int scrollColour = isScrolledBack ? 0xCC3333 : 0x3333AA;
                        Gui.drawRect(0, -scrollPos, 2, -scrollPos - scrollAmt, scrollColour + (scrollOpacity << 24));
                        Gui.drawRect(2, -scrollPos, 1, -scrollPos - scrollAmt, 0xCCCCCC + (scrollOpacity << 24));
                    }
                }
            }
        }
    }

//    @Override
//    public void addTranslatedMessage(String message, Object... params)
//    {
//        printChatMessage(I18n.format(message, params));
//    }
    
    public List<ImprovedChatLine> getChatLines()
	{
		return chatLines;
	}
    
    
    /* (non-Javadoc)
	 * @see net.minecraft.src.GuiNewChat#getChatOpen()
	 */
	@Override
	public boolean getChatOpen()
	{
		// TODO Auto-generated method stub
		return super.getChatOpen() || mc.currentScreen instanceof GuiImprovedChat;
	}

    @Override
	public void clearChatMessages()
	{
    	chatLines.clear();
    	sentMessages.clear();
	}

	/**
     * prints the String to Chat. If the ID is not 0, deletes an existing Chat Line of that ID from the GUI
     */
    @Override
	public void printChatMessageWithOptionalDeletion(IChatComponent message, int id)
    {
        if (id != 0)
        {
            deleteChatLine(id);
        }

        ImprovedChat.receiveLine(message.getFormattedText(), mc.ingameGUI.getUpdateCounter(), id);
    }

    /**
     * Gets the list of messages previously sent through the chat GUI
     */
    @Override
	public List<String> getSentMessages()
    {
        return sentMessages;
    }

    /**
     * Adds this string to the list of sent messages, for recall using the up/down arrow keys
     */
    @Override
	public void addToSentMessages(String message)
    {
        if (sentMessages.isEmpty() || !(sentMessages.get(sentMessages.size() - 1)).equals(message))
        {
            sentMessages.add(message);
        }
    }

    /**
     * Resets the chat scroll (executed when the GUI is closed)
     */
    @Override
	public void resetScroll()
    {
        scrollAmount = 0;
        isScrolledBack = false;
    }

    /**
     * Scrolls the chat by the given number of lines.
     */
    @Override
	public void scroll(int amount)
    {
    }
    
    public void doScroll(int amount)
    {
        scrollAmount += amount;
        int lines = chatLines.size();

        if (scrollAmount > lines - 20)
            scrollAmount = lines - 20;

        if (scrollAmount <= 0)
        {
            scrollAmount = 0;
            isScrolledBack = false;
        }

        ImprovedChat.currentTab().chatScroll = scrollAmount;
    }

//    @Override
//	public ChatClickData func_73766_a(int mouseX, int mouseY)
//    {
//        if (!getChatOpen())
//            return null;
//        
//		ScaledResolution resolution = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
//		int scaleFactor = resolution.getScaleFactor();
//		int col = mouseX / scaleFactor - 3;
//		int row = mouseY / scaleFactor - 40;
//
//		if (col >= 0 && row >= 0)
//		{
//		    int lines = Math.min(ImprovedChat.getChatLinesBig(), getImprovedLines().size());
//
//		    if (col <= 320 && row < mc.fontRenderer.FONT_HEIGHT * lines + lines)
//		    {
//		        int lineNumber = row / (mc.fontRenderer.FONT_HEIGHT + 1) + scrollAmount;
//		        return new ChatClickData(mc.fontRenderer, getImprovedLines().get(lineNumber), col, row - (lineNumber - scrollAmount) * mc.fontRenderer.FONT_HEIGHT + lineNumber);
//		    }
//		}
//		
//		return null;
//    }

    /**
     * finds and deletes a Chat line by ID
     */
    @Override
	public void deleteChatLine(int lineID)
    {
        Iterator<ImprovedChatLine> chatLineIterator = chatLines.iterator();

        while (chatLineIterator.hasNext())
        {
            ImprovedChatLine chatLine = chatLineIterator.next();

            if (chatLine.getChatLineID() == lineID)
            {
                chatLineIterator.remove();
                return;
            }
        }
    }

    public void addImpChatLine(ImprovedChatLine line)
    {
        chatLines.add(line);
        sentMessages.add(line.func_151461_a().getFormattedText());
    }

    private List<ImprovedChatLine> getImprovedLines()
    {
        return ImprovedChat.currentTab().chatLines;
    }
}
