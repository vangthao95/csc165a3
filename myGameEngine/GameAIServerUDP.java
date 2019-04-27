package myGameEngine;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;

public class GameAIServerUDP extends GameConnectionServer<UUID>
{
	public GameAIServerUDP(int localPort) throws IOException
	{
		super(localPort, ProtocolType.UDP);
	}
	
	@Override
	public void processPacket(Object o, InetAddress senderIP, int sndPort)
	{
		String message = (String) o;
		String[] msgTokens = message.split(",");
		if(msgTokens.length > 0)
		{
			// also additional cases for receiving messages about NPCs, such as:
			if(msgTokens[0].compareTo("needNPC") == 0)
			{
				//. . .
			}
			else if(msgTokens[0].compareTo("collide") == 0)
			{
			//. . .
			}
		}
	}
	// game protocol as before, plus additional NPC protocol cases. i.e.,
	// messages regarding NPC’s sent to clients, such as:
	public void sendNPCinfo() // informs clients of new NPC positions
	{/*
		for (int i=0; i<npcCtrl.getNumOfNPCs(); i++)
		{
			try
			{
				String message = new String("mnpc," + Integer.toString(i));
				message += "," + (npcCtrl.getNPC(i)).getX();
				message += "," + (npcCtrl.getNPC(i)).getY();
				message += "," + (npcCtrl.getNPC(i)).getZ();
				sendPacketToAll(message);
				//. . .
			}
			
		}*/
	}
}