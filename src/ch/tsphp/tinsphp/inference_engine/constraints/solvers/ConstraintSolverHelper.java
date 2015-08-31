/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints.solvers;

import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.TinsPHPConstants;
import ch.tsphp.tinsphp.common.inference.constraints.BoundException;
import ch.tsphp.tinsphp.common.inference.constraints.BoundResultDto;
import ch.tsphp.tinsphp.common.inference.constraints.EBindingCollectionMode;
import ch.tsphp.tinsphp.common.inference.constraints.FixedTypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.inference.constraints.OverloadApplicationDto;
import ch.tsphp.tinsphp.common.symbols.IContainerTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IIntersectionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IMinimalVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.IParametricTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IPolymorphicTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.common.utils.ITypeHelper;
import ch.tsphp.tinsphp.common.utils.MapHelper;
import ch.tsphp.tinsphp.common.utils.Pair;
import ch.tsphp.tinsphp.common.utils.TypeHelperDto;
import ch.tsphp.tinsphp.inference_engine.constraints.AggregateBindingDto;
import ch.tsphp.tinsphp.inference_engine.constraints.IMostSpecificOverloadDecider;
import ch.tsphp.tinsphp.inference_engine.constraints.OverloadRankingDto;
import ch.tsphp.tinsphp.inference_engine.constraints.WorkItemDto;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static ch.tsphp.tinsphp.common.utils.Pair.pair;

public class ConstraintSolverHelper implements IConstraintSolverHelper
{
    private final ISymbolFactory symbolFactory;
    private final ITypeHelper typeHelper;
    private final IMostSpecificOverloadDecider mostSpecificOverloadDecider;
    private final IDependencyConstraintSolver dependencyConstraintSolver;
    private final ITypeSymbol mixedTypeSymbol;

    private final Map<String, Set<String>> dependencies;
    private final Map<String, List<Pair<WorkItemDto, Integer>>> directDependencies;
    private final Map<String, Set<WorkItemDto>> unsolvedWorkItems;
    private final TypeSymbolComparator typeSymbolComparator;

    @SuppressWarnings("checkstyle:parameternumber")
    public ConstraintSolverHelper(
            ISymbolFactory theSymbolFactory,
            ITypeHelper theTypeHelper,
            IMostSpecificOverloadDecider theMostSpecificOverloadDecider,
            IDependencyConstraintSolver theDependencyConstraintSolver,
            Map<String, Set<String>> theDependencies,
            Map<String, List<Pair<WorkItemDto, Integer>>> theDirectDependencies,
            Map<String, Set<WorkItemDto>> theUnsolvedWorkItems) {
        symbolFactory = theSymbolFactory;
        typeHelper = theTypeHelper;
        mostSpecificOverloadDecider = theMostSpecificOverloadDecider;
        dependencyConstraintSolver = theDependencyConstraintSolver;
        dependencies = theDependencies;
        directDependencies = theDirectDependencies;
        unsolvedWorkItems = theUnsolvedWorkItems;
        mixedTypeSymbol = symbolFactory.getMixedTypeSymbol();
        typeSymbolComparator = new TypeSymbolComparator(typeHelper);
    }

    @Override
    public boolean createBindingsIfNecessary(
            WorkItemDto workItemDto, IVariable leftHandSide, List<IVariable> arguments) {

        int constantTypeCounter = 0;
        createBindingIfNecessary(workItemDto, leftHandSide);
        boolean atLeastOneBindingCreated = false;
        for (IVariable parameterVariable : arguments) {
            ECreateBinding status = createBindingIfNecessary(workItemDto, parameterVariable);
            switch (status) {
                case Created:
                    atLeastOneBindingCreated = true;
                    break;
                case ConstantType:
                    ++constantTypeCounter;
                    break;
                case NotCreated:
                    if (isIterativeAndHasType(workItemDto, parameterVariable)
                            || !parameterVariable.getName().startsWith("$")) {
                        ++constantTypeCounter;
                    }
                    break;
                default:
                    throw new IllegalStateException(status.name() + " is not yet covered by this switch");
            }
        }

        return atLeastOneBindingCreated || constantTypeCounter < arguments.size();
    }

    private boolean isIterativeAndHasType(WorkItemDto workItemDto, IVariable parameterVariable) {
        if (workItemDto.isInIterativeMode) {
            String absoluteName = parameterVariable.getAbsoluteName();
            IBindingCollection bindingCollection = workItemDto.bindingCollection;
            String typeVariable = bindingCollection.getTypeVariable(absoluteName);
            return bindingCollection.hasLowerTypeBounds(typeVariable)
                    || bindingCollection.hasUpperTypeBounds(typeVariable);
        }
        return false;
    }

