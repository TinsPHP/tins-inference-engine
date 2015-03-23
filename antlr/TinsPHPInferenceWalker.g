/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/* 
 * This file was created based on TSPHPTypeCheckWalker.g - the tree grammar file for the type checking phase of
 * TSPHP's type checker - and reuses TSPHP's AST class as well as other classes/files related to the AST generation.
 * TSPHP is also licenced under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

tree grammar TinsPHPInferenceWalker;
options {
    tokenVocab = TinsPHP;
    ASTLabelType = ITSPHPAst;
}

@header{
/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.antlr;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.ITSPHPErrorAst;
import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.ITypeVariableCollection;
import ch.tsphp.tinsphp.common.scopes.ICaseInsensitiveScope;
import ch.tsphp.tinsphp.common.scopes.IGlobalNamespaceScope;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.common.symbols.ITypeVariableSymbol;
import ch.tsphp.tinsphp.common.inference.IInferencePhaseController;
}

@members {
private IInferencePhaseController controller;
private ITypeVariableCollection currentScope;

public TinsPHPInferenceWalker(
        TreeNodeStream input, 
        IInferencePhaseController theController, 
        IGlobalNamespaceScope globalDefaultNamespaceScope) {
    this(input);
    controller = theController;
    currentScope = globalDefaultNamespaceScope;
}
}

compilationUnit
    :   namespace+
    ;
    
namespace
    :   ^(Namespace
            (TYPE_NAME|DEFAULT_NAMESPACE) 
            ^(NAMESPACE_BODY statement*)
        )
    ;    

statement
//TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
    :   /*useDefinitionList
    |*/   definition
    |   instruction
    ;

//TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
/*
useDefinitionList
    :   ^(Use (^(USE_DECLARATION TYPE_NAME Identifier))+)
    ;
*/
definition
    :   //TODO TINS-210 - reference phase - class definitions
        //classDefinition
        // TINS-211 - reference phase - interface definitions
    //|   interfaceDefinition
        //TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
/*        functionDefinition
        
    |*/   constDefinitionList
    ;

    
//TODO TINS-210 - reference phase - class definitions
/*    
classDefinition
    :   ^(Class
            cMod=.
            identifier=Identifier 
            classExtendsDeclaration[$identifier] 
            implementsDeclaration[identifier] 
            classBody) 
        {
            //TODO TINS-216 reference phase - double definition check classes
            //INamespaceScope namespaceScope = (INamespaceScope) $identifier.getScope();
            //namespaceScope.doubleDefinitionCheckCaseInsensitive($identifier.getSymbol());
        }
    ;

classExtendsDeclaration[ITSPHPAst identifier]
    :   ^(Extends classInterfaceType[null])
        {
            ITypeSymbol typeSymbol = $classInterfaceType.type;
            if(controller.checkIsClass($classInterfaceType.start, typeSymbol)){
                IClassTypeSymbol classTypeSymbol = (IClassTypeSymbol) identifier.getSymbol();
                classTypeSymbol.setParent((IClassTypeSymbol)typeSymbol);
                classTypeSymbol.addParentTypeSymbol((IClassTypeSymbol)typeSymbol);
            }
        } 
    |   'extends'
    ;

implementsDeclaration[ITSPHPAst identifier]
    :   ^('implements'
            (classInterfaceType[null]{
                ITypeSymbol typeSymbol = $classInterfaceType.type;
                if(controller.checkIsInterface($classInterfaceType.start, typeSymbol)){
                    ((IClassTypeSymbol)identifier.getSymbol()).addParentTypeSymbol((IInterfaceTypeSymbol)typeSymbol);
                }
            })+
        )
    |   'implements'
    ;
    
classBody
    :   ^(CLASS_BODY classBodyDefinition*)
    |   CLASS_BODY
    ;
    
classBodyDefinition
    :   constDefinitionList
    |   fieldDefinition
    |   constructDefinition
    |   methodDefinition
    ;
*/

constDefinitionList
    :   ^(CONSTANT_DECLARATION_LIST 
            ^(TYPE tMod=. type=.)
            constDeclaration+
        )
    ;

constDeclaration
    :   ^(identifier=Identifier unaryPrimitiveAtom)
        {
            controller.createRefConstraint(currentScope, $Identifier, $unaryPrimitiveAtom.start);
        }
    ;

unaryPrimitiveAtom
    :   primitiveAtomWithConstant
    //TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0  
    /*
    |   ^(UNARY_MINUS primitiveAtomWithConstant)
    |   ^(UNARY_PLUS primitiveAtomWithConstant)
    */
    ; 
    
