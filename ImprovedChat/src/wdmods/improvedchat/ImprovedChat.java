package wdmods.improvedchat;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.*;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StringUtils;
import net.minecraft.world.storage.WorldInfo;
import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.input.Keyboard;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mumfrey.liteloader.core.LiteLoader;

import wdmods.improvedchat.PatternList.Entry;
import wdmods.improvedchat.Variables.Variable;
import wdmods.improvedchat.overrides.GuiImprovedChatNewChat;

/**
 *
 * @author wd1966
 */
public class ImprovedChat
{
    private static int fade = 0;
    public static int commandScroll = 0;
    private static Minecraft minecraft;
    private static Server globalServer;
    private static Server currentServer;
    private static Hashtable<String, Server> servers = new Hashtable<String, Server>();
    private static Hashtable<String, String[]> translations;
    private static Hashtable<String, String> constantVar;
    private static List<String> pastCommands = new ArrayList<String>();
    private static Set<icChannel> icChannels = new HashSet<icChannel>();
    private static PatternList[] patternList;
    private static File settings;
    private static File constantsFile;
    private static File colors;
    private static File modDir;
    private static final Pattern space = Pattern.compile(" ");
    private static Hashtable<String, Integer> colorHex = new Hashtable<String, Integer>();
    private static boolean chatDisabled = false;
    private static ImprovedChat instance;
    private static Document doc;
    private static Element topElement;
    private static Pattern colorTags = Pattern.compile("(\u00a7|&c)[0-9a-fA-FkKlLmMnNoOrR]|/&c");
    private static Pattern updateColor = Pattern.compile("(?<!/)&c(?=[0-9a-fA-FkKlLmMnNoOrR])");
    private static Pattern buxvillFix = Pattern.compile("\u00a7\u00a7");
    private static Pattern varP = Pattern.compile("\\$\\w*");
    private static Pattern varPinB = Pattern.compile("\'\\$\\w*\'");
    private static Pattern fixInvalidCharacter = Pattern.compile("\u00a7");
    static Hashtable<String, icCommand> commands;
    static Pattern scriptVar = Pattern.compile("\\$[0-9]+");
    private static Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    
    // private -> package-private to avoid synthetic accessors
    static int bgOpacity = 128;
    static int bgColor = 0;
    static int historyOpacity = 128;
    static int historyColor = 0;
    static int historyMaxLines = 300;
    static int chatLineMaxLength = 300;
    static byte chatLinesSmall = 10;
    static byte chatLinesBig = 20;
    static int scrollLines = 1;
    
    private static void createColorFile()
    {
        try
        {
            PrintWriter writer = new PrintWriter(ImprovedChat.colors);
            writer.println("black:0");
            writer.println("darkgreen:0x7F00");
            writer.println("darkblue:0x7F");
            writer.println("darkred:0x7F0000");
            writer.println("darkteal:0x7F7F");
            writer.println("purple:0x7F007F");
            writer.println("gold:0x7F7F00");
            writer.println("gray:0x7F7F7F");
            writer.println("blue:0xFF");
            writer.println("green:0xFF00");
            writer.println("teal:0xFFFF");
            writer.println("red:0xFF0000");
            writer.println("pink:0xFF00FF");
            writer.println("yellow:0xFFFF00");
            writer.println("white:0xFFFFFF");
            writer.close();
        }
        catch (FileNotFoundException ex)
        {
            ex.printStackTrace();
        }
    }

    static void init(Minecraft mc)
    {
        ImprovedChat.minecraft = mc;
        ImprovedChat.instance = new ImprovedChat();
    }

    private static Element newElem(Element element, String name)
    {
        Element child = ImprovedChat.doc.createElement(name);
        element.appendChild(child);
        return child;
    }

    public static String replaceColors(String text)
    {
        text = ImprovedChat.updateColor.matcher(ImprovedChat.patternList[2].process(text)).replaceAll("\u00a7");
        return text;
    }

    public static int colorCount(String text)
    {
        return text.split("\\\u00a7").length - 1;
    }

    public static String stripColors(String text)
    {
        text = ImprovedChat.colorTags.matcher(text).replaceAll("");
        return text;
    }

    private static void addTextNode(Element element, String name)
    {
        element.appendChild(ImprovedChat.doc.createTextNode(name));
    }

    private static void save()
    {
        try
        {
            ImprovedChat.doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        }
        catch (ParserConfigurationException ex)
        {
            ex.printStackTrace();
            return;
        }

        ImprovedChat.topElement = ImprovedChat.doc.createElement("Properties");
        ImprovedChat.doc.appendChild(ImprovedChat.topElement);
        ImprovedChat.topElement = ImprovedChat.newElem(ImprovedChat.topElement, "Input");
        Iterator<Entry> patterns = ImprovedChat.patternList[0].list.iterator();
        Element entryElement;
        Entry pattern;

        while (patterns.hasNext())
        {
            pattern = patterns.next();
            entryElement = ImprovedChat.newElem(ImprovedChat.topElement, "entry");
            entryElement.setAttribute("regex", pattern.pattern.toString());
            ImprovedChat.addTextNode(entryElement, pattern.replacement);
        }

        ImprovedChat.topElement = (Element)ImprovedChat.topElement.getParentNode();
        ImprovedChat.topElement = ImprovedChat.newElem(ImprovedChat.topElement, "Output");
        patterns = ImprovedChat.patternList[1].list.iterator();

        while (patterns.hasNext())
        {
            pattern = patterns.next();
            entryElement = ImprovedChat.newElem(ImprovedChat.topElement, "entry");
            entryElement.setAttribute("regex", pattern.pattern.toString());
            ImprovedChat.addTextNode(entryElement, pattern.replacement);
        }

        ImprovedChat.topElement = (Element)ImprovedChat.topElement.getParentNode();
        ImprovedChat.topElement = ImprovedChat.newElem(ImprovedChat.topElement, "Display");
        patterns = ImprovedChat.patternList[2].list.iterator();

        while (patterns.hasNext())
        {
            pattern = patterns.next();
            entryElement = ImprovedChat.newElem(ImprovedChat.topElement, "entry");
            entryElement.setAttribute("regex", pattern.pattern.toString());
            ImprovedChat.addTextNode(entryElement, pattern.replacement);
        }

        ImprovedChat.topElement = (Element)ImprovedChat.topElement.getParentNode();
        entryElement = ImprovedChat.newElem(ImprovedChat.topElement, "ChatBox");
        entryElement.setAttribute("Color", "" + ImprovedChat.getBgColor());
        entryElement.setAttribute("Opacity", "" + ImprovedChat.getBgOpacity());
        entryElement.setAttribute("ChatLinesBig", "" + ImprovedChat.getChatLinesBig());
        entryElement.setAttribute("ChatLinesSmall", "" + ImprovedChat.getChatLinesSmall());
        entryElement.setAttribute("ScrollLines", "" + ImprovedChat.getScrollLines());
        entryElement = ImprovedChat.newElem(ImprovedChat.topElement, "ChatHistory");
        entryElement.setAttribute("Color", "" + ImprovedChat.getHistoryColor());
        entryElement.setAttribute("Opacity", "" + ImprovedChat.getHistoryOpacity());
        entryElement.setAttribute("MaxLines", "" + ImprovedChat.getHistoryMaxLines());
        ImprovedChat.topElement = ImprovedChat.newElem(ImprovedChat.topElement, "Servers");
        Enumeration<String> serverNames = ImprovedChat.servers.keys();
        ImprovedChat.saveServer(ImprovedChat.globalServer);

        while (serverNames.hasMoreElements())
        {
            ImprovedChat.saveServer(ImprovedChat.servers.get(serverNames.nextElement()));
        }

        ImprovedChat.topElement = (Element)ImprovedChat.topElement.getParentNode();

        try
        {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
            transformer.setOutputProperty("standalone", "yes");
            DOMSource domSource = new DOMSource(ImprovedChat.doc);
            StreamResult result = new StreamResult(ImprovedChat.settings);
            transformer.transform(domSource, result);
        }
        catch (TransformerException ex)
        {
            ex.printStackTrace();
        }
    }

