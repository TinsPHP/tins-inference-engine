/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine;


import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.symbols.ILazyTypeSymbol;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.common.symbols.IUnionTypeSymbol;
import ch.tsphp.tinsphp.common.inference.IInferencePhaseController;
import ch.tsphp.tinsphp.common.inference.constraints.IConstraintSolver;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.issues.IInferenceIssueReporter;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IOverloadSymbol;
import ch.tsphp.tinsphp.common.symbols.ISymbolFactory;
import ch.tsphp.tinsphp.common.symbols.ITypeVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.inference_engine.constraints.IntersectionConstraint;
import ch.tsphp.tinsphp.symbols.gen.TokenTypes;

import java.util.ArrayList;
import java.util.List;

public class InferencePhaseController implements IInferencePhaseController
{
    private final ISymbolFactory symbolFactory;
    private final IInferenceIssueReporter inferenceIssueReporter;
    private final IConstraintSolver constraintSolver;
    private final List<IMethodSymbol> functionScopes = new ArrayList<>();
    private final IGlobalNamespaceScope globalDefaultNamespaceScope;


    public InferencePhaseController(
            ISymbolFactory theSymbolFactory,
            IInferenceIssueReporter theInferenceErrorReporter,
            IConstraintSolver theConstraintSolver,
            IGlobalNamespaceScope theGlobalDefaultNamespaceScope) {
        symbolFactory = theSymbolFactory;
        inferenceIssueReporter = theInferenceErrorReporter;
        constraintSolver = theConstraintSolver;
        globalDefaultNamespaceScope = theGlobalDefaultNamespaceScope;
    }

//    @Override
//    public void assignInitialValueToFinal(ITSPHPAst identifier, ITSPHPAst rhs) {
//        ISymbol symbol = identifier.getSymbol();
//        // only assign if it has not already a type assigned (for instance via PHPDoc)
//        // if type should not match then it will be reported to the user in the
//        // resurrection phase
//        ITypeSymbol typeSymbol = symbol.getType();
//        if (typeSymbol.evalSelf() == null) {
//            IUnionTypeSymbol unionTypeSymbol = (IUnionTypeSymbol) symbol.getType();
//            ITypeSymbol evalType = rhs.getEvalType();
//            ITypeSymbol evalTypeEvaluated = evalType.evalSelf();
//            if (evalTypeEvaluated != null) {
//                unionTypeSymbol.addTypeSymbol(evalTypeEvaluated);
//            } else {
////                new DeferredFinalAssignment(unionTypeSymbol, (ILazyTypeSymbol) evalType);
//            }
//        }
//    }

//    @Override
//    public void assignInitialScopeType(ITSPHPAst identifier, ITSPHPAst typeAst) {
//        //TODO rstoll
//    }
//
//    @Override
//    public void createAssignConstraint(ITSPHPAst lhs, ITSPHPAst rhs) {
//        ISymbol symbol = lhs.getSymbol();
//        IScope scope = symbol.getDefinitionScope();
//        scope.addConstraint(symbol.getName(), new TypeConstraint(rhs.getEvalType()));
//    }
//
//    @Override
//    public void assignment(ITSPHPAst operator, ITSPHPAst lhs, ITSPHPAst rhs) {
//        if (isVariableOrField(lhs)) {
//            assignToVariableOrField(lhs, rhs);
//        } else if (isArrayAccess(lhs)) {
//            assignToArray(lhs, rhs);
//        } else {
//            //TODO TINS-306 inference - runtime check insertion
//        }
//
//        // using the eval type of the rhs rather than the type of the lhs allows the following
//        // class A{} class B extends A{}
//        // $a = new A();
//        // $b = $a = new B();
//        // $b is of type B and not A, seems strange at first but makes sense since no runtime check is required
//
//        // TODO TINS-306 inference - runtime check insertion
//        // Notice the above logic only holds if necessary runtime checks are added after the above statements.
//        // If they should be added later on (since runtime check insertion is another phase),
//        // then the more conservative approach needs to be taken (type of lhs)
//
//        // That is actually not the case since the lhs needs to be able to hold the rhs in all cases (data
// polymorphism)
//        // Unless... unless the lhs is already a closed union (for instance because PHPDoc specified a variable to be
//        // of a certain type)
//        // In this case a runtime check needs to be inserted, and the type would change. Consider the following
//        // /*@var $a string*/
//        // $a = "1";
//        // /*@var $c int*/
//        // $c = $b = $a = 1;
//
//        // would result in (type annotation with <>)
//        // assign($a<string>, "1"<string>)<string>;
//        // assign($c<int>,
//        //   error("string to int not supported",
//        //     assign(
//        //       $b<string>,
//        //       assign($a<string>, intToString(1<int>)<string>)<string>
//        //     )<string>
//        //   )<int>
//        // )<string>
//
//        //Hence we need to make sure that potential runtime checks are already inserted here.
//        //We take the more conservative approach at this point and might come back here
//
//        operator.setEvalType(lhs.getEvalType());
//    }