primitiveAtomWithConstant
    :   (   Null
        |   False
        |   True
        |   Int
        |   Float
        |   String
        |   array
        )
        {
            controller.createTypeConstraint($start);
        }
    |   cnst=CONSTANT
    //TODO TINS-217 reference phase - class constant access
    //|   ^(CLASS_STATIC_ACCESS accessor=staticAccessor identifier=CONSTANT)
    //    {$identifier.setSymbol(accessResolver.resolveClassConstantAccess($accessor.start, $identifier));}
    ;

array
    :   ^(TypeArray arrayKeyValue*)
    ;

arrayKeyValue
    :   ^('=>' expression expression)
    |   expression
    ;

//TODO TINS-217 reference phase - class constant access
/*
staticAccessor
@after{$start.setEvalType((ITypeSymbol) $start.getSymbol());}
    :   classInterfaceType[null]
    |   slf='self'
        {
            IVariableSymbol variableSymbol = controller.resolveThisSelf($slf);
            $slf.setSymbol(variableSymbol.getType());
        }
    |   par='parent'
        {
            IVariableSymbol variableSymbol = controller.resolveParent($par);
            $par.setSymbol(variableSymbol.getType());
        }    
    ;
*/
    
// TINS-220 - reference phase - double definition check fields
/*    
fieldDefinition
    :   ^(FIELD variableDeclarationList[true])
    ;

variableDeclarationList[boolean isImplicitlyInitialised] 
    :   ^(VARIABLE_DECLARATION_LIST
            ^(TYPE tMod=. allTypesOrUnknown[$tMod]) 
            variableDeclaration[$allTypesOrUnknown.type, isImplicitlyInitialised]+ 
        )
    ;

variableDeclaration[ITypeSymbol type, boolean isImplicitlyInitialised] returns [IVariableSymbol variableSymbol]
@init{boolean isInitialised = false;}
    :   (   ^(variableId=VariableId expression) {isInitialised = true;}
        |   variableId=VariableId
        )
        { 
            //Warning! start duplicated code as in parameterNormalOrOptional
            //$variableSymbol = (IVariableSymbol) $variableId.getSymbol();
            //$variableSymbol.setType(type); 
            //$variableId.getScope().doubleDefinitionCheck($variableId.getSymbol());
            //Warning! end duplicated code as in parameterNormalOrOptional
            //TODO Fields are always initially initialised. 
            // Maybe it is better to rename this rule to fieldDeclaration (for now, static variables or global variables within function could not be initialised)
            //if(isInitialised || isImplicitlyInitialised){
            //    $variableId.getScope().addToInitialisedSymbols($variableSymbol, true);
            //}
        }
    ;
*/

//TODO TINS-221 - reference phase - double definition check methods 
/*
constructDefinition
    :   ^(identifier='__construct'
            .
            ^(TYPE rtMod=. voidType) 
            parameterDeclarationList block[false]
        )
        {
            IMethodSymbol methodSymbol = (IMethodSymbol) $identifier.getSymbol();
            methodSymbol.setType($voidType.type); 
            ICaseInsensitiveScope scope = (ICaseInsensitiveScope) $identifier.getScope();
            scope.doubleDefinitionCheckCaseInsensitive(methodSymbol);
        }
    ;
*/
  
//TODO TINS-221 - reference phase - double definition check methods 
/*        
methodDefinition
//Warning! start duplicated code as in functionDeclaration
    @init{
        hasAtLeastOneReturnOrThrow = false;
        boolean shallCheckIfReturns = false;
    }
//Warning! end duplicated code as in functionDeclaration

    :   ^(METHOD_DECLARATION
            ^(METHOD_MODIFIER methodModifier)
            ^(TYPE rtMod=. returnTypes[$rtMod]) 
            {shallCheckIfReturns = !($returnTypes.type instanceof IVoidTypeSymbol) && !$methodModifier.isAbstract;}
            (identifier=Identifier|identifier=Destruct) parameterDeclarationList block
        )
        {
        //Warning! start duplicated code as in functionDeclaration
            IMethodSymbol methodSymbol = (IMethodSymbol) $identifier.getSymbol();
            methodSymbol.setType($returnTypes.type); 
            ICaseInsensitiveScope scope = (ICaseInsensitiveScope) $identifier.getScope();
            scope.doubleDefinitionCheckCaseInsensitive(methodSymbol);
            if(shallCheckIfReturns){
        //Warning! end duplicated code as in functionDeclaration
                controller.checkReturnsFromMethod($block.isReturning, hasAtLeastOneReturnOrThrow, $identifier);
            }
        }

    ;    

methodModifier returns[boolean isAbstract]
    :   (   Static  Final           accessModifier
        |   Static  accessModifier  Final
        |   Static  accessModifier

        |   Final   Static          accessModifier
        |   Final   accessModifier  Static
        |   Final   accessModifier
        
        
        |   accessModifier  Final   Static
        |   accessModifier  Static  Final
        |   accessModifier  Static
        |   accessModifier  Final
        |   accessModifier
        
        |    abstr=Abstract accessModifier
        |    accessModifier abstr=Abstract
        )
        {$isAbstract= $abstr != null;}
    ;

accessModifier
    :   Private
    |   Protected
    |   Public
    ;
*/

//TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
/*    
functionDefinition
    :   ^(Function {currentScope = $Function.getScope();}
            returnType=.
            ^(TYPE rtMod=. type=.)
            Identifier 
            parameterDeclarationList 
            block
        )
        {
            controller.addToSolveConstraints((IMethodSymbol)currentScope);
            currentScope = currentScope.getEnclosingScope();
        }
    ;

parameterDeclarationList
    :   ^(PARAMETER_LIST parameterDeclaration*)
    ;

parameterDeclaration
    :   ^(PARAMETER_DECLARATION
            type=. 
            (   variableId=VariableId
            |   ^(variableId=VariableId unaryPrimitiveAtom)
            )
        )
    ;

block
    :   ^(BLOCK instruction*)
    ;
*/
    
// TINS-211 - reference phase - interface definitions
/*
interfaceDefinition
    :   ^('interface' iMod=. identifier=Identifier extIds=interfaceExtendsDeclaration[$identifier] interfaceBody)
        {
            INamespaceScope namespaceScope = (INamespaceScope) $identifier.getScope();
            namespaceScope.doubleDefinitionCheckCaseInsensitive($identifier.getSymbol());
        }
    ;
interfaceExtendsDeclaration[ITSPHPAst identifier]
    :   ^('extends'
            (allTypes[null]{
                ITypeSymbol typeSymbol = $allTypes.type;
                if(controller.checkIsInterface($allTypes.start, typeSymbol)){
                    ((IInterfaceTypeSymbol)identifier.getSymbol()).addParentTypeSymbol((IInterfaceTypeSymbol)typeSymbol);
                }
            })+
        ) 
    |   'extends'
    ;
    
interfaceBody
    :   ^(INTERFACE_BODY interfaceBodyDefinition*)
    |   INTERFACE_BODY
    ;
    
interfaceBodyDefinition
    :   constDefinitionList
    |   methodDefinition
    |   constructDefinition
    ;    
*/

instruction
    :   variableDeclarationList
    //TODO  TINS-71 inference procedural - take into account control structures
    /*
    |   ifCondition
    |   switchCondition
    |   forLoop
    |   foreachLoop
    |   whileLoop
    |   doWhileLoop
    |   tryCatch
    */
    |   ^(EXPRESSION expression?)
    //TODO  TINS-71 inference procedural - take into account control structures
    /*
    |   ^('return' expression?)
    |   ^('throw' expression)
    |   ^('echo' expression+)
    |   breakContinue
    */
    ;

variableDeclarationList
    :   ^(VARIABLE_DECLARATION_LIST
            ^(TYPE tMod=. type=.)
            variableDeclaration+ 
        )
    ;

variableDeclaration
    :  //TODO rstoll TINS-351 SmartVariableDeclarationCreator
       /*
       ^(VariableId expression)
        {
            controller.createRefConstraint(currentScope, $VariableId, $expression.start);
        }
        
    |*/   VariableId
        {
            currentScope.addTypeVariableWhichNeedToBeSealed((ITypeVariableSymbol)$VariableId.getSymbol());
        }
    ;    

//TODO  TINS-71 inference procedural - take into account control structures    
/*
ifCondition
    :   ^('if'
            expression 
            ifBlock=blockConditional
            (elseBlock=blockConditional)?
        )
    ;

blockConditional
    :   ^(BLOCK_CONDITIONAL instruction*)
    ;
        
switchCondition
    :   ^('switch' expression (^(SWITCH_CASES caseLabels) blockConditional)*)
    ;

caseLabels
    :   (   expression
        |   Default
        )+
    ;
    
forLoop
    :   ^('for'
            expressionList
            expressionList 
            expressionList
            blockConditional
        )
    ;

expressionList
    :   ^(EXPRESSION_LIST expression*)
    ;

foreachLoop
    :
        ^(foreach='foreach' 
            expression 
            key=VariableId?
            value=VariableId
            blockConditional
        )
    ;

whileLoop
    :   ^('while' expression blockConditional)
    ;


doWhileLoop
    :   ^('do' block expression)
    ;

tryCatch
    :   ^('try' blockConditional catchBlocks)
    ;
    
catchBlocks
    :   (   ^(Catch 
                TYPE_NAME
                variableId=VariableId 
                blockConditional
            )
        )+ 
    ;
    
breakContinue
    :   Break
    |   ^(Break Int)
    |   Continue
    |   ^(Continue Int)
    ;
*/    