    private ECreateBinding createBindingIfNecessary(WorkItemDto workItemDto, IVariable variable) {
        IBindingCollection bindings = workItemDto.bindingCollection;
        String absoluteName = variable.getAbsoluteName();
        ECreateBinding status = ECreateBinding.NotCreated;
        if (!bindings.containsVariable(absoluteName)) {
            status = ECreateBinding.Created;
            ITypeVariableReference reference = bindings.getNextTypeVariable();
            ITypeVariableReference typeVariableReference = reference;
            //if it is a literal then we know already the lower bound and it is a fix typed type variable
            ITypeSymbol typeSymbol = variable.getType();
            if (typeSymbol != null
                    && (workItemDto.isInSoftTypingMode
                    || !workItemDto.isSolvingMethod
                    || !variable.getName().startsWith("$"))) {
                typeVariableReference = new FixedTypeVariableReference(reference);
            }
            bindings.addVariable(absoluteName, typeVariableReference);
            if (typeSymbol != null) {
                String typeVariable = typeVariableReference.getTypeVariable();
                if (!workItemDto.isInSoftTypingMode
                        && workItemDto.isSolvingMethod
                        && variable.getName().startsWith("$")) {
                    bindings.addUpperTypeBound(typeVariable, typeSymbol);
                } else {
                    bindings.addLowerTypeBound(typeVariable, typeSymbol);
                }
                status = ECreateBinding.ConstantType;
            }
        }
        return status;
    }

    @Override
    public void solve(WorkItemDto workItemDto, IConstraint constraint) {
        boolean atLeastOneBindingCreated = createBindingsIfNecessary(
                workItemDto, constraint.getLeftHandSide(), constraint.getArguments());

        if (workItemDto.isSolvingMethod && atLeastOneBindingCreated) {
            addApplicableOverloadsToWorklist(workItemDto, constraint);
        } else {
            addMostSpecificOverloadToWorklist(workItemDto, constraint);
        }
    }

    private void addApplicableOverloadsToWorklist(WorkItemDto workItemDto, IConstraint constraint) {
        List<AggregateBindingDto> dtos = new ArrayList<>();

        boolean hasConvertible = false;
        boolean hasNonConvertible = false;
        for (IFunctionType overload : constraint.getMethodSymbol().getOverloads()) {
            try {
                AggregateBindingDto dto = solveOverLoad(workItemDto, constraint, overload);
                if (dto != null) {
                    if (overload.wasSimplified()) {
                        if (overload.hasConvertibleParameterTypes()) {
                            hasConvertible = true;
                        } else if (dto.implicitConversions == null) {
                            hasNonConvertible = true;
                        }
                    }
                    dtos.add(dto);
                }
            } catch (BoundException ex) {
                //That is ok, we will deal with it in solveConstraints
            }
        }

        boolean isAlreadyAnalysingConvertible = workItemDto.convertibleAnalysisDto.isAnalysingConvertible;
        for (AggregateBindingDto dto : dtos) {
            //we do not create overloads with implicit conversions here if an overload with convertibles exists
            if (dto != null && (dto.implicitConversions == null || !hasConvertible)) {
                if (!isAlreadyAnalysingConvertible || !hasConvertible || !hasNonConvertible) {
                    if (dto.overload.wasSimplified() && dto.overload.hasConvertibleParameterTypes()) {
                        workItemDto.convertibleAnalysisDto.isAnalysingConvertible = true;
                    }
                    workItemDto.workDeque.add(nextWorkItemDto(dto));
                } else {
                    int numberOfConvertibleApplications
                            = workItemDto.bindingCollection.getNumberOfConvertibleApplications();
                    boolean hasConvertibleParameterTypes = dto.overload.hasConvertibleParameterTypes();
                    if (numberOfConvertibleApplications > 0 && hasConvertibleParameterTypes
                            || numberOfConvertibleApplications == 0 && !hasConvertibleParameterTypes) {
                        workItemDto.workDeque.add(nextWorkItemDto(dto));
                    }
                }
            }
        }
    }

    private WorkItemDto nextWorkItemDto(AggregateBindingDto dto) {
        return nextWorkItemDto(dto.workItemDto, dto.bindings, dto.helperVariableMapping, dto.hasChanged);
    }

    private WorkItemDto nextWorkItemDto(
            WorkItemDto workItemDto,
            IBindingCollection bindings,
            Map<Integer, Map<String, ITypeVariableReference>> helperVariableMapping,
            boolean hasChanged) {
        int pointer;
        if (!workItemDto.isSolvingDependency || workItemDto.isInIterativeMode) {
            pointer = workItemDto.pointer + 1;
        } else {
            pointer = Integer.MAX_VALUE;
        }
        return new WorkItemDto(workItemDto, pointer, bindings, helperVariableMapping, hasChanged);
    }

    private AggregateBindingDto solveOverLoad(
            WorkItemDto workItemDto,
            IConstraint constraint,
            IFunctionType overload) {

        AggregateBindingDto dto = null;
        if (constraint.getArguments().size() >= overload.getNumberOfNonOptionalParameters()) {
            IBindingCollection bindings = symbolFactory.createBindingCollection(workItemDto.bindingCollection);
            dto = new AggregateBindingDto(constraint, overload, bindings, workItemDto);
            aggregateBinding(dto);
        }
        return dto;
    }