    @Override
    public void createTypeConstraint(ITSPHPAst literal) {
        ITypeVariableSymbol typeVariableSymbol = symbolFactory.createExpressionTypeVariableSymbol(literal);
        typeVariableSymbol.setType(literal.getEvalType());
        literal.setSymbol(typeVariableSymbol);
    }

    @Override
    public void createRefConstraint(ITypeVariableCollection collection, ITSPHPAst identifier, ITSPHPAst rhs) {
        IVariableSymbol variableSymbol = (IVariableSymbol) identifier.getSymbol();
        variableSymbol.addConstraint((ITypeVariableSymbol) rhs.getSymbol());
        collection.addTypeVariable(variableSymbol);
    }

    @Override
    public void createIntersectionConstraint(
            ITypeVariableCollection collection, ITSPHPAst operator, ITSPHPAst... arguments) {
        List<ITypeVariableSymbol> typeVariables = new ArrayList<>(arguments.length);
        for (ITSPHPAst argument : arguments) {
            typeVariables.add((ITypeVariableSymbol) argument.getSymbol());
        }
        IOverloadSymbol overloadSymbol = (IOverloadSymbol) operator.getSymbol();
        ITypeVariableSymbol typeVariableSymbol = symbolFactory.createExpressionTypeVariableSymbol(operator);
        typeVariableSymbol.setType(symbolFactory.createUnionTypeSymbol());
        typeVariableSymbol.addConstraint(new IntersectionConstraint(typeVariables, overloadSymbol.getOverloads()));
        operator.setSymbol(typeVariableSymbol);
        collection.addTypeVariable(typeVariableSymbol);
    }

    @Override
    public void addTypeVariableCollection(IMethodSymbol scope) {
        functionScopes.add(scope);
    }

    @Override
    public void solveAllConstraints() {
        for (ITypeVariableCollection scope : functionScopes) {
            constraintSolver.solveConstraints(scope);
        }

        constraintSolver.solveConstraints(globalDefaultNamespaceScope);
        for (ITypeVariableSymbol typeVariableSymbol : globalDefaultNamespaceScope.getTypeVariables().values()) {
            IUnionTypeSymbol unionTypeSymbol = typeVariableSymbol.getType();
            if (!unionTypeSymbol.isReadyForEval()) {
                unionTypeSymbol.seal();
            }
        }
    }

    private boolean isVariableOrField(ITSPHPAst lhs) {
        int tokenType = lhs.getType();
        //TODO TINS-307 inference OOP - field and static field assignment
        return tokenType == TokenTypes.VariableId;
    }

    private void assignToVariableOrField(ITSPHPAst lhs, ITSPHPAst rhs) {


        IVariableSymbol variableSymbol = (IVariableSymbol) lhs.getSymbol();
        IUnionTypeSymbol variableTypeSymbol = (IUnionTypeSymbol) variableSymbol.getType();
        if (variableTypeSymbol == null) {
            variableTypeSymbol = symbolFactory.createUnionTypeSymbol();
            variableSymbol.setType(variableTypeSymbol);
        }

        //TODO rstoll TINS-305 inference - data polymorphism
        ITypeSymbol evalType = rhs.getEvalType();
        ITypeSymbol evalTypeEvaluated = evalType.evalSelf();
        if (evalTypeEvaluated == null) {
//            new DeferredAssignment(variableTypeSymbol, (ILazyTypeSymbol) evalType);
        } else {
            variableTypeSymbol.addTypeSymbol(evalTypeEvaluated);
        }
    }

    private boolean isArrayAccess(ITSPHPAst lhs) {
        return lhs.getType() == TokenTypes.ARRAY_ACCESS;
    }

    private void assignToArray(ITSPHPAst lhs, ITSPHPAst rhs) {
        //TODO TINS-308 inference - seeding and propagation of arrays
    }

    public ITypeSymbol functionCall(ITSPHPAst identifier, ITSPHPAst arguments) {
        List<ITypeSymbol> args = new ArrayList<>();
        List<ILazyTypeSymbol> lazyArgs = new ArrayList<>();
        if (arguments.getChildCount() > 0) {
            for (ITSPHPAst ast : arguments.getChildren()) {
                ITypeSymbol evalType = ast.getEvalType();
                ITypeSymbol evalTypeEvaluated = evalType.evalSelf();
                if (evalTypeEvaluated == null) {
                    lazyArgs.add((ILazyTypeSymbol) evalType);
                } else {
                    args.add(evalTypeEvaluated);
                }
            }
        }

        //TODO TINS-309 function resolution
        IMethodSymbol function = null;
//        return new Application(function, args, lazyArgs);
        return null;
    }
}
