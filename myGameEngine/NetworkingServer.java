package myGameEngine;

import java.io.IOException;
import ray.networking.IGameConnection.ProtocolType;
import java.lang.Thread; // For npc loop thread.yield()
public class NetworkingServer
{
	// Npc stuff
	//private NPCcontroller npcCtrl;
	//GameAIServerTCP tcpServer;
	
	private GameServerUDP thisUDPServer;
	//private GameServerTCP thisTCPServer;
	public NetworkingServer(int serverPort, String protocol)
	{
		try
		{
			if(protocol.toUpperCase().compareTo("TCP") == 0)
			{
				//thisTCPServer = new GameServerTCP(serverPort);
			}
			else
			{
				thisUDPServer = new GameServerUDP(serverPort);
				if (thisUDPServer != null) 
					System.out.println("Game server created");
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void TestNetworkingServer(int id) // constructor
	{/*
		startTime = System.nanoTime();
		lastUpdateTime = startTime;
		npcCtrl = new NPCcontroller();
		
		// start networking TCP server (as before)
		
		// start NPC control loop
		npcCtrl.setupNPCs();
		npcLoop();*/
	}
	
	public void npcLoop() // NPC control loop
	{/*
		while (true)
		{
			long frameStartTime = System.nanoTime();
			float elapMilSecs = (frameStartTime-lastUpdateTime)/(1000000.0f);
			if (elapMilSecs >= 50.0f)
			{
				lastUpdateTime = frameStartTime;
				npcCtrl.updateNPCs();
				tcpServer.sendNPCinfo();
			}
			Thread.yield();
		}*/
	}
 
	public static void main(String[] args)
	{
		if(args.length > 1)
		{
			NetworkingServer app = new NetworkingServer(Integer.parseInt(args[0]), args[1]);
		}
	}
}