    private static void saveServer(Server server)
    {
        ImprovedChat.topElement = ImprovedChat.newElem(ImprovedChat.topElement, "Server");
        ImprovedChat.topElement.setAttribute("name", server.name);
        ImprovedChat.topElement.setAttribute("address", server.address);
        ImprovedChat.topElement.setAttribute("colorchat", server.colorchat.booleanValue() ? "true" : "false");
        ImprovedChat.topElement.setAttribute("herochat", server.heroChat.booleanValue() ? "true" : "false");

        if (server.heroChat.booleanValue() && server.ChatMode != null)
        {
            ImprovedChat.topElement.setAttribute("herochat_channel", server.ChatMode);
        }

        Element entryElement;

        for (int bindingType = 0; bindingType < 4; ++bindingType)
        {
            ImprovedChat.topElement = ImprovedChat.newElem(ImprovedChat.topElement, "Bindings");
            ImprovedChat.topElement.setAttribute("type", "" + bindingType);
            Enumeration<Integer> bindingTypes = server.bindings.get(bindingType).keys();

            while (bindingTypes.hasMoreElements())
            {
                Integer typeId = bindingTypes.nextElement();
                entryElement = ImprovedChat.newElem(ImprovedChat.topElement, "entry");
                entryElement.setAttribute("key", typeId.toString());
                ImprovedChat.addTextNode(entryElement, (server.bindings.get(bindingType)).get(typeId));
            }

            ImprovedChat.topElement = (Element)ImprovedChat.topElement.getParentNode();
        }

        ImprovedChat.topElement = ImprovedChat.newElem(ImprovedChat.topElement, "Variables");
        Enumeration<String> serverVars = server.vars.keys();

        while (serverVars.hasMoreElements())
        {
            String var = serverVars.nextElement();
            entryElement = ImprovedChat.newElem(ImprovedChat.topElement, "entry");
            entryElement.setAttribute("var", var);
            ImprovedChat.addTextNode(entryElement, server.vars.getPattern(var));
        }

        ImprovedChat.topElement = (Element)ImprovedChat.topElement.getParentNode();
        ImprovedChat.topElement = ImprovedChat.newElem(ImprovedChat.topElement, "Tabs");

        for (int tabId = 0; tabId < server.tabs.size(); ++tabId)
        {
            Tab tab;

            if ((tab = server.tabs.get(tabId)) != null)
            {
                ImprovedChat.topElement = ImprovedChat.newElem(ImprovedChat.topElement, "Tab");
                ImprovedChat.topElement.setAttribute("prefix", tab.prefix);
                ImprovedChat.addTextNode(ImprovedChat.newElem(ImprovedChat.topElement, "name"), tab.name);

                if (tab.blink)
                {
                    ImprovedChat.newElem(ImprovedChat.topElement, "blink");
                }

                Iterator<Pattern> trackEntries = tab.track.iterator();
                Pattern tracker;

                while (trackEntries.hasNext())
                {
                    tracker = trackEntries.next();
                    ImprovedChat.addTextNode(ImprovedChat.newElem(ImprovedChat.topElement, "track"), tracker.pattern());
                }

                trackEntries = tab.ignore.iterator();

                while (trackEntries.hasNext())
                {
                    tracker = trackEntries.next();
                    ImprovedChat.addTextNode(ImprovedChat.newElem(ImprovedChat.topElement, "ignore"), tracker.pattern());
                }

                ImprovedChat.topElement = (Element)ImprovedChat.topElement.getParentNode();
            }
        }

        ImprovedChat.topElement = (Element)ImprovedChat.topElement.getParentNode();

        if (server.translations != null)
        {
            entryElement = ImprovedChat.newElem(ImprovedChat.topElement, "Translation");
            entryElement.setAttribute("from", server.translations[0]);
            entryElement.setAttribute("to", server.translations[1]);
        }

        ImprovedChat.topElement = (Element)ImprovedChat.topElement.getParentNode();
    }

