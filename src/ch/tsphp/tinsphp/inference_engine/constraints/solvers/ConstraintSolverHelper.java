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
import ch.tsphp.tinsphp.common.inference.constraints.EOverloadBindingsMode;
import ch.tsphp.tinsphp.common.inference.constraints.FixedTypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraint;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IOverloadBindings;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableReference;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.inference.constraints.OverloadApplicationDto;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
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
import ch.tsphp.tinsphp.inference_engine.constraints.WorklistDto;

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
    private final IInferenceIssueReporter issueReporter;
    private final IMostSpecificOverloadDecider mostSpecificOverloadDecider;
    private final IDependencyConstraintSolver dependencyConstraintSolver;
    private final ITypeSymbol mixedTypeSymbol;

    private final Map<String, Set<String>> dependencies;
    private final Map<String, List<Pair<WorklistDto, Integer>>> directDependencies;
    private final Map<String, Set<WorklistDto>> unsolvedConstraints;
    private final TypeSymbolComparator typeSymbolComparator;

    @SuppressWarnings("checkstyle:parameternumber")
    public ConstraintSolverHelper(
            ISymbolFactory theSymbolFactory,
            ITypeHelper theTypeHelper,
            IInferenceIssueReporter theIssueReporter,
            IMostSpecificOverloadDecider theMostSpecificOverloadDecider,
            IDependencyConstraintSolver theDependencyConstraintSolver,
            Map<String, Set<String>> theDependencies,
            Map<String, List<Pair<WorklistDto, Integer>>> theDirectDependencies,
            Map<String, Set<WorklistDto>> theUnsolvedConstraints) {
        symbolFactory = theSymbolFactory;
        typeHelper = theTypeHelper;
        issueReporter = theIssueReporter;
        mostSpecificOverloadDecider = theMostSpecificOverloadDecider;
        dependencyConstraintSolver = theDependencyConstraintSolver;
        dependencies = theDependencies;
        directDependencies = theDirectDependencies;
        unsolvedConstraints = theUnsolvedConstraints;
        mixedTypeSymbol = symbolFactory.getMixedTypeSymbol();
        typeSymbolComparator = new TypeSymbolComparator(typeHelper);
    }

    @Override
    public boolean createBindingsIfNecessary(
            WorklistDto worklistDto, IVariable leftHandSide, List<IVariable> arguments) {

        int constantTypeCounter = 0;
        createBindingIfNecessary(worklistDto, leftHandSide);
        boolean atLeastOneBindingCreated = false;
        for (IVariable parameterVariable : arguments) {
            ECreateBinding status = createBindingIfNecessary(worklistDto, parameterVariable);
            switch (status) {
                case Created:
                    atLeastOneBindingCreated = true;
                    break;
                case ConstantType:
                    ++constantTypeCounter;
                    break;
                case NotCreated:
                    if (worklistDto.isInIterativeMode || !parameterVariable.getName().startsWith("$")) {
                        ++constantTypeCounter;
                    }
                    break;
                default:
                    throw new IllegalStateException(status.name() + " is not yet covered by this switch");
            }
        }

        return atLeastOneBindingCreated || constantTypeCounter < arguments.size();
    }

    private ECreateBinding createBindingIfNecessary(WorklistDto worklistDto, IVariable variable) {
        IOverloadBindings bindings = worklistDto.overloadBindings;
        String absoluteName = variable.getAbsoluteName();
        ECreateBinding status = ECreateBinding.NotCreated;
        if (!bindings.containsVariable(absoluteName)) {
            status = ECreateBinding.Created;
            ITypeVariableReference reference = bindings.getNextTypeVariable();
            ITypeVariableReference typeVariableReference = reference;
            //if it is a literal then we know already the lower bound and it is a fix typed type variable
            ITypeSymbol typeSymbol = variable.getType();
            if (typeSymbol != null
                    && (worklistDto.isInSoftTypingMode
                    || !worklistDto.isSolvingMethod
                    || !variable.getName().startsWith("$"))) {
                typeVariableReference = new FixedTypeVariableReference(reference);
            }
            bindings.addVariable(absoluteName, typeVariableReference);
            if (typeSymbol != null) {
                String typeVariable = typeVariableReference.getTypeVariable();
                //TODO rstoll TINS-407 - store fixed type only in lower bound
                //TODO rstoll TINS-387 function application only consider upper bounds
                if (!worklistDto.isInSoftTypingMode && worklistDto.isSolvingMethod
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
    public void solve(WorklistDto worklistDto, IConstraint constraint) {
        boolean atLeastOneBindingCreated = createBindingsIfNecessary(
                worklistDto, constraint.getLeftHandSide(), constraint.getArguments());

        if (worklistDto.isSolvingMethod && atLeastOneBindingCreated) {
            addApplicableOverloadsToWorklist(worklistDto, constraint);
        } else {
            addMostSpecificOverloadToWorklist(worklistDto, constraint);
        }
    }

    private void addApplicableOverloadsToWorklist(WorklistDto worklistDto, IConstraint constraint) {
        List<AggregateBindingDto> dtos = new ArrayList<>();

        boolean hasConvertible = false;
        boolean hasNonConvertible = false;
        for (IFunctionType overload : constraint.getMethodSymbol().getOverloads()) {
            try {
                AggregateBindingDto dto = solveOverLoad(worklistDto, constraint, overload);
                if (dto != null) {
                    if (overload.wasSimplified()) {
                        if (overload.hasConvertibleParameterTypes()) {
                            hasConvertible = true;
                        } else {
                            hasNonConvertible = true;
                        }
                    }
                    dtos.add(dto);
                }
            } catch (BoundException ex) {
                //That is ok, we will deal with it in solveConstraints
            }
        }

        boolean isAlreadyAnalysingConvertible = worklistDto.convertibleAnalysisDto.isAnalysingConvertible;
        for (AggregateBindingDto dto : dtos) {
            //we do not create overloads with implicit conversions here.
            if (dto != null && dto.implicitConversions == null) {
                if (!isAlreadyAnalysingConvertible || !hasConvertible || !hasNonConvertible) {
                    if (dto.overload.wasSimplified() && dto.overload.hasConvertibleParameterTypes()) {
                        worklistDto.convertibleAnalysisDto.isAnalysingConvertible = true;
                    }
                    worklistDto.workDeque.add(nextWorklistDto(worklistDto, dto.bindings));
                } else {
                    int numberOfConvertibleApplications
                            = worklistDto.overloadBindings.getNumberOfConvertibleApplications();
                    boolean hasConvertibleParameterTypes = dto.overload.hasConvertibleParameterTypes();
                    if (numberOfConvertibleApplications > 0 && hasConvertibleParameterTypes
                            || numberOfConvertibleApplications == 0 && !hasConvertibleParameterTypes) {
                        worklistDto.workDeque.add(nextWorklistDto(worklistDto, dto.bindings));
                    }
                }
            }
        }
    }

    private WorklistDto nextWorklistDto(WorklistDto worklistDto, IOverloadBindings bindings) {
        int pointer;
        if (!worklistDto.isSolvingDependency || worklistDto.isInIterativeMode) {
            pointer = worklistDto.pointer + 1;
        } else {
            pointer = Integer.MAX_VALUE;
        }
        return new WorklistDto(worklistDto, pointer, bindings);
    }

    private AggregateBindingDto solveOverLoad(
            WorklistDto worklistDto,
            IConstraint constraint,
            IFunctionType overload) {

        AggregateBindingDto dto = null;
        if (constraint.getArguments().size() >= overload.getNumberOfNonOptionalParameters()) {
            IOverloadBindings bindings = symbolFactory.createOverloadBindings(worklistDto.overloadBindings);
            dto = new AggregateBindingDto(constraint, overload, bindings, worklistDto);
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
                if (!reference.hasFixedType() && argumentsAreAllFixed && !dto.worklistDto.isInSoftTypingMode) {
                    dto.bindings.fixType(lhsAbsoluteName);
                }
                if (!dto.worklistDto.isInIterativeMode && !dto.worklistDto.isInSoftTypingMode
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
        IOverloadBindings rightBindings = dto.overload.getOverloadBindings();
        String overloadTypeVariable
                = rightBindings.getTypeVariableReference(dto.overloadVariableId).getTypeVariable();

        String lhsTypeVariable;
        if (dto.mapping.containsKey(overloadTypeVariable)) {
            lhsTypeVariable = dto.mapping.get(overloadTypeVariable).getTypeVariable();
            String rhsTypeVariable = bindingTypeVariableReference.getTypeVariable();
            dto.bindings.mergeFirstIntoSecond(rhsTypeVariable, lhsTypeVariable);

            //TINS-535 improve precision in soft typing for unconstrained parameters
//            if (dto.worklistDto.isInSoftTypingMode
//                    && dto.worklistDto.param2LowerParams.containsKey(rhsTypeVariable)
//                    && !rhsTypeVariable.equals(lhsTypeVariable)) {
//                List<String> params = dto.worklistDto.param2LowerParams.remove(rhsTypeVariable);
//                if (!dto.worklistDto.param2LowerParams.containsKey(lhsTypeVariable)) {
//                    dto.worklistDto.param2LowerParams.put(lhsTypeVariable, params);
//                } else {
//                    dto.worklistDto.param2LowerParams.get(lhsTypeVariable).addAll(params);
//                }
//            }
        } else {
            lhsTypeVariable = bindingTypeVariableReference.getTypeVariable();
            dto.mapping.put(overloadTypeVariable, bindingTypeVariableReference);
        }

        applyRightToLeft(dto, lhsTypeVariable, overloadTypeVariable);
    }

    private void applyRightToLeft(AggregateBindingDto dto, String left, String right) {

        IOverloadBindings leftBindings = dto.bindings;
        IOverloadBindings rightBindings = dto.overload.getOverloadBindings();

        if (rightBindings.hasUpperTypeBounds(right)) {
            ITypeVariableReference reference
                    = dto.bindings.getTypeVariableReference(dto.bindingVariable.getAbsoluteName());
            if (!dto.worklistDto.isInSoftTypingMode || !reference.hasFixedType()) {
                IIntersectionTypeSymbol rightUpperTypeBounds = rightBindings.getUpperTypeBounds(right);
                ITypeSymbol copy = copyIfNotFixed(rightUpperTypeBounds, dto);
                if (copy != null) {
                    if (!dto.worklistDto.isInSoftTypingMode) {
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
                    leftBindings.addLowerRefBound(left, dto.mapping.get(refTypeVariable));
                } else if (!dto.worklistDto.isInIterativeMode && dto.iterateCount == 1) {
                    ITypeVariableReference typeVariableReference = addHelperVariable(dto, refTypeVariable);
                    leftBindings.addLowerRefBound(left, typeVariableReference);
                } else if (dto.worklistDto.isInIterativeMode && dto.iterateCount == 1) {
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

            IOverloadBindings leftBindings = dto.bindings;
            Deque<IParametricTypeSymbol> parametricTypeSymbols = new ArrayDeque<>();
            copy = containerTypeSymbol.copy(parametricTypeSymbols);

            parametricTypes:
            for (IParametricTypeSymbol parametricTypeSymbol : parametricTypeSymbols) {
                List<String> typeParameters = new ArrayList<>();
                for (String typeParameter : parametricTypeSymbol.getTypeParameters()) {
                    String typeVariable;
                    if (dto.mapping.containsKey(typeParameter)) {
                        typeVariable = dto.mapping.get(typeParameter).getTypeVariable();
                    } else if (!dto.worklistDto.isInIterativeMode && dto.iterateCount == 1) {
                        typeVariable = addHelperVariable(dto, typeParameter).getTypeVariable();
                    } else if (dto.worklistDto.isInIterativeMode && dto.iterateCount == 1) {
                        typeVariable = searchTypeVariable(dto, typeParameter);
                        if (typeVariable == null) {
                            typeVariable = addHelperVariable(dto, typeParameter).getTypeVariable();
                        }
                    } else {
                        dto.needToReIterate = true;
                        copy = null;
                        break parametricTypes;
                    }
                    typeParameters.add(typeVariable);
                }
                if (!dto.worklistDto.isInSoftTypingMode) {
                    leftBindings.bind(parametricTypeSymbol, typeParameters);
                } else {
                    IOverloadBindings copyBindings
                            = symbolFactory.createOverloadBindings(parametricTypeSymbol.getOverloadBindings());
                    copyBindings.bind(parametricTypeSymbol, parametricTypeSymbol.getTypeParameters());
                    copyBindings.fixTypeParameters();
                }
            }
        }

        return copy;
    }

    //Warning! start code duplication - very similar as in addLowerRefInIterativeMode
    private String searchTypeVariable(AggregateBindingDto dto, String typeParameter) {
        IOverloadBindings rightBindings = dto.overload.getOverloadBindings();
        String typeVariable = null;
        if (rightBindings.hasLowerRefBounds(typeParameter)) {
            for (String refRefTypeVariable : rightBindings.getLowerRefBounds(typeParameter)) {
                if (dto.mapping.containsKey(refRefTypeVariable)) {
                    typeVariable = dto.mapping.get(refRefTypeVariable).getTypeVariable();
                    break;
                }
                searchTypeVariable(dto, refRefTypeVariable);
            }
        }
        return typeVariable;
    }
    //Warning! end code duplication - very similar as in addLowerRefInIterativeMode


    //Warning! start code duplication - very similar as in searchTypeVariable
    private void addLowerRefInIterativeMode(
            AggregateBindingDto dto, String left, String refTypeVariable) {
        IOverloadBindings rightBindings = dto.overload.getOverloadBindings();
        if (rightBindings.hasLowerRefBounds(refTypeVariable)) {
            for (String refRefTypeVariable : rightBindings.getLowerRefBounds(refTypeVariable)) {
                if (dto.mapping.containsKey(refRefTypeVariable)) {
                    dto.bindings.addLowerRefBound(left, dto.mapping.get(refRefTypeVariable));
                }
                addLowerRefInIterativeMode(dto, left, refRefTypeVariable);
            }
        }
    }
    //Warning! end code duplication - very similar as in searchTypeVariable

    private ITypeVariableReference addHelperVariable(AggregateBindingDto dto, String typeParameter) {
        ITypeVariableReference typeVariableReference = dto.bindings.createHelperVariable();
        dto.mapping.put(typeParameter, typeVariableReference);

        String typeVariable = typeVariableReference.getTypeVariable();
        applyRightToLeft(dto, typeVariable, typeParameter);

        return typeVariableReference;
    }

    private void updateAggregateBindingDto(AggregateBindingDto dto, BoundResultDto resultDto, ITypeSymbol copy) {
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

        EOverloadBindingsMode originalMode = dto.bindings.getMode();

        TypeHelperDto result = typeHelper.isFirstSameOrSubTypeOfSecond(currentLowerTypeBounds, typeSymbol);
        switch (result.relation) {
            case HAS_RELATION:
                BoundResultDto resultDto = dto.bindings.addLowerTypeBound(typeParameter, typeSymbol);
                hasChanged = resultDto.hasChanged;
                break;
            case HAS_COERCIVE_RELATION:
                dto.bindings.setMode(EOverloadBindingsMode.Modification);
                IUnionTypeSymbol newLowerTypeBounds = symbolFactory.createUnionTypeSymbol();
                newLowerTypeBounds.addTypeSymbol(typeSymbol);
                dto.bindings.setLowerTypeBounds(typeParameter, newLowerTypeBounds);
                dto.bindings.setMode(originalMode);
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
    public void addMostSpecificOverloadToWorklist(WorklistDto worklistDto, IConstraint constraint) {
        Collection<IFunctionType> overloads = constraint.getMethodSymbol().getOverloads();
        if (overloads.size() > 1) {
            List<IVariable> arguments = constraint.getArguments();
            List<ITypeSymbol> argumentTypes = calculateArgumentTypes(worklistDto, arguments);

            List<OverloadRankingDto> applicableOverloads
                    = getApplicableOverloads(worklistDto, constraint, overloads, argumentTypes);

            int numberOfApplicableOverloads = applicableOverloads.size();
            if (numberOfApplicableOverloads > 0) {
                OverloadRankingDto overloadRankingDto = applicableOverloads.get(0);
                if (numberOfApplicableOverloads > 1) {
                    overloadRankingDto = mostSpecificOverloadDecider.inNormalMode(
                            worklistDto, applicableOverloads, argumentTypes);
                }
                worklistDto.workDeque.add(nextWorklistDto(worklistDto, overloadRankingDto.bindings));
            } else if (!worklistDto.isSolvingMethod) {
                issueReporter.constraintViolation(worklistDto.overloadBindings, constraint);
                //TODO rstoll TINS-306 inference - runtime check insertion
                //I am not sure but maybe we do not need to do anything. see
                //TINS-399 save which overload was taken in AST
                //I think it is enough if the symbol does not contain any overload. The translator can then insert an
                // error in the output
            }
        } else {
            AggregateBindingDto dto = null;
            try {
                IFunctionType overload = overloads.iterator().next();
                dto = solveOverLoad(worklistDto, constraint, overload);
                if (dto != null) {
                    worklistDto.workDeque.add(nextWorklistDto(worklistDto, dto.bindings));
                }
            } catch (BoundException ex) {
                //that's ok, we report it below
            }
            if (dto == null && !worklistDto.isSolvingMethod) {
                issueReporter.constraintViolation(worklistDto.overloadBindings, constraint);
            }
        }
    }

    private List<ITypeSymbol> calculateArgumentTypes(WorklistDto worklistDto, List<IVariable> arguments) {
        List<ITypeSymbol> argumentTypes = new ArrayList<>(arguments.size());

        IOverloadBindings overloadBindings = worklistDto.overloadBindings;
        for (IVariable variable : arguments) {
            String argumentId = variable.getAbsoluteName();
            String typeVariable = overloadBindings.getTypeVariable(argumentId);

            //we prefer the upper type bound and when solving methods and the lower type bound when solving global
            // namespace scope
            IContainerTypeSymbol argumentType = null;
            if (worklistDto.isSolvingMethod && overloadBindings.hasUpperTypeBounds(typeVariable)) {
                argumentType = overloadBindings.getUpperTypeBounds(typeVariable);
            } else if (overloadBindings.hasLowerTypeBounds(typeVariable)) {
                argumentType = overloadBindings.getLowerTypeBounds(typeVariable);
            } else if (!worklistDto.isSolvingMethod && overloadBindings.hasUpperTypeBounds(typeVariable)) {
                argumentType = overloadBindings.getUpperTypeBounds(typeVariable);
            }

            if (argumentType != null && !argumentType.isFixed()) {
                //overload has non fixed arguments - since we manipulate it we have to copy it first...
                overloadBindings = symbolFactory.createOverloadBindings(overloadBindings);
                //.. then we fix all bounded types (in a brute force way - overloadBinding cannot be used for
                // constraint solving afterwards)
                overloadBindings.fixTypeParameters();
            }
            argumentTypes.add(argumentType);
        }
        return argumentTypes;
    }

    private List<OverloadRankingDto> getApplicableOverloads(
            WorklistDto worklistDto,
            IConstraint constraint,
            Collection<IFunctionType> overloads,
            List<ITypeSymbol> argumentTypes) {

        List<OverloadRankingDto> overloadBindingsList = new ArrayList<>();
        List<IVariable> arguments = constraint.getArguments();
        int numberOfArguments = arguments.size();

        for (IFunctionType overload : overloads) {
            if (numberOfArguments >= overload.getNumberOfNonOptionalParameters()) {
                try {
                    IOverloadBindings bindings = symbolFactory.createOverloadBindings(worklistDto.overloadBindings);
                    AggregateBindingDto dto = new AggregateBindingDto(constraint, overload, bindings, worklistDto);
                    aggregateBinding(dto);

                    //no need to check for one to one match if arguments were narrowed unless we solve constraints of
                    // the global default namespace scope - moreover, if the overload is not fixed,
                    // then we cannot use the check since there might be a fixed correspondence
                    // and last but not least, we cannot check whether an overload was fixed if it was not simplified
                    // yet which is only the case for indirect recursive functions which are still analysed (hence we
                    // are in iterative mode)
                    boolean isOneToOne = overload.wasSimplified() && overload.isFixed()
                            && (!dto.hasNarrowedArguments || !worklistDto.isSolvingMethod);

                    if (isOneToOne) {
                        isOneToOne = isOneToOneMatch(numberOfArguments, argumentTypes, overload);
                        if (isOneToOne) {
                            //reset list, we will break bellow
                            overloadBindingsList = new ArrayList<>(1);
                        }
                    }

                    overloadBindingsList.add(new OverloadRankingDto(
                            overload, dto.bindings, dto.implicitConversions, null, dto.hasNarrowedArguments));

                    if (isOneToOne) {
                        break;
                    }
                } catch (BoundException ex) {
                    //That is ok, we are looking for applicable overloads
                }
            }
        }
        return overloadBindingsList;
    }

    private boolean isOneToOneMatch(int numberOfArguments, List<ITypeSymbol> argumentTypes, IFunctionType overload) {
        boolean isOneToOne = true;
        IOverloadBindings rightBindings = overload.getOverloadBindings();

        List<IVariable> parameters = overload.getParameters();
        int numberOfParameters = parameters.size();
        int count = numberOfParameters <= numberOfArguments ? numberOfParameters : numberOfArguments;
        for (int i = 0; i < count; ++i) {
            IVariable variable = parameters.get(i);
            String argumentId = variable.getAbsoluteName();
            String typeVariable = rightBindings.getTypeVariable(argumentId);
            //TODO TINS-418 function application only consider upper bounds 0.4.1
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
    public void createDependencies(WorklistDto worklistDto) {
        List<IConstraint> constraints = worklistDto.constraintCollection.getConstraints();
        String absoluteName = worklistDto.constraintCollection.getAbsoluteName();
        for (Integer pointer : worklistDto.unsolvedConstraints) {
            String refAbsoluteName = constraints.get(pointer).getMethodSymbol().getAbsoluteName();
            MapHelper.addToListInMap(directDependencies, refAbsoluteName, pair(worklistDto, pointer));
            MapHelper.addToSetInMap(dependencies, refAbsoluteName, absoluteName);
        }
        MapHelper.addToSetInMap(
                unsolvedConstraints,
                absoluteName,
                worklistDto);
    }

    @Override
    public void finishingMethodConstraints(IMethodSymbol methodSymbol, List<IOverloadBindings> bindings) {
        methodSymbol.setBindings(bindings);
        createOverloads(methodSymbol, bindings);

        String methodName = methodSymbol.getAbsoluteName();
        if (directDependencies.containsKey(methodName)) {
            dependencies.remove(methodName);
            for (Pair<WorklistDto, Integer> element : directDependencies.remove(methodName)) {
                dependencyConstraintSolver.solveDependency(element);
            }
        }
        unsolvedConstraints.remove(methodName);
    }

    private void createOverloads(IMethodSymbol methodSymbol, List<IOverloadBindings> bindingsList) {
        for (IOverloadBindings bindings : bindingsList) {
            createOverload(methodSymbol, bindings);
        }
    }

    @Override
    public IFunctionType createOverload(IMethodSymbol methodSymbol, IOverloadBindings bindings) {
        List<IVariable> parameters = new ArrayList<>();
        for (IVariableSymbol parameter : methodSymbol.getParameters()) {
            String parameterId = parameter.getAbsoluteName();
            if (!bindings.containsVariable(parameterId)) {
                //the parameter is not used at all, hence it can be mixed
                ITypeVariableReference reference = new FixedTypeVariableReference(bindings.getNextTypeVariable());
                bindings.addVariable(parameterId, reference);
                //TODO rstoll TINS-407 - store fixed type only in lower bound
                //TODO rstoll TINS-387 function application only consider upper bounds
                bindings.addLowerTypeBound(reference.getTypeVariable(), mixedTypeSymbol);
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
