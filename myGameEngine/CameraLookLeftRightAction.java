package myGameEngine;

import ray.input.action.AbstractInputAction;
import net.java.games.input.Event;
import ray.rage.scene.*; // for Camera
import ray.rml.Angle;
import ray.rml.Degreef;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class CameraLookLeftRightAction extends AbstractInputAction
{
	private Camera camera;
	public CameraLookLeftRightAction(Camera c)
	{
		camera = c;
	}

	public void performAction(float time, Event e)
	{
		Angle rotAmt = rotAmt = Degreef.createFrom(0.0f);	;
		float rxValue = e.getValue();
		if (rxValue > 0.01f || rxValue < -0.01f)
			rotAmt = Degreef.createFrom(rxValue * -1.0f*.33f);
		
        Vector3 f = camera.getFd();
        Vector3 r = camera.getRt();
        Vector3 u = camera.getUp();
		Vector3 fn = (f.rotate(rotAmt, u)).normalize();
		Vector3 rn = (r.rotate(rotAmt, u)).normalize();
		camera.setRt((Vector3f)Vector3f.createFrom(rn.x(), rn.y(), rn.z()));
		camera.setFd((Vector3f)Vector3f.createFrom(fn.x(), fn.y(), fn.z()));	
		
		System.out.println("RX value: " + e.getValue());
		//System.out.println("look left action initiated");
	}
}