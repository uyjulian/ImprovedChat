package wdmods.improvedchat;

import java.io.File;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.login.INetHandlerLoginClient;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import wdmods.improvedchat.overrides.GuiImprovedChatNewChat;

import com.mojang.realmsclient.dto.RealmsServer;
import com.mumfrey.liteloader.ChatFilter;
import com.mumfrey.liteloader.InitCompleteListener;
import com.mumfrey.liteloader.JoinGameListener;
import com.mumfrey.liteloader.RenderListener;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.LiteLoaderEventBroker.ReturnValue;
import com.mumfrey.liteloader.util.ModUtilities;
import com.mumfrey.liteloader.util.ObfuscationUtilities;

/**
 * LiteLoader adapter class for Improved Chat
 * 
 * @author Adam Mummery-Smith
 */
public class LiteModImprovedChat implements InitCompleteListener, RenderListener, ChatFilter, JoinGameListener
{
	/**
	 * Keypress mask used to determine when keys are released
	 */
	private boolean[] pressed = new boolean[400];
	
	/**
	 * New persistent chat GUI 
	 */
	private GuiImprovedChatNewChat persistentChatGui;
	
	@Override
	public String getName()
	{
		return "Improved Chat";
	}
	
	@Override
	public String getVersion()
	{
		return "3.1.0";
	}
	
	@Override
	public void init(File configPath)
	{
	}
	
	@Override
	public void upgradeSettings(String version, File configPath, File oldConfigPath)
	{
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.InitCompleteListener#onInitCompleted(net.minecraft.client.Minecraft, com.mumfrey.liteloader.core.LiteLoader)
	 */
	@Override
	public void onInitCompleted(Minecraft minecraft, LiteLoader loader)
	{
		ImprovedChat.init(minecraft);
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.RenderListener#onRender()
	 */
	@Override
	public void onRender()
	{
		Minecraft minecraft = Minecraft.getMinecraft();
		
		// Replace persistent chat GUI
		if (minecraft.ingameGUI != null && minecraft.ingameGUI.getChatGUI() != null && !(minecraft.ingameGUI.getChatGUI() instanceof GuiImprovedChatNewChat))
		{
			try
			{
				// TODO Obfuscation - 1.8.9
				Field fChat = GuiIngame.class.getDeclaredField(ObfuscationUtilities.getObfuscatedFieldName("persistantChatGUI", "l", "field_73840_e"));
				fChat.setAccessible(true);
				if (persistentChatGui == null) persistentChatGui = new GuiImprovedChatNewChat(minecraft);
				fChat.set(minecraft.ingameGUI, persistentChatGui);
			}
			catch (Exception ex)
			{
				System.out.println("[ImprovedChat] ERROR OVER-RIDING CHAT GUI - improved chat probably won't function!");
			}
		}
		
		// Replace chat GUI
		if (minecraft.currentScreen != null && minecraft.currentScreen instanceof GuiChat && !(minecraft.currentScreen instanceof GuiImprovedChat) && !(minecraft.currentScreen instanceof GuiSleepMP))
		{
			minecraft.currentScreen = new GuiImprovedChat((GuiChat)minecraft.currentScreen);
		}

		// Replace sleep chat GUI
		if (minecraft.currentScreen != null && minecraft.currentScreen instanceof GuiSleepMP && !(minecraft.currentScreen instanceof GuiImprovedChatSleeping))
		{
			minecraft.currentScreen = new GuiImprovedChatSleeping((GuiSleepMP)minecraft.currentScreen);
		}
	}
	
	@Override
	public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock)
	{
		for (int bindingIndex = -100; bindingIndex < 255; bindingIndex++)
		{
			boolean isPressed = getKeyPressed(bindingIndex);
			
			if (!isPressed && pressed[bindingIndex + 128])
			{
				if (minecraft.currentScreen == null || minecraft.currentScreen.allowUserInput)
					ImprovedChat.keyPressed(bindingIndex);
			}
			else if (isPressed && !pressed[bindingIndex + 128])
			{
		        if ((Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) && Keyboard.isKeyDown(Keyboard.KEY_TAB))
		        {
		            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		            {
		                ImprovedChat.getCurrentServer().previousTab();
		            }
		            else
		            {
		                ImprovedChat.getCurrentServer().nextTab();
		            }
		        }
			}
			
			pressed[bindingIndex + 128] = isPressed;
		}
	}

	private boolean getKeyPressed(int bindingIndex)
	{
		if (bindingIndex < 0)
		{
			return Mouse.isButtonDown(bindingIndex + 100);
		}
		
		return Keyboard.isKeyDown(bindingIndex);
	}

	@Override
	public void onRenderGui(GuiScreen currentScreen)
	{
	}

	@Override
	public void onSetupCameraTransform()
	{
	}

	@Override
	public void onJoinGame(INetHandler netHandler,
			S01PacketJoinGame joinGamePacket, ServerData serverData,
			RealmsServer realmsServer) {
		if (netHandler instanceof NetHandlerPlayServer)
		{
			SocketAddress socketAddress = ((NetHandlerPlayServer)netHandler).getNetworkManager().getRemoteAddress();
			
			if (socketAddress instanceof InetSocketAddress)
			{
				InetSocketAddress inetAddr = (InetSocketAddress)socketAddress;
				
				String serverName = inetAddr.getHostName();
				int serverPort = inetAddr.getPort();
				
		        System.out.println("[ImprovedChat] Loading settings for " + serverName + ":" + serverPort);
		        ImprovedChat.setCurrent(serverName + "_" + serverPort);
			}
		}
		
	}

	@Override
	public boolean onChat(IChatComponent chat, String message,
			ReturnValue<IChatComponent> newMessage) {
		// TODO Auto-generated method stub
		return true;
	}

	
}