    @Override
    public void aggregateBinding(AggregateBindingDto dto) {
        List<IVariable> arguments = dto.constraint.getArguments();
        List<IVariable> parameters = dto.overload.getParameters();
        int numberOfParameters = parameters.size();

        dto.mapping = new HashMap<>(numberOfParameters + 1);
        if (dto.helperVariableMapping != null && dto.helperVariableMapping.containsKey(dto.workItemDto.pointer)) {
            dto.mapping.putAll(dto.helperVariableMapping.get(dto.workItemDto.pointer));
        }

        int numberOfArguments = arguments.size();
        int count = numberOfParameters <= numberOfArguments ? numberOfParameters : numberOfArguments;

        dto.needToReIterate = true;
        while (dto.needToReIterate) {
            dto.needToReIterate = false;

            IVariable leftHandSide = dto.constraint.getLeftHandSide();
            dto.bindingVariable = leftHandSide;
            dto.overloadVariableId = TinsPHPConstants.RETURN_VARIABLE_NAME;
            dto.argumentNumber = null;
            boolean hasNarrowed = dto.hasNarrowedArguments;
            mergeTypeVariables(dto);
            //reset because we are only interested in whether an argument was narrowed
            dto.hasNarrowedArguments = hasNarrowed;

            boolean argumentsAreAllFixed = true;
            for (int i = 0; i < count; ++i) {
                dto.bindingVariable = arguments.get(i);
                dto.overloadVariableId = parameters.get(i).getAbsoluteName();
                dto.argumentNumber = i;
                mergeTypeVariables(dto);
                argumentsAreAllFixed = argumentsAreAllFixed
                        && dto.bindings.getTypeVariableReference(dto.bindingVariable.getAbsoluteName()).hasFixedType();
            }

            if (!dto.needToReIterate) {

                applyTypeParameterConstraints(dto);

                String lhsAbsoluteName = leftHandSide.getAbsoluteName();
                ITypeVariableReference reference = dto.bindings.getTypeVariableReference(lhsAbsoluteName);
                if (!reference.hasFixedType() && argumentsAreAllFixed && !dto.workItemDto.isInSoftTypingMode) {
                    dto.bindings.fixType(lhsAbsoluteName);
                }
                if (!dto.workItemDto.isInSoftTypingMode
                        && !lhsAbsoluteName.equals(TinsPHPConstants.RETURN_VARIABLE_NAME)) {
                    OverloadApplicationDto overloadApplicationDto = new OverloadApplicationDto(
                            dto.overload, dto.implicitConversions, null);
                    dto.bindings.setAppliedOverload(lhsAbsoluteName, overloadApplicationDto);
                }
            }
            if (dto.iterateCount > 1) {
                throw new IllegalStateException("overload uses type variables "
                        + "which are not part of the signature.");
            }
            ++dto.iterateCount;
        }
    }

    private void mergeTypeVariables(AggregateBindingDto dto) {
        String bindingVariableName = dto.bindingVariable.getAbsoluteName();
        ITypeVariableReference bindingTypeVariableReference
                = dto.bindings.getTypeVariableReference(bindingVariableName);
        IBindingCollection rightBindings = dto.overload.getBindingCollection();
        String overloadTypeVariable
                = rightBindings.getTypeVariableReference(dto.overloadVariableId).getTypeVariable();

        String lhsTypeVariable;
        if (dto.mapping.containsKey(overloadTypeVariable)) {
            lhsTypeVariable = dto.mapping.get(overloadTypeVariable).getTypeVariable();
            String rhsTypeVariable = bindingTypeVariableReference.getTypeVariable();
            dto.bindings.mergeFirstIntoSecond(rhsTypeVariable, lhsTypeVariable);

            //TINS-535 improve precision in soft typing for unconstrained parameters
//            if (dto.workItemDto.isInSoftTypingMode
//                    && dto.workItemDto.param2LowerParams.containsKey(rhsTypeVariable)
//                    && !rhsTypeVariable.equals(lhsTypeVariable)) {
//                List<String> params = dto.workItemDto.param2LowerParams.remove(rhsTypeVariable);
//                if (!dto.workItemDto.param2LowerParams.containsKey(lhsTypeVariable)) {
//                    dto.workItemDto.param2LowerParams.put(lhsTypeVariable, params);
//                } else {
//                    dto.workItemDto.param2LowerParams.get(lhsTypeVariable).addAll(params);
//                }
//            }
        } else {
            lhsTypeVariable = bindingTypeVariableReference.getTypeVariable();
            dto.mapping.put(overloadTypeVariable, bindingTypeVariableReference);
        }

        applyRightToLeft(dto, lhsTypeVariable, overloadTypeVariable);
    }

