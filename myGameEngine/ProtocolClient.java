package myGameEngine;

public class ProtocolClient extends GameConnectionClient
{
	private MyGame game;
	private UUID id;
	private Vector<GhostAvatar> ghostAvatars;
	
	public ProtocolClient(InetAddress remAddr, int remPort, ProtocolType pType, MyGame game)
	throws IOException
	{
		super(remAddr, remPort, pType);
		this.game = game;
		this.id = UUID.randomUUID();
		this.ghostAvatars = new Vector<GhostAvatar>();
	}
	@Override
	protected void processPacket(Object msg)
	{
		String strMessage = (String)message;
		String[] messageTokens = strMessage.split(",");
		if(messageTokens.length > 0)
		{
			if(msgTokens[0].compareTo("join") == 0) // receive join
			{ // format: join, success or join, failure
				if(msgTokens[1].compareTo("success") == 0)
				{
					game.setIsConnected(true);
					sendCreateMessage(game.getPlayerPosition());
				}
			if(msgTokens[1].compareTo("failure") == 0)
		{
			game.setIsConnected(false);
} }
if(messageTokens[0].compareTo("bye") == 0) // receive bye
{ // format: bye, remoteId
UUID ghostID = UUID.fromString(messageTokens[1]);
removeGhostAvatar(ghostID);
}
if ((messageTokens[0].compareTo("dsfr") == 0 ) // receive dsfr
 || (messageTokens[0].compareTo("create")==0))
{ // format: create, remoteId, x,y,z or dsfr, remoteId, x,y,z
UUID ghostID = UUID.fromString(messageTokens[1]);
Vector3 ghostPosition = Vector3f.createFrom(
Float.parseFloat(messageTokens[2]),
Float.parseFloat(messageTokens[3]),
Float.parseFloat(messageTokens[4]));
try
{ createGhostAvatar(ghostID, ghostPosition);
} catch (IOException e)
{ System.out.println("error creating ghost avatar");
} }
if(messageTokens[0].compareTo("wsds") == 0) // rec. create…
{ // etc…..
}
if(messageTokens[0].compareTo("wsds") == 0) // rec. wants…
{ // etc….. 
}
if(messageTokens[0].compareTo("move") == 0) // rec. move...
{ // etc….. 
}
} }
}
