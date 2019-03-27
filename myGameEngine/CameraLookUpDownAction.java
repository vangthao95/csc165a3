package myGameEngine;

import ray.input.action.AbstractInputAction;
import net.java.games.input.Event;
import ray.rage.scene.*; // for Camera
import ray.rml.Angle;
import ray.rml.Degreef;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class CameraLookUpDownAction extends AbstractInputAction
{
	private Camera camera;
	public CameraLookUpDownAction(Camera c)
	{
		camera = c;
	}

	public void performAction(float time, Event e)
	{
		
		Angle rotAmt = rotAmt = Degreef.createFrom(0.0f);
		float rxValue = e.getValue();
		if (rxValue > 0.01f || rxValue < -0.01f)
			rotAmt = Degreef.createFrom(rxValue * -1.0f*.33f);				
		
		Vector3 f = camera.getFd();
        Vector3 r = camera.getRt();
        Vector3 u = camera.getUp();
		Vector3 fn = (f.rotate(rotAmt, r)).normalize();
		Vector3 un = (u.rotate(rotAmt, r)).normalize();
		camera.setFd((Vector3f)Vector3f.createFrom(fn.x(), fn.y(), fn.z()));
		camera.setUp((Vector3f)Vector3f.createFrom(un.x(), un.y(), un.z()));
		
		System.out.println("RY value: " + e.getValue());
		//System.out.println("look left action initiated");
	}
}