    private void applyRightToLeft(AggregateBindingDto dto, String left, String right) {

        IBindingCollection leftBindings = dto.bindings;
        IBindingCollection rightBindings = dto.overload.getBindingCollection();

        if (rightBindings.hasUpperTypeBounds(right)) {
            ITypeVariableReference reference
                    = dto.bindings.getTypeVariableReference(dto.bindingVariable.getAbsoluteName());
            if (!dto.workItemDto.isInSoftTypingMode || !reference.hasFixedType()) {
                IIntersectionTypeSymbol rightUpperTypeBounds = rightBindings.getUpperTypeBounds(right);
                ITypeSymbol copy = copyIfNotFixed(rightUpperTypeBounds, dto);
                if (copy != null) {
                    if (!dto.workItemDto.isInSoftTypingMode) {
                        BoundResultDto result = leftBindings.addUpperTypeBound(left, copy);
                        updateAggregateBindingDto(dto, result, copy);
                    } else if (!typeHelper.areSame(copy, mixedTypeSymbol)) {
                        BoundResultDto result = leftBindings.addLowerTypeBound(left, copy);
                        updateAggregateBindingDto(dto, result, copy);
                    }
                }
            }
        }

        if (rightBindings.hasLowerTypeBounds(right)) {
            IUnionTypeSymbol rightLowerTypeBounds = rightBindings.getLowerTypeBounds(right);
            ITypeSymbol copy = copyIfNotFixed(rightLowerTypeBounds, dto);
            if (copy != null) {
                BoundResultDto result = leftBindings.addLowerTypeBound(left, copy);
                updateAggregateBindingDto(dto, result, copy);
            }
        }

        if (rightBindings.hasLowerRefBounds(right)) {
            for (String refTypeVariable : rightBindings.getLowerRefBounds(right)) {
                if (dto.mapping.containsKey(refTypeVariable)) {
                    BoundResultDto resultDto = leftBindings.addLowerRefBound(left, dto.mapping.get(refTypeVariable));
                    dto.hasChanged = dto.hasChanged || resultDto.hasChanged;
                } else if (!dto.workItemDto.isInIterativeMode && dto.iterateCount == 1) {
                    ITypeVariableReference typeVariableReference = addHelperVariable(dto, refTypeVariable);
                    leftBindings.addLowerRefBound(left, typeVariableReference);
                    dto.hasChanged = true; //we add a new type variable, it has changed
                } else if (dto.workItemDto.isInIterativeMode && dto.iterateCount == 1) {
                    addLowerRefInIterativeMode(dto, left, refTypeVariable);
                } else {
                    dto.needToReIterate = true;
                    break;
                }
            }
        }
    }

    private IPolymorphicTypeSymbol copyIfNotFixed(IContainerTypeSymbol containerTypeSymbol, AggregateBindingDto dto) {

        IPolymorphicTypeSymbol copy = containerTypeSymbol;
        if (!containerTypeSymbol.isFixed()) {

            IBindingCollection leftBindings = dto.bindings;
            Deque<IParametricTypeSymbol> parametricTypeSymbols = new ArrayDeque<>();
            copy = containerTypeSymbol.copy(parametricTypeSymbols);

            parametricTypes:
            for (IParametricTypeSymbol parametricTypeSymbol : parametricTypeSymbols) {
                List<String> typeParameters = new ArrayList<>();
                if (!dto.workItemDto.isInSoftTypingMode) {
                    for (String typeParameter : parametricTypeSymbol.getTypeParameters()) {
                        ITypeVariableReference reference;
                        if (dto.mapping.containsKey(typeParameter)) {
                            reference = dto.mapping.get(typeParameter);
                        } else if (!dto.workItemDto.isInIterativeMode && dto.iterateCount == 1) {
                            reference = addHelperVariable(dto, typeParameter);
                        } else if (dto.workItemDto.isInIterativeMode) {
                            int key = dto.workItemDto.pointer;
                            Map<String, ITypeVariableReference> mapping;
                            if (dto.helperVariableMapping != null && dto.helperVariableMapping.containsKey(key)) {
                                mapping = dto.helperVariableMapping.get(key);
                                reference = mapping.get(typeParameter);
                                if (reference == null) {
                                    reference = searchTypeParameter(dto, mapping, typeParameter);
                                    if (reference == null) {
                                        IBindingCollection copyBindings = symbolFactory.createBindingCollection(
                                                parametricTypeSymbol.getBindingCollection());
                                        copyBindings.bind(parametricTypeSymbol,
                                                parametricTypeSymbol.getTypeParameters());
                                        copyBindings.fixTypeParameters();
                                        continue parametricTypes;
                                    }
                                } else {
                                    applyRightToLeft(dto, reference.getTypeVariable(), typeParameter);
                                }
                            } else {
                                mapping = new HashMap<>(2);
                                reference = addHelperVariable(dto, typeParameter);
                                mapping.put(typeParameter, reference);
                                if (dto.helperVariableMapping == null) {
                                    dto.helperVariableMapping = new HashMap<>();
                                }
                                dto.helperVariableMapping.put(key, mapping);
                            }
                        } else {
                            dto.needToReIterate = true;
                            copy = null;
                            break parametricTypes;
                        }
                        typeParameters.add(reference.getTypeVariable());
                    }
                    leftBindings.bind(parametricTypeSymbol, typeParameters);
                } else {
                    IBindingCollection copyBindings
                            = symbolFactory.createBindingCollection(parametricTypeSymbol.getBindingCollection());
                    copyBindings.bind(parametricTypeSymbol, parametricTypeSymbol.getTypeParameters());
                    copyBindings.fixTypeParameters();
                }
            }
        }

        return copy;
    }

