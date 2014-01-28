package wdmods.improvedchat;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * @author wd1966
 */
public class Server
{
    public List<Tab> tabs;
    public int currentTabIndex;
    public List<Hashtable<Integer, String>> bindings;
    public Boolean colorchat;
    public Boolean heroChat;
    public Integer lines;
    public Variables vars;
    public String[] translations;
    public String ChatMode;
    public String name;
    public String address;

    public Server(String address)
    {
        this(address, address);
        tabs = new ArrayList<Tab>();
        Tab defaultServerTab = new Tab("Default");
        tabs.add(defaultServerTab);
        defaultServerTab.track("^");
    }

    public Server(String name, String address)
    {
        currentTabIndex = 0;
        vars = new Variables();
        this.name = name;
        this.address = address;
        bindings = new ArrayList<Hashtable<Integer, String>>();
        colorchat = Boolean.valueOf(false);
        heroChat = Boolean.valueOf(false);
        tabs = new ArrayList<Tab>();
        lines = Integer.valueOf(20);
        ChatMode = null;

        for (int i = 0; i < 4; ++i)
        {
            bindings.add(new Hashtable<Integer, String>());
        }
    }

    public Tab currentTab()
    {
        return tabs.get(currentTabIndex);
    }

    public void nextTab()
    {
        (tabs.get(currentTabIndex)).blinking = false;
        ++currentTabIndex;

        if (currentTabIndex >= tabs.size())
        {
            currentTabIndex = 0;
        }

        (tabs.get(currentTabIndex)).blinking = true;
    }

    public void previousTab()
    {
        (tabs.get(currentTabIndex)).blinking = false;
        --currentTabIndex;

        if (currentTabIndex < 0)
        {
            currentTabIndex = tabs.size() - 1;
        }

        (tabs.get(currentTabIndex)).blinking = true;
    }
}
