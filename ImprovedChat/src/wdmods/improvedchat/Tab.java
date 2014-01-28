package wdmods.improvedchat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

/**
 * @author wd1966
 */
public class Tab
{
    public String name;
    public String prefix;
    public int width;
    public boolean blink = false;
    public boolean blinking = false;
    public List<ImprovedChatLine> chatLines = new ArrayList<ImprovedChatLine>();
    public ArrayList<Pattern> track = new ArrayList<Pattern>();
    public ArrayList<Pattern> ignore = new ArrayList<Pattern>();
    public int chatScroll = 0;

    public Tab(String tabName)
    {
        name = tabName;
        prefix = "";
        width = ImprovedChat.getFontRenderer().getStringWidth(tabName);
    }

    public boolean valid(String text)
    {
        if (text != null && !text.trim().equals(""))
        {
            Iterator<Pattern> ignoreIterator = ignore.iterator();
            Pattern ignorePattern;

            do
            {
                if (!ignoreIterator.hasNext())
                {
                    ignoreIterator = track.iterator();

                    do
                    {
                        if (!ignoreIterator.hasNext())
                            return false;

                        ignorePattern = ignoreIterator.next();
                    }
                    while (!ignorePattern.matcher(text).find());

                    return true;
                }

                ignorePattern = ignoreIterator.next();
            }
            while (!ignorePattern.matcher(text).find());
        }
        
		return false;
    }

    public boolean ignored(String text)
    {
        Iterator<Pattern> ignoreIterator = ignore.iterator();
        Pattern ignorePattern;

        do
        {
            if (!ignoreIterator.hasNext())
                return false;

            ignorePattern = ignoreIterator.next();
        }
        while (!ignorePattern.matcher(text).find());

        return true;
    }

    public void track(String text)
    {
        track.add(Pattern.compile(text));
    }

    public void ignore(String text)
    {
        ignore.add(Pattern.compile(text));
    }

    public void add(String text)
    {
        chatLines.add(0, new ImprovedChatLine(Minecraft.getMinecraft().ingameGUI.getUpdateCounter(), new ChatComponentText(text), 0));
        blinking = blink;
    }

    public void add(ImprovedChatLine line)
    {
        chatLines.add(0, line);
        blinking = blink;
    }

    public void setName(String name)
    {
        this.name = name;
        width = ImprovedChat.getFontRenderer().getStringWidth(name);
    }
}