    private ITypeVariableReference searchTypeParameter(
            AggregateBindingDto dto, Map<String, ITypeVariableReference> mapping, String typeParameter) {
        ITypeVariableReference reference = null;
        IBindingCollection rightBindings = dto.overload.getBindingCollection();
        if (rightBindings.hasLowerRefBounds(typeParameter)) {
            for (String refTypeParameter : rightBindings.getLowerRefBounds(typeParameter)) {
                reference = mapping.get(refTypeParameter);
                if (reference != null) {
                    break;
                }
            }
        }
        return reference;
    }

    private void addLowerRefInIterativeMode(
            AggregateBindingDto dto, String left, String refTypeVariable) {
        IBindingCollection rightBindings = dto.overload.getBindingCollection();
        if (rightBindings.hasLowerRefBounds(refTypeVariable)) {
            for (String refRefTypeVariable : rightBindings.getLowerRefBounds(refTypeVariable)) {
                if (dto.mapping.containsKey(refRefTypeVariable)) {
                    BoundResultDto resultDto = dto.bindings.addLowerRefBound(left, dto.mapping.get(refRefTypeVariable));
                    dto.hasChanged = dto.hasChanged || resultDto.hasChanged;
                }
                addLowerRefInIterativeMode(dto, left, refRefTypeVariable);
            }
        }
    }

    private ITypeVariableReference addHelperVariable(AggregateBindingDto dto, String typeParameter) {
        ITypeVariableReference typeVariableReference = dto.bindings.createHelperVariable();
        dto.mapping.put(typeParameter, typeVariableReference);

        String typeVariable = typeVariableReference.getTypeVariable();
        applyRightToLeft(dto, typeVariable, typeParameter);

        return typeVariableReference;
    }

    private void updateAggregateBindingDto(AggregateBindingDto dto, BoundResultDto resultDto, ITypeSymbol copy) {
        dto.hasChanged = dto.hasChanged || resultDto.hasChanged;

        if (resultDto.usedImplicitConversion) {
            if (dto.argumentNumber != null) {
                if (dto.implicitConversions == null) {
                    dto.implicitConversions = new HashMap<>();
                }
                dto.implicitConversions.put(dto.argumentNumber, pair(copy, resultDto.implicitConversionProvider));
            }
            //implicit conversions always narrow
            dto.hasNarrowedArguments = true;
        } else if (resultDto.hasChanged) {
            dto.hasNarrowedArguments = true;
        }

        if (resultDto.lowerConstraints != null) {
            if (dto.lowerConstraints == null) {
                dto.lowerConstraints = new HashMap<>();
            }
            aggregateConstraintsFromTo(resultDto.lowerConstraints, dto.lowerConstraints);
        }

        if (resultDto.upperConstraints != null) {
            if (dto.upperConstraints == null) {
                dto.upperConstraints = new HashMap<>();
            }
            aggregateConstraintsFromTo(resultDto.upperConstraints, dto.upperConstraints);
        }
    }

    private void aggregateConstraintsFromTo(
            Map<String, Set<ITypeSymbol>> from, Map<String, SortedSet<ITypeSymbol>> to) {
        for (Map.Entry<String, Set<ITypeSymbol>> entry : from.entrySet()) {
            String typeVariable = entry.getKey();
            Set<ITypeSymbol> typeBoundConstraints = entry.getValue();
            ITypeSymbol typeBound;
            if (typeBoundConstraints.size() == 1) {
                typeBound = typeBoundConstraints.iterator().next();
                while (typeBound instanceof IContainerTypeSymbol) {
                    Map<String, ITypeSymbol> typeSymbols = ((IContainerTypeSymbol) typeBound).getTypeSymbols();
                    if (typeSymbols.size() == 1) {
                        typeBound = typeSymbols.values().iterator().next();
                    } else {
                        break;
                    }
                }
            } else {
                IUnionTypeSymbol union = symbolFactory.createUnionTypeSymbol();
                for (ITypeSymbol typeSymbol : typeBoundConstraints) {
                    union.addTypeSymbol(typeSymbol);
                }
                typeBound = union;
            }

            SortedSet<ITypeSymbol> set = to.get(typeVariable);
            if (set == null) {
                set = new TreeSet<>(typeSymbolComparator);
                to.put(typeVariable, set);
            }
            set.add(typeBound);
        }
    }

