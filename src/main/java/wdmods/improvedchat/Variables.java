package wdmods.improvedchat;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author wd1966
 */
public class Variables
{
	public class Variable
	{
	    public String value;
	    public Pattern pattern;

	    public Variable(String pattern)
	    {
	        value = "";
	        this.pattern = Pattern.compile(pattern);
	    }
	}

	private Hashtable<String, Variable> vars = new Hashtable<String, Variable>();

    public boolean add(String name, String pattern)
    {
        try
        {
            Variable variable = new Variable(pattern);
            vars.put(name, variable);
            return true;
        }
        catch (PatternSyntaxException ex)
        {
            ImprovedChat.stderr(ex.getMessage());
            return false;
        }
    }

    public Variable get(String name)
    {
        return vars.get(name);
    }

    public String getPattern(String name)
    {
        Variable variable = vars.get(name);
        return variable != null ? variable.pattern.toString() : "";
    }

    public void remove(String name)
    {
        vars.remove(name);
    }

    public void process(String text)
    {
        String variableName;
        Variable variable;

        for (Enumeration<String> variableEnum = vars.keys(); variableEnum.hasMoreElements(); vars.put(variableName, variable))
        {
            variableName = variableEnum.nextElement();
            variable = vars.get(variableName);
            Matcher varMatcher = variable.pattern.matcher(text);

            if (varMatcher.find())
            {
                variable.value = varMatcher.group();
            }
        }
    }

    public Enumeration<String> keys()
    {
        return vars.keys();
    }

    public void clear()
    {
        vars.clear();
    }
}