    static void load()
    {
        Document xml = null;

        if (ImprovedChat.settings.exists())
        {
            try
            {
                xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ImprovedChat.settings);
                Element root = xml.getDocumentElement();
                root.normalize();
                Node currentNode = root.getElementsByTagName("Input").item(0);
                Element currentElement;
                NodeList entries;
                int entryNumber;

                if (currentNode != null && currentNode.getNodeType() == 1)
                {
                    ImprovedChat.patternList[0].clear();
                    entries = ((Element)currentNode).getElementsByTagName("entry");

                    for (entryNumber = 0; entryNumber < entries.getLength(); ++entryNumber)
                    {
                        currentElement = (Element)entries.item(entryNumber);
                        ImprovedChat.patternList[0].add(currentElement.getAttribute("regex"), currentElement.getTextContent());
                    }
                }

                currentNode = root.getElementsByTagName("Output").item(0);

                if (currentNode != null && currentNode.getNodeType() == 1)
                {
                    ImprovedChat.patternList[1].clear();
                    entries = ((Element)currentNode).getElementsByTagName("entry");

                    for (entryNumber = 0; entryNumber < entries.getLength(); ++entryNumber)
                    {
                        currentElement = (Element)entries.item(entryNumber);
                        ImprovedChat.patternList[1].add(currentElement.getAttribute("regex"), currentElement.getTextContent());
                    }
                }

                currentNode = root.getElementsByTagName("Display").item(0);

                if (currentNode != null && currentNode.getNodeType() == 1)
                {
                    ImprovedChat.patternList[2].clear();
                    entries = ((Element)currentNode).getElementsByTagName("entry");

                    for (entryNumber = 0; entryNumber < entries.getLength(); ++entryNumber)
                    {
                        currentElement = (Element)entries.item(entryNumber);
                        ImprovedChat.patternList[2].add(currentElement.getAttribute("regex"), currentElement.getTextContent());
                    }
                }

                currentElement = (Element)root.getElementsByTagName("ChatBox").item(0);
                String setting;

                if (currentElement != null)
                {
                    setting = currentElement.getAttribute("Color");

                    if (setting != null)
                    {
                        try
                        {
                            ImprovedChat.bgColor = Integer.parseInt(setting);
                        }
                        catch (NumberFormatException ex) {}
                    }

                    setting = currentElement.getAttribute("ChatLinesSmall");

                    if (setting != null)
                    {
                        try
                        {
                            ImprovedChat.chatLinesSmall = Byte.parseByte(setting);
                        }
                        catch (NumberFormatException ex) {}
                    }

                    setting = currentElement.getAttribute("ChatLinesBig");

                    if (setting != null)
                    {
                        try
                        {
                            ImprovedChat.chatLinesBig = Byte.parseByte(setting);
                        }
                        catch (NumberFormatException ex) {}
                    }

                    setting = currentElement.getAttribute("ScrollLines");

                    if (setting != null)
                    {
                        try
                        {
                            ImprovedChat.scrollLines = Integer.parseInt(setting);
                        }
                        catch (NumberFormatException ex) {}
                    }

                    setting = currentElement.getAttribute("Opacity");

                    if (setting != null)
                    {
                        try
                        {
                            ImprovedChat.bgOpacity = Integer.parseInt(setting);
                        }
                        catch (NumberFormatException ex) {}
                    }

                    currentNode = root.getElementsByTagName("ServerTranslations").item(0);

                    if (currentNode != null && currentNode.getNodeType() == 1)
                    {
                        ImprovedChat.translations.clear();
                        entries = ((Element)currentNode).getElementsByTagName("server");

                        for (entryNumber = 0; entryNumber < entries.getLength(); ++entryNumber)
                        {
                            currentElement = (Element)entries.item(entryNumber);
                            String[] kv = new String[] {currentElement.getAttribute("from"), currentElement.getAttribute("to")};
                            ImprovedChat.translations.put(currentElement.getTextContent(), kv);
                        }
                    }
                }

                currentElement = (Element)root.getElementsByTagName("ChatHistory").item(0);

                if (currentElement != null)
                {
                    setting = currentElement.getAttribute("Color");

                    if (setting != null)
                    {
                        try
                        {
                            ImprovedChat.historyColor = Integer.parseInt(setting);
                        }
                        catch (NumberFormatException ex) {}
                    }

                    setting = currentElement.getAttribute("MaxLines");

                    if (setting != null)
                    {
                        try
                        {
                            ImprovedChat.historyMaxLines = Integer.parseInt(setting);
                        }
                        catch (NumberFormatException ex) {}
                    }

                    setting = currentElement.getAttribute("Opacity");

                    if (setting != null)
                    {
                        try
                        {
                            ImprovedChat.historyOpacity = Integer.parseInt(setting);
                        }
                        catch (NumberFormatException ex) {}
                    }
                }

                currentElement = (Element)root.getElementsByTagName("Servers").item(0);

                if (currentElement != null)
                {
                    entries = currentElement.getElementsByTagName("Server");

                    for (entryNumber = 0; entryNumber < entries.getLength(); ++entryNumber)
                    {
                        Element element = (Element)entries.item(entryNumber);
                        Server server = new Server(element.getAttribute("name"), element.getAttribute("address"));
                        String colorChat = element.getAttribute("colorchat");

                        if (colorChat != null)
                        {
                            server.colorchat = Boolean.valueOf(colorChat.equalsIgnoreCase("true"));
                        }

                        String heroChat = element.getAttribute("herochat");
                        server.heroChat = Boolean.valueOf(heroChat.equalsIgnoreCase("true"));

                        if (server.heroChat.booleanValue())
                        {
                            String hcChannel = element.getAttribute("herochat_channel");
                            server.ChatMode = hcChannel;
                        }

                        NodeList bindings = element.getElementsByTagName("Bindings");

                        for (int bindingId = 0; bindingId < bindings.getLength(); ++bindingId)
                        {
                            Element bindingElement = (Element)bindings.item(bindingId);
                            String bindingType = bindingElement.getAttribute("type");
                            int type = 0;

                            if (bindingType != null && !bindingType.equals(""))
                            {
                                type = Integer.parseInt(bindingType);
                            }

                            server.bindings.get(type).clear();
                            NodeList bindingEntries = bindingElement.getElementsByTagName("entry");

                            for (int e = 0; e < bindingEntries.getLength(); ++e)
                            {
                                currentElement = (Element)bindingEntries.item(e);
                                server.bindings.get(type).put(Integer.valueOf(Integer.parseInt(currentElement.getAttribute("key"))), currentElement.getTextContent());
                            }
                        }

                        currentNode = element.getElementsByTagName("Variables").item(0);
                        NodeList nodes;
                        int pos;

                        if (currentNode != null && currentNode.getNodeType() == 1)
                        {
                            nodes = ((Element)currentNode).getElementsByTagName("entry");
                            server.vars.clear();

                            for (pos = 0; pos < nodes.getLength(); ++pos)
                            {
                                currentElement = (Element)nodes.item(pos);
                                server.vars.add(currentElement.getAttribute("var"), currentElement.getTextContent());
                            }
                        }

                        currentNode = element.getElementsByTagName("Tabs").item(0);

                        if (currentNode != null && currentNode.getNodeType() == 1)
                        {
                            nodes = ((Element)currentNode).getElementsByTagName("Tab");

                            for (pos = 0; pos < nodes.getLength(); ++pos)
                            {
                                Element node = (Element)nodes.item(pos);
                                String tabName = node.getElementsByTagName("name").item(0).getTextContent();
                                Tab tab = new Tab(tabName);
                                server.tabs.add(tab);
                                tab.prefix = node.getAttribute("prefix");
                                tab.blink = node.getElementsByTagName("blink").item(0) != null;
                                NodeList tracks = node.getElementsByTagName("track");
                                int t;

                                for (t = 0; t < tracks.getLength(); ++t)
                                {
                                    tab.track(tracks.item(t).getTextContent());
                                }

                                tracks = node.getElementsByTagName("ignore");

                                for (t = 0; t < tracks.getLength(); ++t)
                                {
                                    tab.ignore(tracks.item(t).getTextContent());
                                }
                            }
                        }

                        if (server.name.equals("Global") && server.address.equals(""))
                        {
                            ImprovedChat.globalServer = server;
                        }
                        else
                        {
                            ImprovedChat.servers.put(server.name, server);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }

            if (ImprovedChat.constantsFile.exists())
            {
                try
                {
                    ImprovedChat.constantVar.clear();
                    BufferedReader reader = new BufferedReader(new FileReader(ImprovedChat.constantsFile));
                    String line;

                    while ((line = reader.readLine()) != null)
                    {
                        line = line.split("#", 2)[0];

                        if (!line.equals(""))
                        {
                            String[] tokens = line.split(" ", 2);

                            if (tokens.length == 2)
                            {
                                ImprovedChat.constantVar.put(tokens[0], tokens[1]);
                            }
                        }
                    }

                    reader.close();
                }
                catch (FileNotFoundException ex)
                {
                    ex.printStackTrace();
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static int getColorHex(String colour)
    {
        Integer value = ImprovedChat.colorHex.get(colour);
        return value == null ? -1 : value.intValue();
    }

    public static void listKB()
    {
        ImprovedChat.unProccessedInput("Global key bindings:");
        String[] modifiers = new String[] {"", "ctrl-", "shift-", "ctrl-shift-"};
        Enumeration<Integer> bindings;
        Integer binding;

        for (int i = 0; i < 4; ++i)
        {
            bindings = ImprovedChat.globalServer.bindings.get(i).keys();

            while (bindings.hasMoreElements())
            {
                binding = bindings.nextElement();
                ImprovedChat.unProccessedInput(modifiers[i] + Keyboard.getKeyName(binding.intValue()) + ": " + (ImprovedChat.globalServer.bindings.get(i)).get(binding));
            }
        }

        ImprovedChat.unProccessedInput("Server specific key bindings");

        for (int i = 0; i < 4; ++i)
        {
            bindings = ImprovedChat.currentServer.bindings.get(i).keys();

            while (bindings.hasMoreElements())
            {
                binding = bindings.nextElement();
                ImprovedChat.unProccessedInput(modifiers[i] + Keyboard.getKeyName(binding.intValue()) + ": " + (ImprovedChat.currentServer.bindings.get(i)).get(binding));
            }
        }
    }

    public static void addKB(String bindingName, String binding)
    {
        int modifiers = 0;
        bindingName = bindingName.toUpperCase();

        if (bindingName.startsWith("CTRL-"))
        {
            ++modifiers;
            bindingName = bindingName.substring(5);
        }

        if (bindingName.startsWith("SHIFT-"))
        {
            modifiers += 2;
            bindingName = bindingName.substring(6);
        }

        int keyId = Keyboard.getKeyIndex(bindingName);

        if (keyId == 0)
        {
            ImprovedChat.stderr("There is no key with name " + bindingName);
        }
        else
        {
            ImprovedChat.stdout("Adding binding to " + ImprovedChat.getCurrentServer().name);
            ImprovedChat.getCurrentServer().bindings.get(modifiers).put(new Integer(keyId), binding);
        }
    }

    public static boolean removeKB(String bindingName)
    {
        int modifiers = 0;
        bindingName = bindingName.toUpperCase();

        if (bindingName.startsWith("CTRL-"))
        {
            ++modifiers;
            bindingName = bindingName.substring(5);
        }

        if (bindingName.startsWith("SHIFT-"))
        {
            modifiers += 2;
            bindingName = bindingName.substring(6);
        }

        int keyId = Keyboard.getKeyIndex(bindingName);
        String oldBinding = (ImprovedChat.getCurrentServer().bindings.get(modifiers)).remove(new Integer(keyId));
        return oldBinding != null;
    }

    public static void listVars()
    {
        ImprovedChat.unProccessedInput("Global variables");
        Enumeration<String> varNames = ImprovedChat.globalServer.vars.keys();
        String varName;

        while (varNames.hasMoreElements())
        {
            varName = varNames.nextElement();
            ImprovedChat.unProccessedInput(varName + ":" + ImprovedChat.globalServer.vars.getPattern(varName));
        }

        ImprovedChat.unProccessedInput("Server specific variables");
        varNames = ImprovedChat.getCurrentServer().vars.keys();

        while (varNames.hasMoreElements())
        {
            varName = varNames.nextElement();
            ImprovedChat.unProccessedInput(varName + ":" + ImprovedChat.getCurrentServer().vars.getPattern(varName));
        }
    }

    public static String getVar(String varName)
    {
        if (varName.equals("time"))
        {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            return dateFormat.format(cal.getTime());
        }
		Variable variable = ImprovedChat.getCurrentServer().vars.get(varName);

		if (variable == null)
		{
		    variable = ImprovedChat.globalServer.vars.get(varName);
		}

		String value;

		if (variable == null)
		{
		    value = ImprovedChat.constantVar.get(varName);
		}
		else
		{
		    value = variable.value;
		}

		return value;
    }

    public static void removeVar(String varName)
    {
        ImprovedChat.getCurrentServer().vars.remove(varName);
    }

    public static void varProcess(String varName)
    {
        ImprovedChat.globalServer.vars.process(varName);
        ImprovedChat.getCurrentServer().vars.process(varName);
    }

    public static void addRule(String type, String from, String to)
    {
        if (type.equalsIgnoreCase("input"))
        {
            ImprovedChat.patternList[0].add(from, to);
        }
        else if (type.equalsIgnoreCase("output"))
        {
            ImprovedChat.patternList[1].add(from, to);
        }
        else if (type.equalsIgnoreCase("display"))
        {
            ImprovedChat.patternList[2].add(from, to);
        }
    }

    public static void removeRule(String type, int index)
    {
        List<Entry> rules = null;

        if (type.equalsIgnoreCase("input"))
        {
            rules = ImprovedChat.patternList[0].list;
        }
        else if (type.equalsIgnoreCase("output"))
        {
            rules = ImprovedChat.patternList[1].list;
        }
        else if (type.equalsIgnoreCase("display"))
        {
            rules = ImprovedChat.patternList[2].list;
        }

        if (rules == null)
        {
            ImprovedChat.console("\2476Usage\247f: \247e~delete bind <KeyName>");
            ImprovedChat.console("\2476Usage\247f: \247e~delete var <VarName>");
            ImprovedChat.console("\2476Usage\247f: \247e~delete (input|output|display) id");
        }
        else if (index >= 0 && index < rules.size())
        {
            rules.remove(index);
            ImprovedChat.console("\247aDeleted rule successful");
        }
        else
        {
            ImprovedChat.console("\247cDelete\247f: \247cindex out of bounds.");
        }
    }

    public static boolean moveRule(String type, int fromIndex, int toIndex)
    {
        if (type.equalsIgnoreCase("input"))
        {
            ImprovedChat.console(ImprovedChat.patternList[0].move(fromIndex, toIndex));
        }
        else if (type.equalsIgnoreCase("output"))
        {
            ImprovedChat.console(ImprovedChat.patternList[1].move(fromIndex, toIndex));
        }
        else
        {
            if (!type.equalsIgnoreCase("display"))
            {
                return false;
            }

            ImprovedChat.console(ImprovedChat.patternList[2].move(fromIndex, toIndex));
        }

        return true;
    }

    public static boolean list(String type)
    {
        if (type.equals("input"))
        {
            ImprovedChat.patternList[0].list();
        }
        else if (type.equals("output"))
        {
            ImprovedChat.patternList[1].list();
        }
        else
        {
            if (!type.equals("display"))
            {
                return false;
            }

            ImprovedChat.patternList[2].list();
        }

        return true;
    }

    public static Character getLastColor(String text)
    {
        Character lastColourChar = null;

        for (int pos = 0; pos < text.length(); ++pos)
        {
            char charAt = text.charAt(pos);

            if (charAt == 167 && text.length() > pos)
            {
                lastColourChar = Character.valueOf(text.charAt(pos + 1));
            }
        }

        return lastColourChar;
    }

    @SuppressWarnings("unchecked")
	public static List<String> processInput(String text)
    {
        text = ImprovedChat.buxvillFix.matcher(text).replaceAll("/&c");
        text = ImprovedChat.updateColor.matcher(ImprovedChat.patternList[0].process(text)).replaceAll("\u00a7");
        return ImprovedChat.getFontRenderer().listFormattedStringToWidth(text, 320);
    }

    public static int getStringWidth(String text)
    {
        return ImprovedChat.getFontRenderer().getStringWidth(text);
    }

    public static FontRenderer getFontRenderer()
    {
        return ImprovedChat.minecraft.fontRenderer;
    }

    public static String processOutput(String text)
    {
        text = ImprovedChat.patternList[1].process(text);
        text = ImprovedChat.replaceVars(text);

        if (ImprovedChat.getCurrentServer().colorchat.booleanValue())
        {
            text = ImprovedChat.updateColor.matcher(ImprovedChat.patternList[1].process(text)).replaceAll("\u00a7");
        }
        else
        {
            ImprovedChat.colorTags.matcher(text).replaceAll("");
        }

        return text;
    }

    public static int drawChatHistory(GuiImprovedChatNewChat chatGui, FontRenderer fontRenderer, boolean chatOpen, byte visibleLineCount, int updateCounter)
    {
        int chatLineCount = 0;
        int alpha = (int)((ImprovedChat.getGameSettings().chatOpacity * 0.9F + 0.1F) * 255.0F);

        if (ImprovedChat.currentServer != null)
        {
            int tabLeftPos;
            int tabIndex;

            if (ImprovedChat.getCurrentServer().tabs.size() > 1)
            {
                tabLeftPos = -312;
                int leftPos = 0;

                for (tabIndex = 0; tabIndex <= ImprovedChat.getCurrentServer().currentTabIndex; ++tabIndex)
                {
                    tabLeftPos += 4 + (ImprovedChat.getCurrentServer().tabs.get(tabIndex)).width;
                }

                if (tabLeftPos < 0)
                {
                    tabLeftPos = 0;
                }

                for (tabIndex = 0; tabIndex < ImprovedChat.getCurrentServer().tabs.size(); ++tabIndex)
                {
                    Tab currentTab = ImprovedChat.getCurrentServer().tabs.get(tabIndex);

                    if (leftPos + 4 + currentTab.width > 312)
                    {
                        break;
                    }

                    if (leftPos >= tabLeftPos)
                    {
                        if (ImprovedChat.getCurrentServer().currentTabIndex == tabIndex)
                        {
                            if (chatOpen)
                            {
                                ImprovedChat.drawStringWithShadow(fontRenderer, currentTab.name, 2 + leftPos, -180, 16777215);
                            }
                            else
                            {
                                if (currentTab.blinking)
                                {
                                    ImprovedChat.fade = 64;
                                    currentTab.blinking = false;
                                }

                                if (ImprovedChat.fade > 0)
                                {
                                    ImprovedChat.drawStringWithShadow(fontRenderer, currentTab.name, 2 + leftPos, -180, (ImprovedChat.fade << 25) + 16777215);
                                    --ImprovedChat.fade;
                                }
                            }
                        }
                        else if (currentTab.blinking)
                        {
                            int blinkAlpha = ImprovedChat.minecraft.ingameGUI.getUpdateCounter() % 40 - 20;

                            if (blinkAlpha < 0)
                            {
                                blinkAlpha = -blinkAlpha;
                            }

                            blinkAlpha = (int)(blinkAlpha * 12.75D);

                            if (blinkAlpha == 0)
                            {
                                ++blinkAlpha;
                            }

                            ImprovedChat.drawStringWithShadow(fontRenderer, currentTab.name, 2 + leftPos, -180, (blinkAlpha << 24) + 16777215);
                        }
                        else if (chatOpen)
                        {
                            ImprovedChat.drawStringWithShadow(fontRenderer, currentTab.name, 2 + leftPos, -180, 2130706432);
                        }

                        leftPos += 4 + currentTab.width;
                    }
                    else
                    {
                        tabLeftPos -= 4 + currentTab.width;
                    }
                }
            }

            String text = "";

            for (tabLeftPos = ImprovedChat.currentTab().chatScroll; tabLeftPos < ImprovedChat.currentTab().chatLines.size() && tabLeftPos < visibleLineCount + ImprovedChat.currentTab().chatScroll; ++tabLeftPos)
            {
                tabIndex = updateCounter - ImprovedChat.getUpdateCounterOfChatLine(ImprovedChat.currentTab().chatLines.get(tabLeftPos));

                if (tabIndex < 200 || chatOpen)
                {
                    double tabAlpha = tabIndex / 200.0D;
                    tabAlpha = 1.0D - tabAlpha;
                    tabAlpha *= 10.0D;

                    if (tabAlpha < 0.0D)
                        tabAlpha = 0.0D;

                    if (tabAlpha > 1.0D)
                        tabAlpha = 1.0D;

                    tabAlpha *= tabAlpha;
                    int textAlpha = chatOpen ? alpha : (int)(alpha * tabAlpha);
                    int historyAlpha = ImprovedChat.getHistoryOpacity();
                    ++chatLineCount;

                    if (textAlpha > 4)
                    {
                        byte left = 2;
                        int top = (-tabLeftPos + ImprovedChat.currentTab().chatScroll) * 9;
                        text = ImprovedChat.getChatMessageOfChatLine(ImprovedChat.currentTab().chatLines.get(tabLeftPos));

                        if (!ImprovedChat.getGameSettings().chatColours)
                        {
                            text = StringUtils.stripControlCodes(text);
                        }

                        ImprovedChat.drawRectOnGuiIngame(chatGui, left, top - 1, left + 320, top + 8, ImprovedChat.addAlpha(ImprovedChat.getHistoryColor(), historyAlpha));
                        glEnable(GL_BLEND);
                        ImprovedChat.drawStringWithShadow(fontRenderer, text, left, top, (textAlpha << 24) + 16777215);
                    }
                }
            }
        }

        return chatLineCount;
    }

    public static int gethex(int r, int g, int b, int alpha)
    {
        return alpha << 24 | r << 16 | g << 8 | b;
    }

    public static int addAlpha(int colour, int alpha)
    {
        int r = colour >> 16 & 255;
        int g = colour >> 8 & 255;
        int b = colour & 255;
        return ImprovedChat.gethex(r, g, b, alpha);
    }

    public static void drawRectOnGuiIngame(GuiImprovedChatNewChat gui, int x1, int y1, int x2, int y2, int colour)
    {
    	Gui.drawRect(x1, y1, x2, y2, colour);
    }

    public static String getChatMessageOfChatLine(ImprovedChatLine line)
    {
        return line.func_151461_a().getFormattedText();
    }

    public static EntityClientPlayerMP getPlayer()
    {
        return ImprovedChat.minecraft.thePlayer;
    }

    public static void sendChatMessage(String text)
    {
        ImprovedChat.getPlayer().sendChatMessage(text);
    }

    public static GuiScreen getCurrentScreen()
    {
        return ImprovedChat.minecraft.currentScreen;
    }

    @SuppressWarnings("unchecked")
	public void setSeed(long seed)
    {
        long worldSeed = getWorld().getSeed();

        try
        {
            Class<WorldInfo> worldInfoClass = (Class<WorldInfo>)getWorld().getClass();
            Field seedField = worldInfoClass.getDeclaredField("a");
            seedField.setAccessible(true);
            ImprovedChat.stdout("Original (obfuscated?) seed : " + worldSeed);
            seedField.set(getWorld(), Long.valueOf(seed));
            ImprovedChat.stdout("After seed : " + getWorld().getSeed());

            if (Class.forName("reifnsk.minimap.ChunkData") != null)
            {
                ImprovedChat.stdout("Rei\'s minimap found, setting rei\'s seed!");
                Class<?> chunkDataClass = Class.forName("reifnsk.minimap.ChunkData");
                Class<?> miniMapClass = Class.forName("reifnsk.minimap.ReiMinimap");
                Field miniMapInstanceField = miniMapClass.getDeclaredField("instance");
                miniMapInstanceField.setAccessible(true);
                Object miniMapInstance = miniMapInstanceField.get((Object)null);
                Field chunkDataSeedField = chunkDataClass.getDeclaredField("seed");
                Field preLoadedChunksField = miniMapClass.getDeclaredField("preloadedChunks");
                Field slimeFiled = chunkDataClass.getDeclaredField("slime");
                slimeFiled.setAccessible(true);
                preLoadedChunksField.setAccessible(true);
                chunkDataSeedField.setAccessible(true);
                chunkDataSeedField.set((Object)null, Long.valueOf(seed));
                Method mCreateChunkData = chunkDataClass.getDeclaredMethod("createChunkData", new Class[] {Integer.TYPE, Integer.TYPE});
                Method mUpdateChunk = chunkDataClass.getDeclaredMethod("updateChunk", new Class[] {Boolean.TYPE});
                mCreateChunkData.setAccessible(true);
                mUpdateChunk.setAccessible(true);
                boolean isPreLoadChunks = preLoadedChunksField.getBoolean(miniMapInstance);

                for (int chunkOffsetZ = -8; chunkOffsetZ <= 8; ++chunkOffsetZ)
                {
                    for (int chunkOffsetX = -8; chunkOffsetX <= 8; ++chunkOffsetX)
                    {
                        int chunkCoordX = ImprovedChat.getPlayer().chunkCoordX + chunkOffsetX;
                        int chunkCoordZ = ImprovedChat.getPlayer().chunkCoordZ + chunkOffsetZ;
                        Object chunkData = mCreateChunkData.invoke((Object)null, new Object[] {Integer.valueOf(chunkCoordX), Integer.valueOf(chunkCoordZ)});

                        if (chunkData != null)
                        {
                            mUpdateChunk.invoke(chunkData, new Object[] {Boolean.valueOf(isPreLoadChunks)});
                            slimeFiled.set(chunkData, Boolean.valueOf((new Random(seed + (chunkCoordX * chunkCoordX * 4987142) + (chunkCoordX * 5947611) + (chunkCoordZ * chunkCoordZ) * 4392871L + (chunkCoordZ * 389711) ^ 987234911L)).nextInt(10) == 0));
                        }
                    }
                }
            }
        }
        catch (NoSuchFieldException ex)
        {
            ex.printStackTrace();
        }
        catch (IllegalAccessException ex)
        {
            ex.printStackTrace();
        }
        catch (ClassNotFoundException ex)
        {
        }
        catch (NoSuchMethodException ex)
        {
            ex.printStackTrace();
        }
        catch (InvocationTargetException ex)
        {
            ex.printStackTrace();
        }
        catch (IllegalArgumentException ex)
        {
            ex.printStackTrace();
        }
    }

    public static GameSettings getGameSettings()
    {
        return ImprovedChat.minecraft.gameSettings;
    }

    public static void setCurrent(String address)
    {
        if (address.endsWith("_25565"))
        {
            address = address.substring(0, address.length() - "_25565".length());
        }

        Server server = ImprovedChat.servers.get(address);

        if (server == null)
        {
            server = new Server(address);
            ImprovedChat.servers.put(address, server);
        }

        ImprovedChat.setCurrentServer(server);
    }

    public static String getLastServer()
    {
        return ImprovedChat.getGameSettings().lastServer;
    }

    public static void setLastServer(Server server)
    {
        ImprovedChat.getGameSettings().lastServer = server.name;
    }

    public static int getUpdateCounterOfChatLine(ImprovedChatLine line)
    {
        return line == null ? 0 : line.getUpdatedCounter();
    }

    public static void drawStringWithShadow(FontRenderer fontRenderer, String text, int xPos, int yPos, int colour)
    {
        fontRenderer.drawStringWithShadow(text, xPos, yPos, colour);
    }

    public static void drawString(FontRenderer fontRenderer, String text, int xPos, int yPos, int colour)
    {
        fontRenderer.drawString(text, xPos, yPos, colour);
    }

    public static void displayGuiScreen(ScreenChatOptions screen)
    {
        ImprovedChat.minecraft.displayGuiScreen(screen);
    }

    public static char[] getAllowedCharacters()
    {
        return ChatAllowedCharacters.allowedCharacters;
    }

    public static Server getCurrentServer()
    {
        return ImprovedChat.currentServer;
    }

    private static void setCurrentServer(Server server)
    {
        ImprovedChat.currentServer = server;
        ImprovedChat.setLastServer(server);
    }

    @SuppressWarnings("unchecked")
	public static List<String> processDisplay(String text)
    {
        text = ImprovedChat.updateColor.matcher(ImprovedChat.patternList[2].process(text)).replaceAll("\u00a7");
        return ImprovedChat.getFontRenderer().listFormattedStringToWidth(text, 316);
    }

    public static void keyPressed(int keyCode)
    {
        int modifierSetIndex = 0;

        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
        {
            ++modifierSetIndex;
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
        {
            modifierSetIndex += 2;
        }

        String binding = ImprovedChat.getCurrentServer().bindings.get(modifierSetIndex).get(Integer.valueOf(keyCode));

        if (binding == null)
        {
            binding = ImprovedChat.globalServer.bindings.get(modifierSetIndex).get(Integer.valueOf(keyCode));
        }

        if (binding != null)
        {
            if (binding.endsWith("\\"))
            {
                ImprovedChat.minecraft.displayGuiScreen(new GuiImprovedChat(ImprovedChat.replaceVarsInBind(binding.substring(0, binding.length() - 1))));
            }
            else
            {
                ImprovedChat.process(binding);
            }
        }
    }

    public static String replaceVars(String text)
    {
        Matcher varPatternMatcher = ImprovedChat.varP.matcher(text);
        StringBuilder sb = new StringBuilder();
        int endPos = 0;

        while (varPatternMatcher.find())
        {
            String varValue = ImprovedChat.getVar(text.substring(varPatternMatcher.start() + 1, varPatternMatcher.end()));

            if (varValue != null)
            {
                sb.append(text.substring(endPos, varPatternMatcher.start()));
                endPos = varPatternMatcher.end();
                sb.append(varValue);
            }
        }

        sb.append(text.substring(endPos));
        return sb.toString();
    }

    public static String replaceVarsInBind(String text)
    {
        Matcher varPatternMatcher = ImprovedChat.varPinB.matcher(text);
        StringBuilder sb = new StringBuilder();
        int endPos = 0;

        while (varPatternMatcher.find())
        {
            String varValue = ImprovedChat.getVar(text.substring(varPatternMatcher.start() + 2, varPatternMatcher.end() - 1));

            if (varValue != null)
            {
                sb.append(text.substring(endPos, varPatternMatcher.start()));
                endPos = varPatternMatcher.end();
                sb.append(varValue);
            }
        }

        sb.append(text.substring(endPos));
        return sb.toString();
    }

    public static String fixInvalidCharacter(String text)
    {
        return ImprovedChat.fixInvalidCharacter.matcher(text).replaceAll("&c");
    }

    public static void listConstants()
    {
        Enumeration<String> constants = ImprovedChat.constantVar.keys();

        while (constants.hasMoreElements())
        {
            String constant = constants.nextElement();
            ImprovedChat.unProccessedInput(constant + ":" + ImprovedChat.constantVar.get(constant));
        }
    }

    public static int getMaxChatPacketLength()
    {
        return 100;
    }

    public static void send(String text)
    {
        text = text.trim();

        if (text.length() != 0)
        {
            for (text = ImprovedChat.processOutput(text); text.length() > ImprovedChat.getMaxChatPacketLength(); text = text.substring(ImprovedChat.getMaxChatPacketLength()))
            {
                ImprovedChat.sendChatMessage(text.substring(0, ImprovedChat.getMaxChatPacketLength()));
            }

            ImprovedChat.sendChatMessage(text);
        }
    }

    public static void console(String text)
    {
        if (text != null && !text.trim().equals(""))
        {
            List<String> inputList = ImprovedChat.processInput(text);
            String cleaned = ImprovedChat.colorTags.matcher("~" + text).replaceAll("");

            if (!ImprovedChat.getCurrentServer().currentTab().ignored(cleaned))
            {
                Iterator<String> inputs = inputList.iterator();

                while (inputs.hasNext())
                {
                    String input = inputs.next();
                    ImprovedChat.getCurrentServer().currentTab().add(input);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
	public static void unProccessedInput(String text)
    {
        if (text != null && !text.trim().equals(""))
        {
            String cleaned = ImprovedChat.colorTags.matcher("~" + text).replaceAll("");

            if (!ImprovedChat.getCurrentServer().currentTab().ignored(cleaned))
            {
                List<String> lines = ImprovedChat.getFontRenderer().listFormattedStringToWidth(text, 320);
                Iterator<String> lineIter = lines.iterator();

                while (lineIter.hasNext())
                {
                    String line = lineIter.next();
                    ImprovedChat.getCurrentServer().currentTab().add(line);
                }
            }
        }
    }

    public static void stdout(String text)
    {
        ImprovedChat.stdout(text, ImprovedChat.minecraft.ingameGUI.getUpdateCounter(), 0);
    }

    public static void stdout(String text, int updateCounter, int id)
    {
        if (text != null && !text.trim().equals(""))
        {
            List<String> processedLines = ImprovedChat.processInput(text);
            ImprovedChatLine[] lines = new ImprovedChatLine[processedLines.size()];

            for (int lineNumber = 0; lineNumber < lines.length; ++lineNumber)
            {
                lines[lineNumber] = new ImprovedChatLine(updateCounter, new ChatComponentText(processedLines.get(lineNumber)), id);
                
                GuiNewChat chatGUI = ImprovedChat.minecraft.ingameGUI.getChatGUI();
                
                if (chatGUI instanceof GuiImprovedChatNewChat)
                {
                	((GuiImprovedChatNewChat)chatGUI).addImpChatLine(lines[lineNumber]);
                }
            }

            String cleaned = ImprovedChat.colorTags.matcher(text).replaceAll("");
            Iterator<Tab> tabs = ImprovedChat.getCurrentServer().tabs.iterator();
            Tab tab;

            while (tabs.hasNext())
            {
                tab = tabs.next();

                if (tab.valid(cleaned))
                {
                    ImprovedChatLine[] linesCopy = lines;
                    int lineCount = lines.length;

                    for (int lineNumber = 0; lineNumber < lineCount; ++lineNumber)
                    {
                        ImprovedChatLine line = linesCopy[lineNumber];
                        tab.add(line);
                    }

                    List<ImprovedChatLine> tabLines = tab.chatLines;

                    while (tabLines.size() > ImprovedChat.getHistoryMaxLines())
                    {
                        tabLines.remove(tabLines.size() - 1);
                    }
                }
            }

            tab = ImprovedChat.getCurrentServer().tabs.get(ImprovedChat.getCurrentServer().currentTabIndex);
            tab.blinking = false;

            if (ImprovedChat.minecraft.currentScreen instanceof ScreenChatOptions && tab.chatScroll > 0 && tab.valid(cleaned))
            {
                tab.chatScroll += processedLines.size();

                if (tab.chatScroll > tab.chatLines.size() - 9)
                {
                    tab.chatScroll = tab.chatLines.size() - 9;
                }

                if (tab.chatScroll < 0)
                {
                    tab.chatScroll = 0;
                }
            }
        }
    }

    public static void stderr(String text)
    {
        if (text != null && !text.trim().equals(""))
        {
            ImprovedChat.console("\247cERROR\247f: " + text);
        }
    }

    static String unsplit(String[] tokens, int from)
    {
        StringBuilder sb = new StringBuilder();

        if (from >= tokens.length)
        {
            return "";
        }
		sb.append(tokens[from]);

		for (int tokenId = from + 1; tokenId < tokens.length; ++tokenId)
		{
		    sb.append(" ").append(tokens[tokenId]);
		}

		return sb.toString();
    }

    static String unsplit(String[] tokens, int from, int to)
    {
        StringBuilder sb = new StringBuilder();

        if (from >= tokens.length)
        {
            return "";
        }
        
		sb.append(tokens[from]);

		for (int tokenId = from + 1; tokenId < tokens.length && tokenId < to; ++tokenId)
		{
		    sb.append(" ").append(tokens[tokenId]);
		}

		return sb.toString();
    }

    public ImprovedChat()
    {
        ImprovedChat.modDir = new File(LiteLoader.getModsFolder(), "wd1966");
        ImprovedChat.settings = new File(ImprovedChat.getModDir(), "ImprovedChat.xml");
        ImprovedChat.constantsFile = new File(ImprovedChat.getModDir(), "constants.txt");
        ImprovedChat.colors = new File(ImprovedChat.getModDir(), "colors.txt");
        
        if (!ImprovedChat.getModDir().exists())
        	ImprovedChat.getModDir().mkdirs();

//        try
//        {
//            if (Class.forName("dzHooksManager") != null)
//            {
//                new dzHook(this);
//                dzHookEnabled = true;
//            }
//        }
//        catch (ClassNotFoundException ex) {}

        System.out.println("[ImprovedChat] Loading ImprovedChat...");
//        System.out.println("[ImprovedChat] dzHooksManager support ? " + (dzHookEnabled ? "yes" : "no"));

        if (!ImprovedChat.colors.exists())
        {
            ImprovedChat.createColorFile();
        }

        ImprovedChat.patternList = new PatternList[3];
        ImprovedChat.patternList[0] = new PatternList();
        ImprovedChat.patternList[1] = new PatternList();
        ImprovedChat.patternList[2] = new PatternList();
        ImprovedChat.constantVar = new Hashtable<String, String>();

        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(ImprovedChat.colors));
            String line;

            while ((line = reader.readLine()) != null)
            {
                line = line.split("#", 2)[0].toLowerCase();
                String[] parts = line.split(":", 2);

                try
                {
                    int value;

                    if (parts[1].startsWith("0x"))
                    {
                        value = Integer.parseInt(parts[1].substring(2), 16);
                    }
                    else
                    {
                        value = Integer.parseInt(parts[1]);
                    }

                    ImprovedChat.colorHex.put(parts[0], Integer.valueOf(value));
                }
                catch (NumberFormatException ex) {}
            }

            reader.close();
        }
        catch (FileNotFoundException ex)
        {
            ex.printStackTrace();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

        ImprovedChat.commands = new Hashtable<String, icCommand>();
        ImprovedChat.commands.put("bind", new icCommand("binds a command to a key.", "bind <keyName> <command>", "Key binding succesfull")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 2)
		        {
		            ImprovedChat.addKB(args[0], ImprovedChat.unsplit(args, 1));
		            return true;
		        }
		        
				return false;
			}
		});
        
        ImprovedChat.commands.put("bgcolor", new icCommand("changes the backround color of the chat box.", "~bgColor <colorName>", "Background color of the chat bar successfully changed")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 1)
		        {
		            int colour = ImprovedChat.getColorHex(args[0].toLowerCase());

		            if (colour == -1)
		            {
		                ImprovedChat.bgColor = 0;
		                ImprovedChat.stderr("Color " + args[0] + " not defined,");
		            }
		            else
		            {
		                ImprovedChat.bgColor = colour;
		            }

		            return true;
		        }
		        
				return false;
			}
		});
        
        ImprovedChat.commands.put("bgopacity", new icCommand("changes the opacity of the chat box.", "~bgOpacity <num>", "Opacity of the chat bar successfully changed")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 1)
		        {
		            if (!ImprovedChat.isNumeric(args[0]))
		                return false;
		            
					int opacity = Integer.parseInt(args[0]);

					if (opacity >= 0 && opacity <= 100)
					{
					    ImprovedChat.bgOpacity = (int)(opacity * 2.55D);
					}
					else
					{
					    ImprovedChat.stderr("Opacity must be between 0 and 100.");
					}

					return true;
		        }
		        
				return false;
			}
		});

        ImprovedChat.commands.put("histcolor", new icCommand("changes the background color of the chat history.", "~histColor <colorName>", "Background color of the chat history successfully changed")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 1)
		        {
		            int colour = ImprovedChat.getColorHex(args[0].toLowerCase());

		            if (colour == -1)
		            {
		                ImprovedChat.historyColor = 0;
		                ImprovedChat.stderr("Color " + args[0] + " not defined,");
		            }
		            else
		            {
		                ImprovedChat.historyColor = colour;
		            }

		            return true;
		        }
		        
				return false;
			}
		});
        
        ImprovedChat.commands.put("histscroll", new icCommand("changes the amount of lines you scroll per scroll.", "~histscroll <number>", "Number of lines successfully changed")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 1)
		        {
		            if (!ImprovedChat.isNumeric(args[0]))
		                return false;
		            
					ImprovedChat.scrollLines = Integer.parseInt(args[0]);
					return true;
		        }
		        
				return false;
			}
		});
        
        ImprovedChat.commands.put("histlines", new icCommand("changes the amount of lines you see in the history.", "~histlines <number>", "Number of lines successfully changed")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 1)
		        {
		            if (!ImprovedChat.isNumeric(args[0]))
		                return false;
		            
					ImprovedChat.historyMaxLines = Integer.parseInt(args[0]);
					return true;
		        }
		        
				return false;
			}
		});
        
        ImprovedChat.commands.put("histopacity", new icCommand("changes the opacity of the chat history.", "~histOpacity <num>", "Opacity of the chat history successfully changed")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 1)
		        {
		            if (!ImprovedChat.isNumeric(args[0]))
		                return false;
		            
					int opacity = Integer.parseInt(args[0]);

					if (opacity >= 0 && opacity <= 100)
					{
					    ImprovedChat.historyOpacity = (int)(opacity * 2.55D);
					}
					else
					{
					    ImprovedChat.stderr("Opacity must be between 0 and 100.");
					}

					return true;
		        }
		        
				return false;
			}
		});
        
        ImprovedChat.commands.put("stop", new icCommand("Stops the chatting", null, "")
		{
			@Override
			public boolean process(String[] args)
			{
		        ImprovedChat.setChatDisabled(true);
		        return true;
			}
		});
        
        ImprovedChat.commands.put("colorchat", new icCommand("Sets the colorchat setting for this server", null, "")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length > 0)
		        {
		            ImprovedChat.getCurrentServer().colorchat = Boolean.valueOf(args[0].equalsIgnoreCase("true"));
		        }
		        else
		        {
		            ImprovedChat.stdout("Colorchat for this server is : " + (ImprovedChat.getCurrentServer().colorchat.booleanValue() ? "ENABLED" : "DISABLED"));
		        }

		        return true;
			}
		});
        
        ImprovedChat.commands.put("chatlines", new icCommand("Sets the amount of lines you see", null, "")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length > 0)
		        {
		            if (args[0].equalsIgnoreCase("small"))
		            {
		                try
		                {
		                    ImprovedChat.chatLinesSmall = Byte.parseByte(args[1]);
		                }
		                catch (NumberFormatException ex)
		                {
		                    ImprovedChat.stderr("Caught exception!");
		                }
		            }

		            if (args[0].equalsIgnoreCase("big"))
		            {
		                try
		                {
		                    ImprovedChat.chatLinesBig = Byte.parseByte(args[1]);
		                }
		                catch (NumberFormatException ex)
		                {
		                    ImprovedChat.stderr("Caught exception!");
		                }
		            }
		        }
		        else
		        {
		            ImprovedChat.stdout("Small lines : " + ImprovedChat.getChatLinesSmall() + " Big lines : " + ImprovedChat.getChatLinesBig());
		        }

		        return true;
			}
		});
        
        ImprovedChat.commands.put("start", new icCommand("Starts the chatting", null, "")
		{
			@Override
			public boolean process(String[] args)
			{
		        ImprovedChat.setChatDisabled(false);
		        return true;
			}
		});
        
        ImprovedChat.commands.put("help", new icCommand("Displays this message", null, "")
		{
			@Override
			public boolean process(String[] args)
			{
		        for (String command : ImprovedChat.commands.keySet())
		        {
		            ImprovedChat.console("\2476" + command + "\247f: \247e" + ImprovedChat.commands.get(command).desc);
		        }

		        return true;
			}
		});
        
        ImprovedChat.commands.put("list", new icCommand("Lists variables, constants and rules(input, output or display)", "~list (bind|var|const|input|output|display|track|ignore)", "")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 1)
		        {
		            if (args[0].equalsIgnoreCase("bind"))
		            {
		                ImprovedChat.listKB();
		            }
		            else if (args[0].equalsIgnoreCase("var"))
		            {
		                ImprovedChat.listVars();
		            }
		            else if (args[0].equalsIgnoreCase("const"))
		            {
		                ImprovedChat.listConstants();
		            }
		            else
		            {
		                if (!args[0].equalsIgnoreCase("track") && !args[0].equalsIgnoreCase("ignore"))
		                {
		                    return ImprovedChat.list(args[0].toLowerCase());
		                }

		                ImprovedChat.unProccessedInput(args[0] + " rule list:");
		                ArrayList<Pattern> patterns;

		                if (args[0].equals("ignore"))
		                {
		                    patterns = (ImprovedChat.getCurrentServer().tabs.get(ImprovedChat.getCurrentServer().currentTabIndex)).ignore;
		                }
		                else
		                {
		                    patterns = (ImprovedChat.getCurrentServer().tabs.get(ImprovedChat.getCurrentServer().currentTabIndex)).track;
		                }

		                if (patterns.size() == 0)
		                {
		                    ImprovedChat.unProccessedInput("Empty");
		                }

		                Iterator<Pattern> patternsIterator = patterns.iterator();

		                while (patternsIterator.hasNext())
		                {
		                    Pattern pattern = patternsIterator.next();
		                    ImprovedChat.unProccessedInput(pattern.toString());
		                }
		            }

		            return true;
		        }
		        
				return false;
			}
		});
        
        
        ImprovedChat.commands.put("move", new icCommand("Changes rules priority or moves tab position", "~move (input|output|display|tab) <from> <to>", "")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 3)
		        {
		            if (ImprovedChat.isNumeric(args[1]))
		            {
		                int from = Integer.parseInt(args[1]);

		                if (ImprovedChat.isNumeric(args[2]))
		                {
		                    int to = Integer.parseInt(args[2]);

		                    if (args[0].equalsIgnoreCase("tab"))
		                    {
		                        if (from >= 0 && to >= 0 && from < ImprovedChat.getCurrentServer().tabs.size() && to < ImprovedChat.getCurrentServer().tabs.size())
		                        {
		                            ImprovedChat.getCurrentServer().tabs.add(to, ImprovedChat.getCurrentServer().tabs.remove(from));
		                            return true;
		                        }
								ImprovedChat.stderr("Index out of range");
								return false;
		                    }
		                    
							return ImprovedChat.moveRule(args[0], from, to);
		                }
		            }
		        }
		        
				return false;
			}
		});

        ImprovedChat.commands.put("clear", new icCommand("Clears chat history.", null, "")
		{
			@Override
			public boolean process(String[] args)
			{
		        (ImprovedChat.getCurrentServer().tabs.get(ImprovedChat.getCurrentServer().currentTabIndex)).chatLines.clear();
		        return true;
			}
		});
        
        ImprovedChat.commands.put("var", new icCommand("used for creating variables", "~var <varName> <regex>", "Variable created")
		{
			
			@Override
			public boolean process(String[] args)
			{
		        return args != null && args.length >= 2 ? ImprovedChat.getCurrentServer().vars.add(args[0], ImprovedChat.unsplit(args, 1)) : false;
			}
		});
        
        ImprovedChat.commands.put("reload", new icCommand("reloads config", null, "Config reloaded")
		{
			@Override
			public boolean process(String[] args)
			{
		        ImprovedChat.load();
		        return true;
			}
		});
        
        ImprovedChat.commands.put("delete", new icCommand("deletes variables, binds or rules", "~delete (bind|var|input|output|display) <id>", "")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 2)
		        {
		            if (args[0].equalsIgnoreCase("bind"))
		            {
		                return ImprovedChat.removeKB(args[1].toUpperCase());
		            }
					if (args[0].equalsIgnoreCase("var"))
					{
					    ImprovedChat.removeVar(args[1]);
					}

					int deleteIndex;

					if (ImprovedChat.isNumeric(args[1]) && (deleteIndex = Integer.parseInt(args[1])) >= 0)
					{
					    if (!args[0].equalsIgnoreCase("track") && !args[0].equalsIgnoreCase("ignore"))
					    {
					        ImprovedChat.removeRule(args[0], deleteIndex);
					        return true;
					    }
						ArrayList<Pattern> patternList;

						if (args[0].equals("ignore"))
						{
						    patternList = (ImprovedChat.getCurrentServer().tabs.get(ImprovedChat.getCurrentServer().currentTabIndex)).ignore;
						}
						else
						{
						    patternList = (ImprovedChat.getCurrentServer().tabs.get(ImprovedChat.getCurrentServer().currentTabIndex)).track;
						}

						if (deleteIndex >= patternList.size())
						{
						    ImprovedChat.stderr("Index out of bounds.");
						    return false;
						}
						patternList.remove(deleteIndex);
						return true;
					}
					
					ImprovedChat.stderr("Second argument should be a nonegative number.");
		        }
				return false;
			}
		});
        
        ImprovedChat.commands.put("script", new icCommand("run scripts from desktop(D:), .minecraft folder(M:) or mod folder", "~script [D:|M:]<scriptName>", "Script executed")
        {
        	@Override
        	public boolean process(String[] args)
        	{
        		if (args != null && args.length >= 1)
        		{
        			String scriptName = args[0];
        			File sourceFolder;
        			
        			if (scriptName.startsWith("D:"))
        			{
        				sourceFolder = new File(System.getProperty("user.home") + File.separator + "Desktop");
        				scriptName = scriptName.substring(2);
        			}
        			else
        			{
        				sourceFolder = LiteLoader.getGameDirectory();
        				
        				if (scriptName.startsWith("M:"))
        				{
        					scriptName = scriptName.substring(2);
        				}
        				else
        				{
        					sourceFolder = new File(sourceFolder, "mods" + File.separator + "wd1966");
        				}
        			}
        			
        			File scriptFile = new File(sourceFolder, scriptName);
        			
        			if (!scriptFile.exists())
        			{
        				ImprovedChat.stderr("File " + args[0] + " doesn\'t exist.");
        				return true;
        			}
        			
					try
					{
						BufferedReader reader = new BufferedReader(new FileReader(scriptFile));
						String scriptLine;
						
						while ((scriptLine = reader.readLine()) != null)
						{
							if (!scriptLine.startsWith("#"))
							{
								StringBuffer buffer = new StringBuffer();
								Matcher scriptVarPatternMatcher = ImprovedChat.scriptVar.matcher(scriptLine);
								
								while (scriptVarPatternMatcher.find())
								{
									int pos = Integer.parseInt(scriptVarPatternMatcher.group().substring(1));
									
									if (pos >= 0 && pos < args.length)
									{
										scriptVarPatternMatcher.appendReplacement(buffer, args[pos]);
									}
								}
								
								scriptVarPatternMatcher.appendTail(buffer);
								ImprovedChat.process(buffer.toString());
							}
						}
						
						reader.close();
						return true;
					}
					catch (FileNotFoundException ex)
					{
						ex.printStackTrace();
						return false;
					}
					catch (IOException ex)
					{
						ex.printStackTrace();
						return false;
					}
        		}
        		
				return false;
        	}        	
        });
        
        ImprovedChat.commands.put("input", new icCommand("makes rules for formating input window", "~input <regex>  <repl>", "Input rule created")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 3)
		        {
		            for (int arg = 1; arg < args.length; ++arg)
		            {
		                if (args[arg].equals(""))
		                {
		                    ImprovedChat.addRule("input", ImprovedChat.unsplit(args, 0, arg), ImprovedChat.unsplit(args, arg + 1));
		                    return true;
		                }
		            }

		            return false;
		        }
				return false;
			}
		});
        
        ImprovedChat.commands.put("output", new icCommand("makes rules for formating output messages", "~output <regex>  <repl>", "Output rule created")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 3)
		        {
		            for (int arg = 1; arg < args.length; ++arg)
		            {
		                if (args[arg].equals(""))
		                {
		                    ImprovedChat.addRule("output", ImprovedChat.unsplit(args, 0, arg), ImprovedChat.unsplit(args, arg + 1));
		                    return true;
		                }
		            }

		            return false;
		        }
		        
				return false;
			}
		});
        
        ImprovedChat.commands.put("display", new icCommand("makes rules for formating chat box", "~display <regex>  <repl>", "Display rule created")
		{
			
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 3)
		        {
		            for (int arg = 1; arg < args.length; ++arg)
		            {
		                if (args[arg].equals(""))
		                {
		                    ImprovedChat.addRule("display", ImprovedChat.unsplit(args, 0, arg), ImprovedChat.unsplit(args, arg + 1));
		                    return true;
		                }
		            }

		            return false;
		        }
		        
				return false;
			}
		});
        
        ImprovedChat.commands.put("seed", new icCommand("Sets seed for this world (for Rei\'s minimap", "~seed <seed>", "Set seed")
		{
			
			@Override
			public boolean process(String[] args)
			{
		        if (args != null)
		        {
		            try
		            {
		                setSeed(Long.parseLong(args[0]));
		                return true;
		            }
		            catch (NumberFormatException ex)
		            {
		                return false;
		            }
		        }
		        
				return false;
			}
		});
        
        ImprovedChat.commands.put("close", new icCommand("Used for closing the curent tab", "~close", "")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (ImprovedChat.getCurrentServer().tabs.size() < 2)
		        {
		            ImprovedChat.stderr("Can not remove only tab.");
		            return false;
		        }
		        
				ImprovedChat.getCurrentServer().tabs.remove(ImprovedChat.getCurrentServer().currentTabIndex);

				if (ImprovedChat.getCurrentServer().currentTabIndex >= ImprovedChat.getCurrentServer().tabs.size())
				{
				    ImprovedChat.getCurrentServer().currentTabIndex = ImprovedChat.getCurrentServer().tabs.size() - 1;
				}

				ImprovedChat.console("Tab removed.");
				return true;
			}
		});
        
        ImprovedChat.commands.put("blink", new icCommand("Blink on new messages", "~blink (on|off)", "")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 1)
		        {
		            ImprovedChat.getCurrentServer().currentTab().blink = args[0].equalsIgnoreCase("on");
		            return true;
		        }
		        
				return false;
			}
		});
        
        ImprovedChat.commands.put("track", new icCommand("Includes all the messages of given format into this tab", "~track <regex>", "")
		{
			
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 1)
		        {
		            String joined = ImprovedChat.unsplit(args, 0);
		            ImprovedChat.getCurrentServer().currentTab().track(joined);
		            return true;
		        }
		        
				return false;
			}
		});

        ImprovedChat.commands.put("ignore", new icCommand("Excludes all the messages of given format from this tab", "~ignore <regex>", "")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 1)
		        {
		            String joined = ImprovedChat.unsplit(args, 0);
		            ImprovedChat.getCurrentServer().currentTab().ignore(joined);
		            return true;
		        }
		        
				return false;
			}
		});

        ImprovedChat.commands.put("prefix", new icCommand("All messages sent in this tab will start with this prefix", "~prefix <prefix>", "")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 1)
		        {
		            ImprovedChat.getCurrentServer().currentTab().prefix = ImprovedChat.unsplit(args, 0);
		            return true;
		        }
		        
				return false;
			}
		});

        ImprovedChat.commands.put("tab", new icCommand("Used for creating and renameing tabs.", "~tab [name] <name>", "")
		{
			@Override
			public boolean process(String[] args)
			{
		        if (args != null && args.length >= 1)
		        {
		            if (args[0].equalsIgnoreCase("name"))
		            {
		                if (args.length < 2)
		                {
		                    ImprovedChat.console("\247cMissing parametar.");
		                    return false;
		                }
						ImprovedChat.getCurrentServer().currentTab().setName(ImprovedChat.unsplit(args, 1));
						return true;
		            }
		            
					ImprovedChat.getCurrentServer().tabs.add(new Tab(ImprovedChat.unsplit(args, 0)));
					ImprovedChat.getCurrentServer().currentTabIndex = ImprovedChat.getCurrentServer().tabs.size() - 1;
					ImprovedChat.console("\247aTab created.");
					return true;
		        }
		        
				return false;
			}
		});

        ImprovedChat.load();

        if (ImprovedChat.globalServer == null)
        {
            ImprovedChat.globalServer = new Server("Global", "");
            Tab defaultTab = new Tab("Default");
            ImprovedChat.globalServer.tabs.add(defaultTab);
            defaultTab.track("^");
        }

        ImprovedChat.setCurrentServer(ImprovedChat.globalServer);
    }

    private static boolean startsWithChannelKey(String text)
    {
        Iterator<icChannel> channels = ImprovedChat.icChannels.iterator();
        icChannel channel;

        do
        {
            if (!channels.hasNext())
            {
                return false;
            }

            channel = channels.next();
        }
        while (!text.startsWith(channel.getPrefix().toString()));

        return true;
    }

    public static void addChannel(icChannel channel)
    {
        ImprovedChat.icChannels.add(channel);
    }

    public static void removeChannel(icChannel channel)
    {
        ImprovedChat.icChannels.remove(channel);
    }

    public static void process(String text)
    {
        if (text != null && !text.trim().equals(""))
        {
            if (!text.startsWith("~") && !text.startsWith("/") && !ImprovedChat.startsWithChannelKey(text))
            {
                text = (ImprovedChat.getCurrentServer().tabs.get(ImprovedChat.getCurrentServer().currentTabIndex)).prefix + text;
            }

            if (text.startsWith("~"))
            {
                ImprovedChat.exec(text.substring(1));
            }
            else
            {
                boolean send = true;
                Iterator<icChannel> channels = ImprovedChat.icChannels.iterator();

                while (channels.hasNext())
                {
                    icChannel channel = channels.next();

                    if (channel.getPrefix().charValue() == text.charAt(0))
                    {
                        send = channel.isFallthrough();
                        ArrayList<String> tokens = new ArrayList<String>();
                        String[] parts = text.substring(1).split(" ");

                        for (int partNumber = 1; partNumber < parts.length; ++partNumber)
                        {
                            if (!parts[partNumber].trim().equals(""))
                            {
                                tokens.add(parts[partNumber]);
                            }
                        }

                        channel.process(parts[0], tokens.toArray(new String[tokens.size()]));
                    }
                }

                if (send)
                {
                    ImprovedChat.send(text);
                }
            }
        }
    }

    public WorldInfo getWorld()
    {
        return ImprovedChat.minecraft.theWorld.getWorldInfo();
    }

    public static void exec(String commandString)
    {
        if (!commandString.startsWith("("))
        {
            String[] parts = ImprovedChat.space.split(commandString, 2);
            Server currentServer = ImprovedChat.getCurrentServer();

            if (parts.length == 2 && parts[0].equalsIgnoreCase("global"))
            {
                ImprovedChat.setCurrentServer(ImprovedChat.globalServer);
                parts = ImprovedChat.space.split(parts[1], 2);
            }

            icCommand command = ImprovedChat.commands.get(parts[0].toLowerCase());

            if (command == null)
            {
                ImprovedChat.console("\247cCommand " + parts[0] + " doesn\'t exist.");
            }
            else
            {
                if (parts.length != 2)
                {
                    parts = null;
                }
                else
                {
                    parts = ImprovedChat.space.split(parts[1], -1);
                }

                if (command.process(parts))
                {
                    ImprovedChat.console("\247a" + command.success);
                }
                else
                {
                    ImprovedChat.console("\2476Description\247f: \247e" + command.desc);
                    ImprovedChat.console("\2476Usage\247f: \247e" + command.usage);
                }

                ImprovedChat.setCurrentServer(currentServer);
                ImprovedChat.save();
            }
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            int pos;
            reparse:

            for (pos = 1; pos < commandString.length(); ++pos)
            {
                char charAt;

                switch (charAt = commandString.charAt(pos))
                {
                    case ')':
                        break reparse;

                    case '\\':
                        if (pos + 1 < commandString.length() && commandString.charAt(pos + 1) == 41)
                        {
                            sb.append(')');
                            ++pos;
                            break;
                        }
                        //$FALL-THROUGH$

					default:
                        sb.append(charAt);
                }
            }

            commandString = commandString.substring(pos + 1);
            String prefix = sb.toString();
            int remainingChars = 100 - prefix.length();

            if (remainingChars <= 0)
            {
                ImprovedChat.stderr("Prefix is too long");
            }
            else
            {
                ImprovedChat.send(commandString);
            }
        }
    }

    public static boolean isNumeric(String string)
    {
        for (int pos = 0; pos < string.length(); ++pos)
        {
            if (string.charAt(pos) < 48 || string.charAt(pos) > 57)
            {
                return false;
            }
        }

        return true;
    }

    public static String getHeroChatChatMode(String string)
    {
        String[] parts = string.split("\u00a7eNow chatting in ");
        return parts.length > 1 ? parts[1].substring(0, parts[1].length() - 3) : null;
    }

    public static void receiveChatPacket(String message)
    {
        String token;

        if (message.startsWith("\u00a7eNow chatting in "))
        {
            token = ImprovedChat.getHeroChatChatMode(message);
            String mode = "[" + token + "\u00a7f]";
            ImprovedChat.getCurrentServer().ChatMode = token != null ? mode : null;
            ImprovedChat.getCurrentServer().heroChat = Boolean.valueOf(true);
            ImprovedChat.save();
        }
        else if (message.startsWith("Seed: "))
        {
            token = message.substring(6);

            try
            {
                ImprovedChat.getInstance().setSeed(Long.parseLong(token));
            }
            catch (Exception ex) {}
        }
    }

    public static void receiveLine(String message, int updateCounter, int id)
    {
        ImprovedChat.receiveChatPacket(message);

        if (!ImprovedChat.chatDisabled)
        {
            ImprovedChat.varProcess(message);
            ImprovedChat.stdout(message, updateCounter, id);
        }
    }

    public static Tab currentTab()
    {
        return ImprovedChat.getCurrentServer().currentTab();
    }

    public static String getServer(int serverIndex)
    {
        if (serverIndex > ImprovedChat.servers.size())
        {
            return null;
        }
        
		Enumeration<String> serverEnumerator = ImprovedChat.servers.keys();
		String serverName;

		for (serverName = null; serverIndex-- > 0 && serverEnumerator.hasMoreElements(); serverName = serverEnumerator.nextElement())
		{
		}

		return serverName;
    }

    public static String getServerAddress(String address)
    {
        return (ImprovedChat.servers.get(address)).address;
    }

    public static void copy(String text)
    {
        ImprovedChat.clipboard.setContents(new StringSelection(text), (ClipboardOwner)null);
    }

    public static String paste()
    {
        String text = "";
        Transferable clipboardContents = ImprovedChat.clipboard.getContents((Object)null);

        if (clipboardContents != null && clipboardContents.isDataFlavorSupported(DataFlavor.stringFlavor))
        {
            try
            {
                text = (String)clipboardContents.getTransferData(DataFlavor.stringFlavor);
            }
            catch (UnsupportedFlavorException ex) {}
            catch (IOException ex) {}
        }

        return text;
    }

    public static boolean isChatDisabled()
    {
        return ImprovedChat.chatDisabled;
    }

    public static void setChatDisabled(boolean disabled)
    {
        ImprovedChat.chatDisabled = disabled;
    }

	/**
	 * @return the modDir
	 */
	public static File getModDir()
	{
		return ImprovedChat.modDir;
	}

	/**
	 * @return the pastCommands
	 */
	public static List<String> getPastCommands()
	{
		return ImprovedChat.pastCommands;
	}

	/**
	 * @return the bgOpacity
	 */
	public static int getBgOpacity()
	{
		return ImprovedChat.bgOpacity;
	}

	/**
	 * @return the bgColor
	 */
	public static int getBgColor()
	{
		return ImprovedChat.bgColor;
	}

	/**
	 * @return the historyOpacity
	 */
	public static int getHistoryOpacity()
	{
		return ImprovedChat.historyOpacity;
	}

	/**
	 * @return the historyColor
	 */
	public static int getHistoryColor()
	{
		return ImprovedChat.historyColor;
	}

	/**
	 * @return the historyMaxLines
	 */
	public static int getHistoryMaxLines()
	{
		return ImprovedChat.historyMaxLines;
	}

	/**
	 * @return the chatLineMaxLength
	 */
	public static int getChatLineMaxLength()
	{
		return ImprovedChat.chatLineMaxLength;
	}

	/**
	 * @return the chatLinesSmall
	 */
	public static byte getChatLinesSmall()
	{
		return ImprovedChat.chatLinesSmall;
	}

	/**
	 * @return the chatLinesBig
	 */
	public static byte getChatLinesBig()
	{
		return ImprovedChat.chatLinesBig;
	}

	/**
	 * @return the scrollLines
	 */
	public static int getScrollLines()
	{
		return ImprovedChat.scrollLines;
	}

	/**
	 * @return the instance
	 */
	public static ImprovedChat getInstance()
	{
		return ImprovedChat.instance;
	}
}
