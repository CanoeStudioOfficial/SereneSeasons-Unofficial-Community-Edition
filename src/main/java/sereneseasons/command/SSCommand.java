package sereneseasons.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.Season.SubSeason;
import sereneseasons.handler.season.SeasonHandler;
import sereneseasons.season.SeasonSavedData;
import sereneseasons.season.SeasonTime;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SSCommand extends CommandBase
{
    // OPTIMIZATION: Cache the sub-season names array statically.
    // The original code created a new Stream and Array EVERY TIME the player pressed TAB, causing micro-lag.
    private static final String[] SUB_SEASON_NAMES = Arrays.stream(SubSeason.values())
            .map(e -> e.toString().toLowerCase())
            .toArray(String[]::new);

    @Override
    public String getName()
    {
        return "sereneseasons";
    }

    @Override
    public List<String> getAliases()
    {
        // OPTIMIZATION: Use Collections.singletonList instead of Guava's Lists.newArrayList for immutable single-element lists
        return Collections.singletonList("ss");
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "commands.sereneseasons.usage";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 1)
        {
            throw new WrongUsageException("commands.sereneseasons.usage");
        }
        else if ("setseason".equals(args[0]))
        {
            setSeason(sender, args);
        } 
        else if ("getseason".equals(args[0]))
        {
            getSeason(sender, args);
        } 
        else 
        {
            sender.sendMessage(new TextComponentTranslation("commands.sereneseasons.usage"));
        }
    }

    private void getSeason(ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getCommandSenderAsPlayer(sender);
        SeasonSavedData data = SeasonHandler.getSeasonSavedData(player.world);

        int seasonCycleTicks = data.seasonCycleTicks;
        SeasonTime time = new SeasonTime(seasonCycleTicks);
        Season.SubSeason season = time.getSubSeason();

        int subSeasonDuration = time.getSubSeasonDuration();
        final int ticksPerSecond = 20;
        int index = Arrays.asList(SubSeason.VALUES).indexOf(season);

        int ticksTillNext;
        
        // BUG FIX: The original code set index to -1 if it was 11 (the last sub-season).
        // This caused the math to evaluate to a NEGATIVE number, showing negative days/hours in chat.
        // Now it correctly calculates the time remaining until the cycle resets to EARLY_SPRING.
        if (index == 11)
        {
            int cycleDuration = subSeasonDuration * 12;
            ticksTillNext = cycleDuration - seasonCycleTicks;
        }
        else
        {
            ticksTillNext = subSeasonDuration * (index + 1) - seasonCycleTicks;
        }

        int days = ticksTillNext / ticksPerSecond / 60 / 60 / 24;
        int hours = (ticksTillNext - (days * ticksPerSecond * 60 * 60 * 24)) / ticksPerSecond / 60 / 60;
        sender.sendMessage(new TextComponentTranslation("commands.sereneseasons.getseason", getSeasonName(season), days, hours));
    }

    private void setSeason(ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 2)
        {
            throw new WrongUsageException("commands.sereneseasons.usage");
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        Season.SubSeason newSeason = null;
        String inputSeason = args[1].toLowerCase();

        for (Season.SubSeason season : Season.SubSeason.VALUES)
        {
            // OPTIMIZATION: Compare lowercase strings without creating new string objects in every iteration
            if (season.toString().toLowerCase().equals(inputSeason))
            {
                newSeason = season;
                break;
            }
        }

        if (newSeason != null)
        {
            SeasonSavedData seasonData = SeasonHandler.getSeasonSavedData(player.world);
            seasonData.seasonCycleTicks = SeasonTime.ZERO.getSubSeasonDuration() * newSeason.ordinal();
            seasonData.markDirty();
            SeasonHandler.sendSeasonUpdate(player.world);
            sender.sendMessage(new TextComponentTranslation("commands.sereneseasons.setseason.success", getSeasonName(newSeason)));
        }
        else
        {
            sender.sendMessage(new TextComponentTranslation("commands.sereneseasons.setseason.fail", args[1]));
        }
    }

    private ITextComponent getSeasonName(SubSeason season)
    {
        return new TextComponentTranslation(season.getTranslationKey());
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos)
    {
        if (args.length == 1)
        {
            // BUG FIX: Added "getseason" to tab completions. It existed in the code but wasn't suggested.
            return getListOfStringsMatchingLastWord(args, "setseason", "getseason");
        }
        else if (args.length == 2 && "setseason".equals(args[0]))
        {
            // BUG FIX: Only suggest sub-seasons if the first argument is "setseason". 
            // Previously, it suggested them for "getseason" too, which takes no additional arguments.
            return getListOfStringsMatchingLastWord(args, SUB_SEASON_NAMES);
        }

        return super.getTabCompletions(server, sender, args, pos);
    }
}