    private void applyTypeParameterConstraints(AggregateBindingDto dto) {

        if (dto.lowerConstraints != null) {
            for (Map.Entry<String, SortedSet<ITypeSymbol>> entry : dto.lowerConstraints.entrySet()) {
                String typeParameter = entry.getKey();
                SortedSet<ITypeSymbol> typeSymbols = entry.getValue();
                Iterator<ITypeSymbol> iterator = typeSymbols.iterator();
                if (!dto.bindings.hasLowerTypeBounds(typeParameter)) {
                    ITypeSymbol typeSymbol = iterator.next();
                    //TODO rstoll TINS-600 - function instantiation with convertibles too general
                    BoundResultDto resultDto = dto.bindings.addLowerTypeBound(typeParameter, typeSymbol);
                    dto.hasNarrowedArguments = dto.hasNarrowedArguments || resultDto.hasChanged;
                }
                boolean hasChanged = applyLowerTypeConstraints(dto, typeParameter, iterator);
                dto.hasNarrowedArguments = dto.hasNarrowedArguments || hasChanged;
            }
        }

        if (dto.upperConstraints != null) {
            for (Map.Entry<String, SortedSet<ITypeSymbol>> entry : dto.upperConstraints.entrySet()) {
                String typeParameter = entry.getKey();
                for (ITypeSymbol typeSymbol : entry.getValue()) {
                    BoundResultDto resultDto = dto.bindings.addUpperTypeBound(typeParameter, typeSymbol);
                    dto.hasNarrowedArguments = dto.hasNarrowedArguments || resultDto.hasChanged;
                }
            }
        }
    }

    private boolean applyLowerTypeConstraints(
            AggregateBindingDto dto, String typeParameter, Iterator<ITypeSymbol> iterator) {

        boolean hasChanged = false;

        while (iterator.hasNext()) {

            IUnionTypeSymbol currentLowerTypeBounds = dto.bindings.getLowerTypeBounds(typeParameter);
            ITypeSymbol typeSymbol = iterator.next();

            boolean isSingleType = true;
            if (typeSymbol instanceof IContainerTypeSymbol) {
                isSingleType = ((IContainerTypeSymbol) typeSymbol).getTypeSymbols().size() == 1;
            }
            if (isSingleType) {
                hasChanged = applySingleLowerTypeConstraint(
                        dto, typeParameter, currentLowerTypeBounds, typeSymbol);
            } else {
                IContainerTypeSymbol containerTypeSymbol = (IContainerTypeSymbol) typeSymbol;
                for (ITypeSymbol innerTypeSymbol : containerTypeSymbol.getTypeSymbols().values()) {
                    TypeHelperDto result = typeHelper.isFirstSameOrSubTypeOfSecond(
                            innerTypeSymbol, currentLowerTypeBounds);

                    switch (result.relation) {
                        case HAS_NO_RELATION:
                            BoundResultDto resultDto = dto.bindings.addLowerTypeBound(typeParameter, innerTypeSymbol);
                            hasChanged = resultDto.hasChanged;
                            break;
                        case HAS_RELATION:
                        case HAS_COERCIVE_RELATION:
                        default:
                            //nothing to do
                            break;
                    }
                }
            }
        }
        return hasChanged;
    }

    private boolean applySingleLowerTypeConstraint(
            AggregateBindingDto dto, String typeParameter, IUnionTypeSymbol currentLowerTypeBounds,
            ITypeSymbol typeSymbol) {
        boolean hasChanged = false;

        EBindingCollectionMode originalMode = dto.bindings.getMode();

        TypeHelperDto result = typeHelper.isFirstSameOrSubTypeOfSecond(currentLowerTypeBounds, typeSymbol);
        switch (result.relation) {
            case HAS_RELATION:
                BoundResultDto resultDto = dto.bindings.addLowerTypeBound(typeParameter, typeSymbol);
                hasChanged = resultDto.hasChanged;
                break;
            case HAS_COERCIVE_RELATION:
                if (!dto.bindings.hasUpperRefBounds(typeParameter)) {
                    dto.bindings.setMode(EBindingCollectionMode.Modification);
                    IUnionTypeSymbol newLowerTypeBounds = symbolFactory.createUnionTypeSymbol();
                    newLowerTypeBounds.addTypeSymbol(typeSymbol);
                    dto.bindings.setLowerTypeBounds(typeParameter, newLowerTypeBounds);
                    dto.bindings.setMode(originalMode);
                } else {
                    // if type parameter has upper type bounds (it is used in by-ref semantics)
                    // then we need to add it instead of replacing it
                    resultDto = dto.bindings.addLowerTypeBound(typeParameter, typeSymbol);
                    hasChanged = resultDto.hasChanged;
                }
                break;
            default:
                result = typeHelper.isFirstSameOrSubTypeOfSecond(typeSymbol, currentLowerTypeBounds);
                switch (result.relation) {
                    case HAS_RELATION:
                        //no need to add it
                        break;
                    case HAS_COERCIVE_RELATION:
                        //no need to add it
                        break;
                    default:
                        resultDto = dto.bindings.addLowerTypeBound(typeParameter, typeSymbol);
                        hasChanged = resultDto.hasChanged;
                        break;
                }
                break;
        }
        return hasChanged;
    }

