package org.cyberpwn.factionsplus;

import org.phantomapi.Phantom;
import org.phantomapi.construct.PhantomPlugin;
import org.phantomapi.util.DMSRequire;
import org.phantomapi.util.DMSRequirement;

@DMSRequire(DMSRequirement.SQL)
public class FactionsPlus extends PhantomPlugin
{
	private FactionsBlockHandler bh;
	private FactionScanner scanner;
	private FactionOwnerController factionOwnerController;
	private EndermanController endermanController;
	
	@Override
	public void enable()
	{
		this.scanner = new FactionScanner(this);
		this.endermanController = new EndermanController(this);
		this.factionOwnerController = new FactionOwnerController(this);
		this.bh = new FactionsBlockHandler();
		
		register(scanner);
		register(endermanController);
		register(factionOwnerController);
	}

	@Override
	public void disable()
	{
		
	}
	
	public void onStart()
	{
		Phantom.instance().getBlockCheckController().registerBlockHandler(bh);
	}
	
	public void onStop()
	{
		Phantom.instance().getBlockCheckController().unRegisterBlockHandler(bh);
	}
}
