package myGameEngine;

import java.io.IOException;
import ray.networking.IGameConnection.ProtocolType;
public class NetworkingServer
{
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
	public static void main(String[] args)
	{
		if(args.length > 1)
		{
			NetworkingServer app = new NetworkingServer(Integer.parseInt(args[0]), args[1]);
		}
	}
}