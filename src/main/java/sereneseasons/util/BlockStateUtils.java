package sereneseasons.util;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import sereneseasons.api.ISSBlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map.Entry;

public class BlockStateUtils
{




    public static String getStateInfoAsString(IBlockState state)
    {
        StringBuilder desc = new StringBuilder(state.getBlock().getClass().getName() + "[");
        boolean first = true;


        for (Entry<IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet())
        {
            if (!first) {
                desc.append(",");
            }
            IProperty<?> iproperty = entry.getKey();
            Comparable<?> comparable = entry.getValue();
            desc.append(iproperty.getName()).append("=").append(getPropertyName(iproperty, comparable));
            first = false;
        }
        desc.append("]");
        return desc.toString();
    }


    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String getPropertyName(IProperty<T> property, Comparable<?> value)
    {
        return property.getName((T) value);
    }





    public static ImmutableSet<IBlockState> getStatesSet(IBlockState baseState, IProperty<?>... properties)
    {

        Deque<IProperty<?>> propStack = new ArrayDeque<>();
        List<IBlockState> states = new ArrayList<>();

        for (IProperty<?> prop : properties) {
            propStack.push(prop);
        }

        if (!propStack.isEmpty())
        {
            addStatesToList(baseState, states, propStack);
        }

        return ImmutableSet.copyOf(states);
    }




    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> void addStatesToList(IBlockState state, List<IBlockState> list, Deque<IProperty<?>> stack)
    {
        if (stack.isEmpty())
        {
            list.add(state);
            return;
        }

        IProperty<T> prop = (IProperty<T>) stack.pop();

        for (T value : prop.getAllowedValues())
        {
            addStatesToList(state.withProperty(prop, value), list, stack);
        }

        stack.push(prop);
    }






    public static ImmutableSet<IBlockState> getBlockPresets(Block block)
    {
        if (!(block instanceof ISSBlock)) {
            return ImmutableSet.of();
        }

        IBlockState defaultState = block.getDefaultState();
        if (defaultState == null) {
            defaultState = block.getBlockState().getBaseState();
        }

        return getStatesSet(defaultState, ((ISSBlock) block).getPresetProperties());
    }




    @SuppressWarnings("unchecked")
    public static IBlockState getPresetState(IBlockState state)
    {
        IBlockState outState = state.getBlock().getDefaultState();

        if (state.getBlock() instanceof ISSBlock)
        {
            ISSBlock bopBlock = (ISSBlock) state.getBlock();

            for (IProperty property : bopBlock.getPresetProperties())
            {
                outState = outState.withProperty(property, state.getValue(property));
            }
        }

        return outState;
    }




    public static IProperty<?> getPropertyByName(IBlockState blockState, String propertyName)
    {

        for (IProperty<?> property : blockState.getProperties().keySet())
        {
            if (property.getName().equals(propertyName))
                return property;
        }

        return null;
    }

    public static boolean isValidPropertyName(IBlockState blockState, String propertyName)
    {
        return getPropertyByName(blockState, propertyName) != null;
    }




    public static Comparable<?> getPropertyValueByName(IBlockState blockState, IProperty<?> property, String valueName)
    {

        for (Comparable<?> value : property.getAllowedValues())
        {
            if (value.toString().equals(valueName))
                return value;
        }

        return null;
    }
}