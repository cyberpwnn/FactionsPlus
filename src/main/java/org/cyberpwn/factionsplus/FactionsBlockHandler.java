package org.cyberpwn.factionsplus;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.phantomapi.block.BlockHandler;

public class FactionsBlockHandler implements BlockHandler
{
	@Override
	public boolean canModify(Player p, Block block)
	{
		if(FA.getFaction(p).equals(FA.getFaction(block.getLocation())))
		{
			return true;
		}
		
		return false;
	}

	@Override
	public boolean hasProtection(Block block)
	{
		return FA.isClaimed(block.getChunk());
	}

	@Override
	public String getProtector()
	{
		return "Factions";
	}

	@Override
	public String getProtector(Block block)
	{
		return FA.getFaction(block.getChunk()).getTag();
	}
}
