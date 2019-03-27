// Author: Professor Scott Gordon

package myGameEngine;

import ray.rage.scene.*;
import ray.rage.scene.controllers.*;
import ray.rml.*;
public class CustomController extends AbstractController
{
	private float jumpRate = .0100f; // growth per second
	private float cycleTime = 2000.0f; // default cycle time
	private float totalTime = 0.0f;
	private float direction = 1.0f;
	private float curHeight = 0.0f;
	@Override
	protected void updateImpl(float elapsedTimeMillis)
	{
		totalTime += elapsedTimeMillis;
		curHeight = 1.0f + direction * jumpRate;
		if (totalTime > cycleTime)
		{
			direction = -direction;
			totalTime = 0.0f;
		}
		for (Node n : super.controlledNodesList)
		{
			Vector3 curPos = n.getLocalPosition();
			curPos = Vector3f.createFrom(curPos.x(), curPos.y()*curHeight, curPos.z());
			n.setLocalPosition(curPos);
		}
	}
}