expression
    :   atom
    |   operator
        //TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
    /*|   functionCall
    //TODO rstoll TINS-161 inference OOP    
    //|   methodCall
    //|   methodCallStatic
    //|   classStaticAccess
    |   postFixExpression
    |   exit
    */
    ;
        
atom
    :   primitiveAtomWithConstant
    |   variable
    //TODO rstoll TINS-223 - reference phase - resolve this and self
    //|   thisVariable
    ;

variable    
    :   varId=VariableId
    ;

    
//TODO TINS-223 - reference phase - resolve this and self
/*    
thisVariable
    :   t='$this'
        {$t.setSymbol(controller.resolveThisSelf($t));}
    ;
*/



operator
//TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0  
    : /*  ^(unaryOperator expression)
        {
            controller.createIntersectionConstraint(currentScope, $start,$expression.start);
        }
    | */  ^(binaryOperator lhs=expression rhs=expression)
        {
            controller.createIntersectionConstraint(currentScope, $start, $lhs.start, $rhs.start); 
        }
    //TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
    /*|   ^('?' cond=expression ifExpr=expression elseExpr=expression)
        {
            controller.createIntersectionConstraint(currentScope, $start, $cond.start, $ifExpr.start, $elseExpr.start);
        }
    |   ^(CAST ^(TYPE tMod=. type=.) expression)
    |   ^(Instanceof 
            lhs=expression 
            (   variable {rhsType=$variable.start;}
            |   rhsType=TYPE_NAME  { controller.createTypeConstraint($start);}
            )
        )
        {
            controller.createIntersectionConstraint(currentScope, $lhs.start, $rhsType);
        }

    //|   ^('new' classInterfaceType[null] actualParameters)
    */
    ;

//TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
/*
unaryOperator
    :   PRE_INCREMENT
    |   PRE_DECREMENT
    |   '@'
    |   '~'
    |   '!'
    |   UNARY_MINUS
    |   UNARY_PLUS
    |   POST_INCREMENT
    |   POST_DECREMENT
    |   'clone'
    ;
*/
binaryOperator
    :   'or'
    |   'xor'
    |   'and'
    
    |   '='
    |   '+='
    |   '-='
    |   '*='
    |   '/='
    |   '&='
    |   '|='
    |   '^='
    |   '%='
    |   '.='
    |   '<<='
    |   '>>='
    
    |   '||'
    |   '&&'
    |   '|'
    |   '^'
    |   '&'
    
    |   '=='
    |   '!='
    |   '==='
    |   '!=='
    
    |   '<'
    |   '<='
    |   '>'
    |   '>='
    
    |   '<<'
    |   '>>'
    
    |   '+'
    |   '-'
    |   '.'
    
    |   '*'
    |   '/'
    |   '%'
    ;

//TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
/*    
functionCall
        // function call has no callee and is therefor not resolved in this phase.
        // resolving occurs in the inference phase where overloads are taken into account
    :   ^(FUNCTION_CALL identifier=TYPE_NAME actualParameters)
    ;
    
actualParameters
    :   ^(ACTUAL_PARAMETERS expression+)
    |   ACTUAL_PARAMETERS
    ; 
*/
  
//TODO TINS-161 inference OOP    
/*
methodCall
    :   ^(METHOD_CALL methodCallee Identifier actualParameters)
    ;
    
methodCallee
    :  //TODO TINS-223 reference phase - resolve this and self
       // thisVariable
       variable
    //TODO TINS-223 reference phase - resolve this and self
    //|   slf='self'
    //    {$slf.setSymbol(controller.resolveThisSelf($slf));}
    //TODO TINS-225 - reference phase - resolve parent
    //|   par='parent'
    //    {$par.setSymbol(controller.resolveParent($par));}    
    ;
    
methodCallStatic
    :   ^(METHOD_CALL_STATIC classInterfaceType[null] Identifier actualParameters)
    ;
    
classStaticAccess
    :   ^(CLASS_STATIC_ACCESS accessor=staticAccessor identifier=CLASS_STATIC_ACCESS_VARIABLE_ID)
        {$identifier.setSymbol(accessResolver.resolveStaticFieldAccess($accessor.start, $identifier));}
    ;        
*/

//TODO rstoll TINS-314 inference procedural - seeding & propagation v. 0.3.0
/*
postFixExpression
// postFixExpression are resolved in the type checking phase
// due to the fact that method/function calls are resolved during the type check phase
// This rules are needed to resolve variables/function calls etc. in expression and actualParameters
    :   //TODO TINS-161 inference OOP
        //^(FIELD_ACCESS expression Identifier)
       ^(ARRAY_ACCESS expression expression)
    //TODO TINS-161 inference OOP
    //|   ^(METHOD_CALL_POSTFIX expression Identifier actualParameters)
    ;

exit
    :   ^('exit' expression)
    |   'exit'
    ;
*/