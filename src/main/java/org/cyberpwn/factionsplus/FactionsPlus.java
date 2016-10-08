package org.cyberpwn.factionsplus;

import org.phantomapi.construct.PhantomPlugin;
import org.phantomapi.util.DMSRequire;
import org.phantomapi.util.DMSRequirement;

@DMSRequire(DMSRequirement.SQL)
public class FactionsPlus extends PhantomPlugin
{
	private FactionScanner scanner;
	private FactionOwnerController factionOwnerController;
	private EndermanController endermanController;
	
	@Override
	public void enable()
	{
		this.scanner = new FactionScanner(this);
		this.endermanController = new EndermanController(this);
		this.factionOwnerController = new FactionOwnerController(this);
		
		register(scanner);
		register(endermanController);
		register(factionOwnerController);
	}

	@Override
	public void disable()
	{
		
	}
}
