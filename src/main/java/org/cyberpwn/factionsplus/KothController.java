package org.cyberpwn.factionsplus;

import org.phantomapi.clust.ConfigurableController;
import org.phantomapi.construct.Controllable;

public class KothController extends ConfigurableController
{
	public KothController(Controllable parentController)
	{
		super(parentController, "koth");
	}

	@Override
	public void onStart()
	{
		loadCluster(this);
	}

	@Override
	public void onStop()
	{
		
	}
}