    @Override
    public void addMostSpecificOverloadToWorklist(WorkItemDto workItemDto, IConstraint constraint) {
        Collection<IFunctionType> overloads = constraint.getMethodSymbol().getOverloads();
        if (overloads.size() > 1) {
            List<IVariable> arguments = constraint.getArguments();
            List<ITypeSymbol> argumentTypes = calculateArgumentTypes(workItemDto, arguments);

            List<OverloadRankingDto> applicableOverloads
                    = getApplicableOverloads(workItemDto, constraint, overloads, argumentTypes);

            int numberOfApplicableOverloads = applicableOverloads.size();
            if (numberOfApplicableOverloads > 0) {
                OverloadRankingDto overloadRankingDto = applicableOverloads.get(0);
                if (numberOfApplicableOverloads > 1) {
                    overloadRankingDto = mostSpecificOverloadDecider.inNormalMode(
                            workItemDto, applicableOverloads, argumentTypes);
                }
                workItemDto.workDeque.add(nextWorkItemDto(
                        workItemDto,
                        overloadRankingDto.bindings,
                        overloadRankingDto.helperVariableMapping,
                        overloadRankingDto.hasChanged));
            }
        } else {
            try {
                IFunctionType overload = overloads.iterator().next();
                AggregateBindingDto dto = solveOverLoad(workItemDto, constraint, overload);
                if (dto != null) {
                    workItemDto.workDeque.add(nextWorkItemDto(dto));
                }
            } catch (BoundException ex) {
                //that's ok, we will report an error in soft typing if it should still exists there
            }
        }
    }

    private List<ITypeSymbol> calculateArgumentTypes(WorkItemDto workItemDto, List<IVariable> arguments) {
        List<ITypeSymbol> argumentTypes = new ArrayList<>(arguments.size());

        IBindingCollection bindingCollection = workItemDto.bindingCollection;
        for (IVariable variable : arguments) {
            String argumentId = variable.getAbsoluteName();
            String typeVariable = bindingCollection.getTypeVariable(argumentId);

            //we prefer the upper type bound and when solving methods and the lower type bound when solving global
            // namespace scope
            IContainerTypeSymbol argumentType = null;
            if (workItemDto.isSolvingMethod && bindingCollection.hasUpperTypeBounds(typeVariable)) {
                argumentType = bindingCollection.getUpperTypeBounds(typeVariable);
            } else if (bindingCollection.hasLowerTypeBounds(typeVariable)) {
                argumentType = bindingCollection.getLowerTypeBounds(typeVariable);
            } else if (!workItemDto.isSolvingMethod && bindingCollection.hasUpperTypeBounds(typeVariable)) {
                argumentType = bindingCollection.getUpperTypeBounds(typeVariable);
            }

            if (argumentType != null && !argumentType.isFixed()) {
                //overload has non fixed arguments - since we manipulate it we have to copy it first...
                bindingCollection = symbolFactory.createBindingCollection(bindingCollection);
                //.. then we fix all bounded types (in a brute force way - overloadBinding cannot be used for
                // constraint solving afterwards)
                bindingCollection.fixTypeParameters();
            }
            argumentTypes.add(argumentType);
        }
        return argumentTypes;
    }

    private List<OverloadRankingDto> getApplicableOverloads(
            WorkItemDto workItemDto,
            IConstraint constraint,
            Collection<IFunctionType> overloads,
            List<ITypeSymbol> argumentTypes) {

        List<OverloadRankingDto> applicableOverloads = new ArrayList<>();
        List<IVariable> arguments = constraint.getArguments();
        int numberOfArguments = arguments.size();

        for (IFunctionType overload : overloads) {
            if (numberOfArguments >= overload.getNumberOfNonOptionalParameters()) {
                try {
                    IBindingCollection bindings = symbolFactory.createBindingCollection(workItemDto.bindingCollection);
                    AggregateBindingDto dto = new AggregateBindingDto(constraint, overload, bindings, workItemDto);
                    aggregateBinding(dto);

                    //no need to check for one to one match if arguments were narrowed unless we solve constraints of
                    // the global default namespace scope - moreover, if the overload is not fixed,
                    // then we cannot use the check since there might be a fixed correspondence
                    // and last but not least, we cannot check whether an overload was fixed if it was not simplified
                    // yet which is only the case for indirect recursive functions which are still analysed (hence we
                    // are in iterative mode)
                    boolean isOneToOne = overload.wasSimplified() && overload.isFixed()
                            && (!dto.hasNarrowedArguments || !workItemDto.isSolvingMethod);

                    if (isOneToOne) {
                        isOneToOne = isOneToOneMatch(numberOfArguments, argumentTypes, overload);
                        if (isOneToOne) {
                            //reset list, we will break bellow
                            applicableOverloads = new ArrayList<>(1);
                        }
                    }

                    applicableOverloads.add(new OverloadRankingDto(dto));

                    if (isOneToOne) {
                        break;
                    }
                } catch (BoundException ex) {
                    //That is ok, we are looking for applicable overloads
                }
            }
        }
        return applicableOverloads;
    }

