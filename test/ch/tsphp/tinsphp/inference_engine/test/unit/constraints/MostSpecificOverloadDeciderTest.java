/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.test.unit.constraints;

import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.utils.ITypeHelper;
import ch.tsphp.tinsphp.inference_engine.constraints.IMostSpecificOverloadDecider;
import ch.tsphp.tinsphp.inference_engine.constraints.MostSpecificOverloadDecider;
import ch.tsphp.tinsphp.inference_engine.constraints.OverloadRankingDto;
import ch.tsphp.tinsphp.inference_engine.constraints.WorkItemDto;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MostSpecificOverloadDeciderTest
{

    @Test
    public void inNormalMode_HasAlreadyConvertibleAndOnlyFirstHasConvertibleParams_ReturnsFirst() {
        IBindingCollection bindingCollection = mock(IBindingCollection.class);
        when(bindingCollection.getNumberOfConvertibleApplications()).thenReturn(1);
        WorkItemDto workItemDto = new WorkItemDto(null, null, 0, true, bindingCollection);
        List<OverloadRankingDto> applicableOverloads = new ArrayList<>();
        IFunctionType overload = mock(IFunctionType.class);
        when(overload.hasConvertibleParameterTypes()).thenReturn(false);
        IFunctionType overloadWithConvertible = mock(IFunctionType.class);
        when(overloadWithConvertible.hasConvertibleParameterTypes()).thenReturn(true);
        OverloadRankingDto first = createOverloadRankingDto(overloadWithConvertible);
        OverloadRankingDto second = createOverloadRankingDto(overload);
        OverloadRankingDto third = createOverloadRankingDto(overload);
        applicableOverloads.add(first);
        applicableOverloads.add(second);
        applicableOverloads.add(third);

        IMostSpecificOverloadDecider decider = createMostSpecificOverloadDecider();
        OverloadRankingDto result = decider.inNormalMode(workItemDto, applicableOverloads, null);

        assertThat(result, is(first));
    }

    @Test
    public void inNormalMode_HasAlreadyConvertibleAndOnlySecondHasConvertibleParams_ReturnsSecond() {
        IBindingCollection bindingCollection = mock(IBindingCollection.class);
        when(bindingCollection.getNumberOfConvertibleApplications()).thenReturn(1);
        WorkItemDto workItemDto = new WorkItemDto(null, null, 0, true, bindingCollection);
        List<OverloadRankingDto> applicableOverloads = new ArrayList<>();
        IFunctionType overload = mock(IFunctionType.class);
        when(overload.hasConvertibleParameterTypes()).thenReturn(false);
        IFunctionType overloadWithConvertible = mock(IFunctionType.class);
        when(overloadWithConvertible.hasConvertibleParameterTypes()).thenReturn(true);
        OverloadRankingDto first = createOverloadRankingDto(overload);
        OverloadRankingDto second = createOverloadRankingDto(overloadWithConvertible);
        OverloadRankingDto third = createOverloadRankingDto(overload);
        applicableOverloads.add(first);
        applicableOverloads.add(second);
        applicableOverloads.add(third);

        IMostSpecificOverloadDecider decider = createMostSpecificOverloadDecider();
        OverloadRankingDto result = decider.inNormalMode(workItemDto, applicableOverloads, null);

        assertThat(result, is(second));
    }

    @Test
    public void inNormalMode_HasAlreadyConvertibleAndOnlyThirdHasConvertibleParams_ReturnsThird() {
        IBindingCollection bindingCollection = mock(IBindingCollection.class);
        when(bindingCollection.getNumberOfConvertibleApplications()).thenReturn(1);
        WorkItemDto workItemDto = new WorkItemDto(null, null, 0, true, bindingCollection);
        List<OverloadRankingDto> applicableOverloads = new ArrayList<>();
        IFunctionType overload = mock(IFunctionType.class);
        when(overload.hasConvertibleParameterTypes()).thenReturn(false);
        IFunctionType overloadWithConvertible = mock(IFunctionType.class);
        when(overloadWithConvertible.hasConvertibleParameterTypes()).thenReturn(true);
        OverloadRankingDto first = createOverloadRankingDto(overload);
        OverloadRankingDto second = createOverloadRankingDto(overload);
        OverloadRankingDto third = createOverloadRankingDto(overloadWithConvertible);
        applicableOverloads.add(first);
        applicableOverloads.add(second);
        applicableOverloads.add(third);

        IMostSpecificOverloadDecider decider = createMostSpecificOverloadDecider();
        OverloadRankingDto result = decider.inNormalMode(workItemDto, applicableOverloads, null);

        assertThat(result, is(third));
    }

    private OverloadRankingDto createOverloadRankingDto(IFunctionType overloadWithoutConvertible) {
        return new OverloadRankingDto(overloadWithoutConvertible, null, null, null, null, false, false);
    }

    protected IMostSpecificOverloadDecider createMostSpecificOverloadDecider() {
        return new MostSpecificOverloadDecider(mock(ISymbolFactory.class), mock(ITypeHelper.class));

    }
}
