package myGameEngine;

public class NPCcontroller
{
	private NPC[] NPClist = new NPC[5];
	public void updateNPCs()
	{
		for (int i=0; i<numNPCs; i++)
		{
			NPClist[i].updateLocation();
		}
	}

}