package wdmods.improvedchat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author wd1966
 */
public class PatternList
{
	/**
	 * @author wd1966
	 */
	public class Entry
	{
	    public Pattern pattern;
	    public String replacement;

	    public Entry(Pattern pattern, String replacement)
	    {
	        this.pattern = pattern;
	        this.replacement = replacement;
	    }
	}

	public List<Entry> list = new ArrayList<Entry>();
    private static Pattern evilDollarSign = Pattern.compile("\\$(?!\\d)");

    public void add(String pattern, String replacement)
    {
        replacement = PatternList.evilDollarSign.matcher(replacement).replaceAll("\\$");

        try
        {
            list.add(new Entry(Pattern.compile(pattern), replacement));
        }
        catch (PatternSyntaxException ex) {}
    }

    public String process(String text)
    {
        String result;
        Matcher entryMatcher;

        for (Iterator<Entry> entries = list.iterator(); entries.hasNext(); text = entryMatcher.replaceAll(result))
        {
            Entry entry = entries.next();
            entryMatcher = entry.pattern.matcher(text);
            result = ImprovedChat.replaceVars(entry.replacement);
            result = PatternList.evilDollarSign.matcher(result).replaceAll("\\\\\\$");
        }

        return text;
    }

    public void list()
    {
        for (int id = 0; id < list.size(); ++id)
        {
            Entry entry = list.get(id);
            ImprovedChat.unProccessedInput(id + ". (" + entry.pattern.toString() + ")->(" + entry.replacement + ")");
        }

        if (list.size() == 0)
        {
            ImprovedChat.unProccessedInput("Empty");
        }
    }

    public String move(int from, int to)
    {
        if (from >= 0 && from <= list.size())
        {
            if (to >= 0 && to <= list.size())
            {
                list.add(to, list.remove(from));
                return "Move: Rule successfully moved";
            }
            
			return "Move: Second index out of range";
        }
        
		return "Move: First index out of range";
    }

    public void clear()
    {
        list.clear();
    }
}