    private boolean isOneToOneMatch(int numberOfArguments, List<ITypeSymbol> argumentTypes, IFunctionType overload) {
        boolean isOneToOne = true;
        IBindingCollection rightBindings = overload.getBindingCollection();

        List<IVariable> parameters = overload.getParameters();
        int numberOfParameters = parameters.size();
        int count = numberOfParameters <= numberOfArguments ? numberOfParameters : numberOfArguments;
        for (int i = 0; i < count; ++i) {
            IVariable variable = parameters.get(i);
            String argumentId = variable.getAbsoluteName();
            String typeVariable = rightBindings.getTypeVariable(argumentId);
            ITypeSymbol argumentType = argumentTypes.get(i);
            if (rightBindings.hasUpperTypeBounds(typeVariable)) {
                IIntersectionTypeSymbol parameterType = rightBindings.getUpperTypeBounds(typeVariable);
                if (argumentType == null) {
                    argumentType = mixedTypeSymbol;
                }
                if (!typeHelper.areSame(argumentType, parameterType)) {
                    isOneToOne = false;
                    break;
                }
            } else if (argumentType != null) {
                isOneToOne = false;
                break;
            }
        }
        return isOneToOne;
    }

    @Override
    public void createDependencies(WorkItemDto workItemDto) {
        List<IConstraint> constraints = workItemDto.constraintCollection.getConstraints();
        String absoluteName = workItemDto.constraintCollection.getAbsoluteName();

        for (Integer pointer : workItemDto.dependentConstraints) {
            String refAbsoluteName = constraints.get(pointer).getMethodSymbol().getAbsoluteName();
            MapHelper.addToListInMap(directDependencies, refAbsoluteName, pair(workItemDto, pointer));
            MapHelper.addToSetInMap(dependencies, refAbsoluteName, absoluteName);
        }
        MapHelper.addToSetInMap(
                unsolvedWorkItems,
                absoluteName,
                workItemDto);
    }

    @Override
    public void finishingMethodConstraints(IMethodSymbol methodSymbol) {
        createOverloads(methodSymbol);
        solveDependentConstraints(methodSymbol);
    }

    @Override
    public void solveDependentConstraints(IMethodSymbol methodSymbol) {
        String methodName = methodSymbol.getAbsoluteName();
        if (directDependencies.containsKey(methodName)) {
            dependencies.remove(methodName);
            for (Pair<WorkItemDto, Integer> element : directDependencies.remove(methodName)) {
                dependencyConstraintSolver.solveDependency(element);
            }
        }
        unsolvedWorkItems.remove(methodName);
    }

    private void createOverloads(IMethodSymbol methodSymbol) {
        for (IBindingCollection bindings : methodSymbol.getBindings()) {
            createOverload(methodSymbol, bindings);
        }
    }

    @Override
    public IFunctionType createOverload(IMethodSymbol methodSymbol, IBindingCollection bindings) {
        List<IVariable> parameters = new ArrayList<>();
        for (IVariableSymbol parameter : methodSymbol.getParameters()) {
            String parameterId = parameter.getAbsoluteName();
            if (!bindings.containsVariable(parameterId)) {
                //the parameter is not used at all, hence it can be mixed
                ITypeVariableReference reference = new FixedTypeVariableReference(bindings.getNextTypeVariable());
                bindings.addVariable(parameterId, reference);
                bindings.addUpperTypeBound(reference.getTypeVariable(), mixedTypeSymbol);
                //TODO could generate a warning
            }
            IMinimalVariableSymbol parameterVariable = symbolFactory.createMinimalVariableSymbol(
                    parameter.getDefinitionAst(), parameter.getName());
            parameterVariable.setDefinitionScope(parameter.getDefinitionScope());
            parameterVariable.setType(parameter.getType());
            parameters.add(parameterVariable);
        }

        IFunctionType functionType = symbolFactory.createFunctionType(methodSymbol.getName(), bindings, parameters);
        functionType.simplify();
        methodSymbol.addOverload(functionType);
        return functionType;
    }

    private enum ECreateBinding
    {
        NotCreated,
        ConstantType,
        Created,
    }

    private static class TypeSymbolComparator implements Comparator<ITypeSymbol>
    {
        private ITypeHelper typeHelper;

        public TypeSymbolComparator(ITypeHelper theTypeHelper) {
            typeHelper = theTypeHelper;
        }

        @Override
        public int compare(ITypeSymbol typeSymbolA, ITypeSymbol typeSymbolB) {
            if (!typeHelper.areSame(typeSymbolA, typeSymbolB)) {
                int numberA = 1;
                if (typeSymbolA instanceof IContainerTypeSymbol) {
                    numberA = ((IContainerTypeSymbol) typeSymbolA).getTypeSymbols().size();
                }
                int numberB = 1;
                if (typeSymbolB instanceof IContainerTypeSymbol) {
                    numberB = ((IContainerTypeSymbol) typeSymbolB).getTypeSymbols().size();
                }
                if (numberB - numberA <= 0) {
                    return 1;
                }
                return -1;
            }
            return 0;
        }
    }

}
